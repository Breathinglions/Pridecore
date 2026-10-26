package com.prideregion.core.story;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.prideregion.core.PrideCoreMod;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

/** Module 02: opt-in dialogue engine, not a replacement for existing NPC mod hooks. */
public final class StoryModule {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrideStory");
    private static final String PREFIX = "pride_npc_";
    private static volatile MinecraftServer liveServer;
    private static volatile StoryEngine engine;
    private static volatile String health = "disabled";

    private StoryModule() {}

    public static void registerHooks() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity sp) || hand != Hand.MAIN_HAND)
                return ActionResult.PASS;
            StoryEngine running = liveServer == sp.getServer() ? engine : null;
            if (running == null) return ActionResult.PASS;
            List<String> matches = entity.getCommandTags().stream()
                .filter(t -> t.startsWith(PREFIX))
                .filter(t -> StoryDefinition.validId(t.substring(PREFIX.length())))
                .sorted().toList();
            if (matches.size() != 1) return ActionResult.PASS;
            String npc = matches.getFirst().substring(PREFIX.length());
            try {
                show(sp, running.begin(sp.getUuid(), npc));
            } catch (IOException | RuntimeException ex) {
                LOGGER.warn("Story conversation denied for player {} npc {}: {}",
                    sp.getUuid(), npc, ex.toString());
                sp.sendMessage(Text.literal("Story not available: " + safe(ex.getMessage()))
                    .formatted(Formatting.RED), false);
            }
            // Only explicitly tagged demo actors get consumed. No generic NPC interception.
            return ActionResult.SUCCESS;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            StoryEngine running = liveServer == server ? engine : null;
            if (running != null) running.cancel(handler.getPlayer().getUuid());
        });
    }

    public static void start(MinecraftServer server) {
        liveServer = server;
        engine = null;
        health = "disabled";
        try {
            Path directory = configDir();
            Files.createDirectories(directory);
            Path settings = directory.resolve("story.properties");
            if (Files.notExists(settings)) {
                try {
                    Files.writeString(settings,
                        "# Keep disabled until using disposable staging NPC actors.\nstory_enabled=false\n",
                        CREATE_NEW);
                } catch (FileAlreadyExistsException ignored) { }
            }
            Properties props = new Properties();
            try (var input = Files.newInputStream(settings)) { props.load(input); }
            if (!"true".equalsIgnoreCase(props.getProperty("story_enabled", "false").trim())) {
                LOGGER.info("Pride Story module disabled; existing NPCs are untouched");
                return;
            }
            var service = PrideCoreMod.progression(server)
                .orElseThrow(() -> new IOException("Module 01 progression unavailable"));
            Path file = storyFile();
            if (Files.notExists(file)) {
                try (InputStream input = StoryModule.class.getResourceAsStream("/story/default.json")) {
                    if (input == null) throw new IOException("Bundled default story missing");
                    try { Files.copy(input, file); }
                    catch (FileAlreadyExistsException ignored) { }
                }
            }
            var loaded = new StoryEngine(StoryJson.load(file), service);
            engine = loaded;
            health = "active";
            LOGGER.info("Pride Story module active: {} NPC definitions", loaded.npcCount());
        } catch (IOException | RuntimeException ex) {
            engine = null;
            health = "error (see server log)";
            LOGGER.error("Pride Story initialization refused; no world/NPC changes made", ex);
        }
    }

    public static void stop(MinecraftServer server) {
        if (liveServer == server) {
            engine = null;
            liveServer = null;
            health = "disabled";
        }
    }

    private static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("pride_core");
    }
    private static Path storyFile() { return configDir().resolve("story.json"); }

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> commands() {
        var story = CommandManager.literal("story");
        story.then(CommandManager.literal("status").executes(ctx -> status(ctx.getSource())));
        story.then(CommandManager.literal("choose")
            .then(CommandManager.argument("option", StringArgumentType.word())
                .executes(ctx -> choose(ctx.getSource(),
                    StringArgumentType.getString(ctx, "option")))));
        story.then(CommandManager.literal("cancel").executes(ctx -> cancel(ctx.getSource())));
        story.then(CommandManager.literal("reload")
            .requires(source -> source.hasPermissionLevel(4))
            .executes(ctx -> reload(ctx.getSource())));
        story.then(CommandManager.literal("admin")
            .requires(source -> source.hasPermissionLevel(4))
            .then(CommandManager.literal("talk")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("npc", StringArgumentType.word())
                        .executes(ctx -> simulate(ctx.getSource(),
                            EntityArgumentType.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "npc")))))));
        return story;
    }

    private static StoryEngine ready(MinecraftServer server) {
        StoryEngine active = liveServer == server ? engine : null;
        if (active == null) throw new IllegalStateException("Story module not active: " + health);
        return active;
    }

    private static int status(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null)
            return message(source, "Module 02 story=" + health);
        try {
            var service = PrideCoreMod.progression(source.getServer())
                .orElseThrow(() -> new IOException("Module 01 unavailable"));
            var p = service.get(player.getUuid());
            return message(source, "Story module=" + health + " | stage=" + p.storyStage()
                + " | owned facts=" + p.facts().size() + " | revision=" + p.revision());
        } catch (IOException ex) {
            LOGGER.warn("Story status load failed for {}", player.getUuid(), ex);
            source.sendError(Text.literal("Story profile unavailable; check server log"));
            return 0;
        }
    }

    private static int choose(ServerCommandSource source, String id) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Only players can choose story responses"));
            return 0;
        }
        try {
            show(player, ready(source.getServer()).choose(player.getUuid(), id));
            return 1;
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Story choice rejected player {} choice {}: {}", player.getUuid(), id, ex.toString());
            source.sendError(Text.literal(safe(ex.getMessage())));
            return 0;
        }
    }

    private static int cancel(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Only players can cancel dialogue"));
            return 0;
        }
        try {
            ready(source.getServer()).cancel(player.getUuid());
            return message(source, "Conversation cancelled");
        } catch (RuntimeException ex) {
            source.sendError(Text.literal(safe(ex.getMessage())));
            return 0;
        }
    }

    /** Reload atomically: invalid configuration leaves the previous live story untouched. */
    private static int reload(ServerCommandSource source) {
        try {
            var service = PrideCoreMod.progression(source.getServer())
                .orElseThrow(() -> new IOException("Module 01 unavailable"));
            StoryDefinition def = StoryJson.load(storyFile());
            StoryEngine next = new StoryEngine(def, service);
            if (liveServer != source.getServer() || engine == null)
                throw new IOException("Story module disabled; reload cannot enable it");
            engine = next; // old conversations discarded, player progress preserved.
            health = "active";
            return message(source, "Story definitions reloaded: " + next.npcCount()
                + " NPCs. Existing conversations reset; saved progress retained.");
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Story reload failed; active definitions kept", ex);
            source.sendError(Text.literal("Story reload rejected; old definitions retained: "
                + safe(ex.getMessage())));
            return 0;
        }
    }

    /** Explicit operator test harness; no remote unprivileged NPC interaction. */
    private static int simulate(ServerCommandSource source, ServerPlayerEntity target, String npc) {
        try {
            show(target, ready(source.getServer()).begin(target.getUuid(), npc));
            return message(source, "Presented NPC " + npc + " dialogue to " + target.getName().getString());
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Admin NPC test refused for {}: {}", npc, ex.toString());
            source.sendError(Text.literal("NPC interaction unavailable: " + safe(ex.getMessage())));
            return 0;
        }
    }

    private static void show(ServerPlayerEntity player, StoryEngine.Reply reply) {
        player.sendMessage(Text.literal(reply.npcName() + ": ").formatted(Formatting.GOLD)
            .append(Text.literal(reply.text()).formatted(Formatting.WHITE)), false);
        for (var option : reply.options()) {
            player.sendMessage(Text.literal("  [" + option.id() + "] ")
                .formatted(Formatting.AQUA)
                .append(Text.literal(option.text()).formatted(Formatting.GRAY)), false);
        }
        if (!reply.options().isEmpty())
            player.sendMessage(Text.literal("Choose: /pridecore story choose <option>")
                .formatted(Formatting.DARK_GRAY), false);
    }

    private static String safe(String message) {
        if (message == null || message.isBlank()) return "Unknown error";
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private static int message(ServerCommandSource source, String text) {
        source.sendFeedback(() -> Text.literal(text), false);
        return 1;
    }
}
