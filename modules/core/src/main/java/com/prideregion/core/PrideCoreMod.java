package com.prideregion.core;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.prideregion.core.story.StoryModule;
import com.prideregion.core.story.StorySceneVisualBridge;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Module 01 progression gateway and Module 02 optional Story & NPC engine.
 * Deliberately does NOT modify legacy NPCs, League, economy or chat rendering.
 */
public final class PrideCoreMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrideCore");
    private static volatile PrideCoreConfig config = new PrideCoreConfig(false);
    private static volatile MinecraftServer activeServer;
    private static volatile ProgressionService activeProgression;

    /** Future modules can obtain the sole progression service for this server. */
    public static Optional<ProgressionService> progression(MinecraftServer server) {
        return activeServer == server ? Optional.ofNullable(activeProgression) : Optional.empty();
    }

    @Override public void onInitialize() {
        StorySceneVisualBridge.register();
        StoryModule.registerHooks();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                config = PrideCoreConfig.load(FabricLoader.getInstance().getConfigDir());
                activeProgression = new ProgressionService(new ProfileStore(
                    server.getSavePath(WorldSavePath.ROOT)
                          .resolve("data").resolve("pride_core").resolve("players")));
                activeServer = server;
                LOGGER.info("Pride Core Module 01 service online. Admin writes enabled: {}",
                    config.allowAdminMutations());
                StoryModule.start(server);
            } catch (IOException ex) {
                activeServer = null;
                activeProgression = null;
                config = new PrideCoreConfig(false);
                LOGGER.error("Pride Core initialization failed. Profile service unavailable.", ex);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            StoryModule.stop(server);
            if (activeServer == server) {
                activeServer = null;
                activeProgression = null;
                config = new PrideCoreConfig(false);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = CommandManager.literal("pridecore")
                .then(CommandManager.literal("profile")
                    .executes(ctx -> ownProfile(ctx.getSource())))
                .then(CommandManager.literal("status")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(ctx -> feedback(ctx.getSource(),
                        "Pride Core Module 01 + 02 RC | active=" +
                        progression(ctx.getSource().getServer()).isPresent() +
                        " | admin writes=" + config.allowAdminMutations() +
                        " | legacy tags read-only")));
            var admin = CommandManager.literal("admin")
                .requires(source -> source.hasPermissionLevel(4));
            admin.then(CommandManager.literal("inspect")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> inspect(ctx.getSource(),
                        EntityArgumentType.getPlayer(ctx, "player"), false))));
            admin.then(CommandManager.literal("facts")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> inspect(ctx.getSource(),
                        EntityArgumentType.getPlayer(ctx, "player"), true))));
            admin.then(CommandManager.literal("legacy")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> legacy(ctx.getSource(),
                        EntityArgumentType.getPlayer(ctx, "player")))));
            for (String action : List.of("grant", "revoke")) {
                admin.then(CommandManager.literal(action)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("fact", StringArgumentType.greedyString())
                            .executes(ctx -> fact(ctx.getSource(),
                                EntityArgumentType.getPlayer(ctx, "player"),
                                StringArgumentType.getString(ctx, "fact"),
                                action.equals("grant"))))));
            }
            admin.then(CommandManager.literal("stage")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("stage", StringArgumentType.greedyString())
                        .executes(ctx -> stage(ctx.getSource(),
                            EntityArgumentType.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "stage"))))));
            dispatcher.register(root.then(admin).then(StoryModule.commands()));
        });
    }

    private static ProgressionService service(ServerCommandSource source) throws IOException {
        return progression(source.getServer())
            .orElseThrow(() -> new IOException("Pride Core progression service unavailable"));
    }

    private static int ownProfile(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return error(source, "From console use /pridecore admin inspect <online-player>.");
        return inspect(source, player, false);
    }

    private static int inspect(ServerCommandSource source, ServerPlayerEntity player, boolean showFacts) {
        try {
            ProgressionView view = service(source).view(player.getUuid(), player.getCommandTags());
            ProfileState p = view.owned();
            if (showFacts) {
                String displayed = String.join(", ", p.facts().stream().limit(16).toList());
                return feedback(source, player.getName().getString() + " Pride-owned facts (" +
                    p.facts().size() + "): " + (displayed.isEmpty() ? "(none)" : displayed) +
                    (p.facts().size() > 16 ? " ... (first 16)" : ""));
            }
            return feedback(source, "Trainer " + player.getName().getString() +
                " | story=" + p.storyStage() + " | owned facts=" + p.facts().size() +
                " | revision=" + p.revision() +
                " | external legacy tags=" + view.legacyTags().size());
        } catch (IOException ex) {
            LOGGER.error("Cannot read profile for {}", player.getUuid(), ex);
            return error(source, "Profile validation failed; no changes made. Check server log.");
        }
    }

    private static int legacy(ServerCommandSource source, ServerPlayerEntity player) {
        var tags = LegacyTagView.progressionTags(player.getCommandTags());
        String preview = String.join(", ", tags.stream().limit(16).toList());
        return feedback(source, player.getName().getString() + " legacy tags (" + tags.size() +
            "): " + (tags.isEmpty() ? "(none)" : preview) +
            (tags.size() > 16 ? " ... (first 16)" : ""));
    }

    private static int stage(ServerCommandSource source, ServerPlayerEntity player, String stage) {
        if (!config.allowAdminMutations())
            return error(source, "Admin writes disabled. Enable in STAGING config and restart.");
        try {
            var result = service(source).setStage(player.getUuid(), stage);
            return feedback(source, "Story stage " + player.getName().getString() +
                ": " + result.profile().storyStage() + " | revision=" +
                result.profile().revision() + " | changed=" + result.changed());
        } catch (IOException | IllegalArgumentException ex) {
            LOGGER.warn("Rejected stage change for {}", player.getUuid(), ex);
            return error(source, "Stage rejected or storage unavailable. No external data changed.");
        }
    }

    private static int fact(ServerCommandSource source, ServerPlayerEntity player,
                            String fact, boolean present) {
        if (!config.allowAdminMutations())
            return error(source, "Admin writes disabled. Enable in STAGING config and restart.");
        try {
            var result = present ? service(source).grant(player.getUuid(), fact)
                : service(source).revoke(player.getUuid(), fact);
            return feedback(source, (present ? "Grant " : "Revoke ") + fact +
                " | " + player.getName().getString() +
                " | revision=" + result.profile().revision() +
                " | changed=" + result.changed());
        } catch (IOException | IllegalArgumentException ex) {
            LOGGER.warn("Rejected fact change for {}", player.getUuid(), ex);
            return error(source, "Fact rejected or storage unavailable. No external data changed.");
        }
    }

    private static int feedback(ServerCommandSource source, String value) {
        source.sendFeedback(() -> Text.literal(value), false);
        return 1;
    }

    private static int error(ServerCommandSource source, String value) {
        source.sendError(Text.literal(value));
        return 0;
    }
}
