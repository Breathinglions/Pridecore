package com.prideregion.core;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/** Staging progression foundation: NO tag mutation, NO rewards, NO external mod state changes. */
public final class PrideCoreMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrideCore");
    private static volatile PrideCoreConfig CONFIG = new PrideCoreConfig(false);

    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                CONFIG = PrideCoreConfig.load(FabricLoader.getInstance().getConfigDir());
                LOGGER.info("Pride Core 0.1 foundation online. Admin writes enabled: {}", CONFIG.allowAdminMutations());
            } catch (IOException ex) {
                CONFIG = new PrideCoreConfig(false);
                LOGGER.error("Pride Core config unavailable; admin writes disabled", ex);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = CommandManager.literal("pridecore")
                .then(CommandManager.literal("profile").executes(ctx -> profile(ctx.getSource())))
                .then(CommandManager.literal("status")
                    .requires(src -> src.hasPermissionLevel(4))
                    .executes(ctx -> feedback(ctx.getSource(), "Pride Core 0.1-alpha.1 | admin writes="
                        + CONFIG.allowAdminMutations() + " | legacy tags read-only")));
            var admin = CommandManager.literal("admin")
                .requires(src -> src.hasPermissionLevel(4));
            admin.then(CommandManager.literal("inspect")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> inspect(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))));
            admin.then(CommandManager.literal("legacy")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> legacy(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))));
            for (String action : List.of("grant", "revoke")) {
                admin.then(CommandManager.literal(action)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("fact", StringArgumentType.word())
                            .executes(ctx -> fact(ctx.getSource(),
                                EntityArgumentType.getPlayer(ctx, "player"),
                                StringArgumentType.getString(ctx, "fact"), action.equals("grant"))))));
            }
            admin.then(CommandManager.literal("stage")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("stage", StringArgumentType.word())
                        .executes(ctx -> stage(ctx.getSource(),
                            EntityArgumentType.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "stage"))))));
            dispatcher.register(root.then(admin));
        });
    }

    private static ProfileStore store(ServerCommandSource source) {
        return new ProfileStore(source.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("data").resolve("pride_core").resolve("players"));
    }

    private static int profile(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return error(source, "Use /pridecore admin inspect <online-player> from console.");
        return inspect(source, player);
    }

    private static int inspect(ServerCommandSource source, ServerPlayerEntity player) {
        try {
            var p = store(source).load(player.getUuid());
            return feedback(source, "Trainer " + player.getName().getString() +
                " | story=" + p.storyStage() + " | facts=" + p.facts().size() +
                " | revision=" + p.revision() +
                " | legacy tags=" + LegacyTagView.progressionTags(player.getCommandTags()).size());
        } catch (IOException ex) {
            LOGGER.error("Cannot read profile for {}", player.getUuid(), ex);
            return error(source, "Profile read failed; no data was modified. Check server log.");
        }
    }

    private static int legacy(ServerCommandSource source, ServerPlayerEntity player) {
        var tags = LegacyTagView.progressionTags(player.getCommandTags());
        String preview = String.join(", ", tags.stream().limit(12).toList());
        return feedback(source, player.getName().getString() + " legacy tags (" + tags.size()
            + "): " + (tags.isEmpty() ? "(none)" : preview) + (tags.size() > 12 ? " ..." : ""));
    }

    private static int stage(ServerCommandSource source, ServerPlayerEntity player, String stage) {
        if (!CONFIG.allowAdminMutations())
            return error(source, "Admin writes disabled. Enable allow_admin_mutations on STAGING then restart.");
        try {
            var old = store(source).load(player.getUuid());
            var next = old.withStage(stage);
            if (next != old) store(source).save(next);
            return feedback(source, "Story stage for " + player.getName().getString() + ": " + next.storyStage());
        } catch (IllegalArgumentException | IOException ex) {
            LOGGER.warn("Rejected stage write for {}", player.getUuid(), ex);
            return error(source, "Invalid stage or storage error. No legacy tags changed.");
        }
    }

    private static int fact(ServerCommandSource source, ServerPlayerEntity player, String fact, boolean present) {
        if (!CONFIG.allowAdminMutations())
            return error(source, "Admin writes disabled. Enable allow_admin_mutations on STAGING then restart.");
        try {
            var old = store(source).load(player.getUuid());
            var next = old.withFact(fact, present);
            if (next != old) store(source).save(next);
            return feedback(source, (present ? "Granted " : "Revoked ") + fact + " for "
                + player.getName().getString() + " (revision " + next.revision() + ")");
        } catch (IllegalArgumentException | IOException ex) {
            LOGGER.warn("Rejected fact write for {}", player.getUuid(), ex);
            return error(source, "Invalid fact or storage error. No legacy tags changed.");
        }
    }

    private static int feedback(ServerCommandSource src, String value) {
        src.sendFeedback(() -> Text.literal(value), false);
        return 1;
    }

    private static int error(ServerCommandSource src, String value) {
        src.sendError(Text.literal(value));
        return 0;
    }
}
