package com.prideregion.core;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

/**
 * Server-owned, validated starter kits. A claimed native onboarding service
 * remains the only authority for the stage; item lists cannot grant progress.
 * The original Cobblemon starter selector is deliberately out of scope.
 */
public final class OnboardingRewards {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrideOnboardingRewards");
    private static final int MAX_CONFIG_BYTES = 32768;
    private static final String THEO = "theo_registration";
    private static final String AVERY = "avery_field_kit";
    private record Grant(Identifier id, int count) {}
    private record Kit(List<Grant> items) {}
    private static volatile Map<String, Kit> kits = Map.of();
    private static volatile String health = "not loaded";
    private static Path sourceFile;

    private OnboardingRewards() {}

    private static String defaults() {
        return """
            {
              "schema": 1,
              "theo_registration": {
                "items": [
                  {"id": "rctmod:trainer_card", "count": 1},
                  {"id": "knowlogy:knowlogy_book", "count": 1}
                ]
              },
              "avery_field_kit": {
                "items": [
                  {"id": "cobblemon:poke_ball", "count": 10},
                  {"id": "minecraft:bread", "count": 8}
                ]
              }
            }
            """;
    }

    public static synchronized void load() throws IOException {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("pride_core");
        Files.createDirectories(dir);
        Path path = dir.resolve("onboarding_rewards.json");
        if (Files.isSymbolicLink(path))
            throw new IOException("Refusing symlinked onboarding rewards config");
        if (Files.notExists(path)) {
            try { Files.writeString(path, defaults(), StandardCharsets.UTF_8, CREATE_NEW); }
            catch (FileAlreadyExistsException ignored) { }
        }
        sourceFile = path;
        reload();
    }

    public static synchronized void reload() throws IOException {
        if (sourceFile == null) throw new IOException("No configured onboarding file");
        try {
            if (!Files.isRegularFile(sourceFile) || Files.isSymbolicLink(sourceFile)
                    || Files.size(sourceFile) > MAX_CONFIG_BYTES)
                throw new IOException("Unsafe or oversized onboarding config");
            JsonObject root = JsonParser.parseString(Files.readString(sourceFile,
                StandardCharsets.UTF_8)).getAsJsonObject();
            keys(root, Set.of("schema", THEO, AVERY));
            if (root.get("schema").getAsInt() != 1)
                throw new IllegalArgumentException("Unsupported onboarding schema");
            Map<String, Kit> next = new HashMap<>();
            next.put(THEO, parseKit(root.getAsJsonObject(THEO)));
            next.put(AVERY, parseKit(root.getAsJsonObject(AVERY)));
            // Validate all item registry references *before* accepting config.
            for (var entry : next.entrySet())
                for (Grant grant : entry.getValue().items())
                    if (!Registries.ITEM.containsId(grant.id()))
                        throw new IllegalArgumentException("Unknown item " + grant.id()
                            + " in " + entry.getKey() + " (required mod missing?)");
            kits = Map.copyOf(next);
            health = "ready: " + sourceFile;
            LOGGER.info("Onboarding rewards ready, Theo={} entries, Avery={} entries",
                kits.get(THEO).items().size(), kits.get(AVERY).items().size());
        } catch (IOException | RuntimeException failed) {
            // Never silently fall back to an old kit after an invalid reload.
            kits = Map.of();
            health = "INVALID: " + failed.getMessage();
            throw new IOException("Onboarding rewards configuration rejected; grants disabled", failed);
        }
    }

    private static Kit parseKit(JsonObject object) {
        if (object == null) throw new IllegalArgumentException("Missing onboarding kit");
        keys(object, Set.of("items"));
        JsonArray array = object.getAsJsonArray("items");
        if (array == null || array.size() < 1 || array.size() > 32)
            throw new IllegalArgumentException("Kit must have 1-32 entries");
        List<Grant> out = new ArrayList<>();
        Set<Identifier> used = new HashSet<>();
        int total = 0;
        for (JsonElement element : array) {
            JsonObject row = element.getAsJsonObject();
            keys(row, Set.of("id", "count"));
            Identifier id = Identifier.tryParse(row.get("id").getAsString());
            int count = row.get("count").getAsInt();
            if (id == null || count < 1 || count > 64 || !used.add(id))
                throw new IllegalArgumentException("Invalid item ID, count or duplicate entry");
            total += count;
            if (total > 512) throw new IllegalArgumentException("Kit too large");
            out.add(new Grant(id, count));
        }
        return new Kit(List.copyOf(out));
    }

    private static void keys(JsonObject json, Set<String> expected) {
        if (!json.keySet().equals(expected))
            throw new IllegalArgumentException("Unexpected config keys: " + json.keySet());
    }

    public static String health() { return health; }

    public static int claim(ServerCommandSource source, String name) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return failure(source, "A real online player is required");
        Kit kit = kits.get(name);
        if (kit == null) return failure(source, "Onboarding kit unavailable: " + health);
        boolean theo = THEO.equals(name), avery = AVERY.equals(name);
        if (!theo && !avery) return failure(source, "Unrecognized kit");
        String claimTag = theo ? "trainer_card_received" : "trainer_guide_received";
        String gate = theo ? "starter_chosen" : "trainer_card_received";
        if (!player.getCommandTags().contains(gate))
            return failure(source, "Complete the previous onboarding step first");
        if (player.getCommandTags().contains(claimTag)
                || (avery && player.getCommandTags().contains("pride_avery_kit")))
            return failure(source, "Starter kit already claimed");

        // Command is player-callable but cannot be used remotely or at an
        // unrelated actor. Only the tagged original Cobblemon service NPC works.
        String npc = theo ? "pride_npc_technician_theo" : "pride_npc_avery";
        boolean physicalServiceNearby = false;
        for (Entity candidate : player.getWorld().getOtherEntities(player,
                player.getBoundingBox().expand(5.0),
                e -> "cobblemon:npc".equals(Registries.ENTITY_TYPE.getId(e.getType()).toString())
                    && e.getCommandTags().contains("pride_service_npc")
                    && e.getCommandTags().contains(npc))) {
            if (candidate.squaredDistanceTo(player) <= 36.0D) {
                physicalServiceNearby = true;
                break;
            }
        }
        if (!physicalServiceNearby)
            return failure(source, "Talk to the original nearby Theo/Avery service NPC");

        List<ItemStack> items = new ArrayList<>();
        for (Grant grant : kit.items()) {
            if (!Registries.ITEM.containsId(grant.id()))
                return failure(source, "Missing registered item: " + grant.id());
            Item item = Registries.ITEM.get(grant.id());
            ItemStack stack = new ItemStack(item, grant.count());
            if (stack.isEmpty() || grant.count() > stack.getMaxCount())
                return failure(source, "Item count exceeds stack limit: " + grant.id());
            items.add(stack);
        }
        if (!canFit(player, items))
            return failure(source, "Free inventory slots before receiving your kit");
        // Entire grant executes on the main server thread. One recipient,
        // bounded stacks, and preflighted inventory capacity.
        if (!source.getServer().isOnThread())
            return failure(source, "Onboarding grants require server thread");
        for (ItemStack stack : items) {
            if (!player.getInventory().insertStack(stack) || !stack.isEmpty()) {
                LOGGER.error("Unexpected partial onboarding grant for {}, kit={}", player.getUuid(), name);
                return failure(source, "Inventory changed during kit delivery; alert an administrator");
            }
        }
        player.addCommandTag(claimTag);
        if (theo) player.addCommandTag("pokepedia_received");
        if (avery) {
            player.addCommandTag("field_kit_received");
            player.addCommandTag("pride_avery_kit");
        }
        LOGGER.info("Native onboarding kit {} claimed once by {}", name, player.getUuid());
        source.sendFeedback(() -> Text.literal(theo
            ? "Trainer Registration complete! Your configured Trainer Card and Poképedia kit was delivered. Visit Avery next."
            : "Field Kit received! Your configured supplies were delivered. Head toward Route 1."), false);
        return 1;
    }

    /** Predict fill order without touching the real inventory. */
    private static boolean canFit(ServerPlayerEntity player, List<ItemStack> grants) {
        ItemStack[] slots = new ItemStack[36];
        for (int i = 0; i < slots.length; i++)
            slots[i] = player.getInventory().getStack(i).copy();
        for (ItemStack addition : grants) {
            int remaining = addition.getCount();
            for (ItemStack current : slots) {
                if (current.isEmpty() || current.getItem() != addition.getItem()
                        || !current.getComponents().equals(addition.getComponents())) continue;
                int fill = Math.min(remaining, current.getMaxCount() - current.getCount());
                current.increment(fill);
                remaining -= fill;
                if (remaining == 0) break;
            }
            for (int i = 0; i < slots.length && remaining > 0; i++) {
                if (!slots[i].isEmpty()) continue;
                int fill = Math.min(remaining, addition.getMaxCount());
                slots[i] = addition.copyWithCount(fill);
                remaining -= fill;
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private static int failure(ServerCommandSource source, String error) {
        source.sendError(Text.literal(error));
        return 0;
    }
}
