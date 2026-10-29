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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private static volatile StoryBattleProof battleProof;
    private static volatile StoryBattleSessionRouter battleRouter;
    private static volatile ResonanceFinaleController finale;
    private static volatile String health = "disabled";
    private static volatile String configStatus = "unchecked";

    private StoryModule() {}

    public static void registerHooks() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity sp) || hand != Hand.MAIN_HAND)
                return ActionResult.PASS;
            // Environmental evidence uses ONLY intentionally placed interaction
            // entities. Never intercept unrelated world props or native NPCs.
            if (entity.getType() == EntityType.INTERACTION) {
                List<StoryEvidenceEngine.Clue> clues = StoryEvidenceEngine.definitions()
                    .stream().filter(clue -> entity.getCommandTags().contains(clue.markerTag()))
                    .toList();
                if (clues.isEmpty()) return ActionResult.PASS;
                if (clues.size() != 1 || entity.squaredDistanceTo(sp) > 36.0D)
                    return ActionResult.FAIL;
                try {
                    var service = PrideCoreMod.progression(sp.getServer())
                        .orElseThrow(() -> new IOException("Progression unavailable"));
                    var evidence = new StoryEvidenceEngine(service,
                        id -> id.equals(sp.getUuid()) ? Set.copyOf(sp.getCommandTags()) : Set.of());
                    var result = evidence.complete(sp.getUuid(), clues.getFirst().id());
                    StoryEngine running = liveServer == sp.getServer() ? engine : null;
                    if (running != null) StoryAdvancementBridge.synchronize(sp, running);
                    sp.sendMessage(Text.literal(result.firstDiscovery()
                            ? "[Pride Region] Evidence discovered: " + result.clue().title()
                            : "[Pride Region] You've already examined this evidence.")
                        .formatted(Formatting.AQUA), false);
                } catch (IOException | RuntimeException ex) {
                    sp.sendMessage(Text.literal("Story evidence unavailable: " + safe(ex.getMessage()))
                        .formatted(Formatting.RED), false);
                }
                return ActionResult.SUCCESS;
            }
            if (entity.getType() != EntityType.VILLAGER)
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
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            StoryEngine active = liveServer == server ? engine : null;
            if (active == null) return;
            try {
                // Read old *verified* onboarding markers, then project owned checkpoints.
                var migrated = active.reconcileOnboarding(handler.getPlayer().getUuid());
                boolean verdantWin = active.reconcileVerdantVictory(handler.getPlayer().getUuid());
                StoryAdvancementBridge.synchronize(handler.getPlayer(), active);
                if (!migrated.isEmpty())
                    LOGGER.info("Reconciled legacy onboarding for {}: {}",
                        handler.getPlayer().getUuid(), migrated);
                if (verdantWin)
                    sendNextObjective(handler.getPlayer(),
                        active.currentStage(handler.getPlayer().getUuid()));
            } catch (IOException | RuntimeException ex) {
                LOGGER.warn("Read-only story advancement reconciliation failed for {}: {}",
                    handler.getPlayer().getUuid(), ex.toString());
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            StoryEngine running = liveServer == server ? engine : null;
            if (running != null) running.cancel(handler.getPlayer().getUuid());
            StoryBattleSessionRouter router = liveServer == server ? battleRouter : null;
            if (router != null) router.disconnect(handler.getPlayer().getUuid());
            ResonanceFinaleController activeFinale = liveServer == server ? finale : null;
            if (activeFinale != null) activeFinale.disconnect(handler.getPlayer().getUuid());
        });
    }

    public static void start(MinecraftServer server) {
        liveServer = server;
        engine = null;
        battleProof = null;
        battleRouter = null;
        finale = null;
        health = "disabled";
        configStatus = "unchecked";
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
            try (var bundled = StoryModule.class.getResourceAsStream("/story/default.json")) {
                if (bundled == null) throw new IOException("Bundled default story missing");
                var update = StoryConfigManager.upgrade(storyFile(), bundled);
                configStatus = update.change().name().toLowerCase(Locale.ROOT);
                if (update.backup() != null) {
                    LOGGER.info("Story config safely upgraded, previous file backed up at {} (added {} NPCs and {} legacy prerequisites)",
                        update.backup(), update.newNpcs(), update.newGates());
                }
            }
            if (!"true".equalsIgnoreCase(props.getProperty("story_enabled", "false").trim())) {
                LOGGER.info("Pride Story module disabled; existing NPCs are untouched");
                return;
            }
            var service = PrideCoreMod.progression(server)
                .orElseThrow(() -> new IOException("Module 01 progression unavailable"));
            var loaded = new StoryEngine(StoryJson.load(storyFile()), service, playerId -> {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                return player == null ? Set.of() : Set.copyOf(player.getCommandTags());
            });
            battleProof = new StoryBattleProof(service);
            battleRouter = new StoryBattleSessionRouter(battleProof);
            finale = new ResonanceFinaleController();
            engine = loaded;
            health = "active";
            LOGGER.info("Pride Story module active: {} NPC definitions", loaded.npcCount());
        } catch (IOException | RuntimeException ex) {
            engine = null;
            battleProof = null;
            battleRouter = null;
            finale = null;
            health = "error (see server log)";
            configStatus = "error";
            LOGGER.error("Pride Story initialization refused; no world/NPC changes made", ex);
        }
    }

    public static void stop(MinecraftServer server) {
        if (liveServer == server) {
            if (finale != null) finale.clear();
            finale = null;
            if (battleRouter != null) battleRouter.clear();
            battleRouter = null;
            if (battleProof != null) battleProof.clear();
            battleProof = null;
            engine = null;
            liveServer = null;
            health = "disabled";
            configStatus = "unchecked";
        }
    }

    /**
     * Server-side integration API for a verified native Cobblemon battle adapter.
     * Do not invoke from chat commands, NPC dialogue options or client packets.
     * Adapters must validate the actual configured opponent and battle UUID.
     */
    public static StoryBattleProof.Ticket beginVerifiedBattle(
            MinecraftServer server, UUID playerId, UUID battleId, String encounterId)
            throws IOException {
        StoryBattleProof proof = liveServer == server ? battleProof : null;
        if (proof == null || engine == null)
            throw new IllegalStateException("Story battles disabled");
        if (!server.isOnThread())
            throw new IllegalStateException("Story battle start must run on server thread");
        if (server.getPlayerManager().getPlayer(playerId) == null)
            throw new IllegalStateException("Story battle player offline");
        return proof.arm(playerId, battleId, encounterId);
    }

    /** Only a trusted native battle victory event may call this method. */
    public static boolean completeVerifiedBattle(
            MinecraftServer server, StoryBattleProof.Ticket ticket,
            UUID actualBattleId, UUID actualWinnerId) throws IOException {
        StoryBattleProof proof = liveServer == server ? battleProof : null;
        if (proof == null || engine == null) return false;
        if (!server.isOnThread())
            throw new IllegalStateException("Story battle win must run on server thread");
        if (ticket == null || server.getPlayerManager().getPlayer(ticket.player()) == null)
            return false;
        boolean completed = proof.victory(ticket, actualBattleId, actualWinnerId);
        if (completed) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(ticket.player());
            player.sendMessage(Text.literal("[Pride Region] Story battle victory recorded.")
                .formatted(Formatting.GREEN), false);
        }
        return completed;
    }

    /** Loss, disconnect or genuine battle cancellation: no story credit. */
    public static void cancelVerifiedBattle(
            MinecraftServer server, UUID playerId, UUID battleId) {
        StoryBattleProof proof = liveServer == server ? battleProof : null;
        if (proof == null) return;
        if (!server.isOnThread())
            throw new IllegalStateException("Story battle cancellation must run on server thread");
        proof.cancel(playerId, battleId);
    }

    /**
     * Trusted native battle event adapter entrypoint. The caller must have
     * independently verified the native battle UUID, configured trainer
     * identity, and owning player. Never expose this as a player command.
     */
    public static boolean nativeStoryBattleStarted(MinecraftServer server,
            StoryBattleSessionRouter.NativeStart start) throws IOException {
        if (liveServer != server || engine == null || battleRouter == null) return false;
        if (!server.isOnThread()) throw new IllegalStateException("Must run on server thread");
        if (server.getPlayerManager().getPlayer(start.playerId()) == null) return false;
        return battleRouter.started(start);
    }

    /** Process only genuine resolved native battle results. */
    public static boolean nativeStoryBattleEnded(MinecraftServer server,
            UUID battleId, UUID winnerId) throws IOException {
        if (liveServer != server || engine == null || battleRouter == null) return false;
        if (!server.isOnThread()) throw new IllegalStateException("Must run on server thread");
        boolean result = battleRouter.ended(battleId, winnerId);
        if (result && winnerId != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(winnerId);
            if (player != null)
                player.sendMessage(Text.literal("[Pride Region] Story battle victory recorded.")
                    .formatted(Formatting.GREEN), false);
        }
        return result;
    }

    public static void nativeStoryBattleCancelled(MinecraftServer server, UUID battleId) {
        if (liveServer != server || battleRouter == null) return;
        if (!server.isOnThread()) throw new IllegalStateException("Must run on server thread");
        battleRouter.cancel(battleId);
    }

    /** Show each encounter's prerequisite and verified-win status; never grants anything. */
    private static int adminEncounters(ServerCommandSource source, ServerPlayerEntity target) {
        try {
            var profile = PrideCoreMod.progression(source.getServer())
                .orElseThrow(() -> new IOException("Progression unavailable"))
                .get(target.getUuid());
            source.sendFeedback(() -> Text.literal(
                "Module 02 scripted encounters for " + target.getName().getString() + ":"), false);
            for (var entry : StoryBattleProof.definitions()) {
                boolean unlocked = profile.facts().contains(entry.requiredFact());
                boolean won = profile.facts().contains(entry.earnedFact());
                String status = won ? "WON" : unlocked ? "ELIGIBLE (not launched)" : "LOCKED";
                source.sendFeedback(() -> Text.literal(" - " + entry.id() + ": " + status +
                    " [requires " + entry.requiredFact() + "]"), false);
            }
            return 1;
        } catch (IOException | RuntimeException ex) {
            source.sendError(Text.literal("Encounter inspection failed: " + safe(ex.getMessage())));
            return 0;
        }
    }

    /** Read-only operator diagnostics; never changes story facts or rewards. */
    private static int inspectTestState(ServerCommandSource source, ServerPlayerEntity target) {
        try {
            var profile = PrideCoreMod.progression(source.getServer())
                .orElseThrow(() -> new IOException("Progression unavailable"))
                .get(target.getUuid());
            var phase = finale == null ? null : finale.snapshot(target.getUuid());
            source.sendFeedback(() -> Text.literal(
                "Module 02 [" + target.getName().getString() + "] stage=" +
                profile.storyStage() + " revision=" + profile.revision() +
                " battleTickets=" + (battleRouter == null ? "disabled" : "active") +
                " finale=" + (phase == null ? "disabled" : phase.phase()) +
                " conduits=" + (phase == null ? 0 : phase.conduits()) +
                " shields=" + (phase == null ? 0 : phase.shields())), false);
            return 1;
        } catch (IOException | RuntimeException ex) {
            source.sendError(Text.literal("Module 02 inspect failed: " + safe(ex.getMessage())));
            return 0;
        }
    }

    /** Clears only temporary sessions; permanent story facts, badges and prizes remain intact. */
    private static int resetTestSession(ServerCommandSource source, ServerPlayerEntity target) {
        if (engine != null) engine.cancel(target.getUuid());
        if (battleRouter != null) battleRouter.disconnect(target.getUuid());
        if (finale != null) finale.disconnect(target.getUuid());
        LOGGER.info("Operator {} reset temporary Module 02 sessions for {}",
            source.getName(), target.getUuid());
        return message(source, "Temporary story, battle and finale sessions cleared for " +
            target.getName().getString() + "; saved progress preserved.");
    }

    /** Starts a finale rehearsal only when persisted victory and clue evidence are real. */
    private static int adminFinaleStart(ServerCommandSource source, ServerPlayerEntity target) {
        try {
            if (finale == null) throw new IllegalStateException("Story finale disabled");
            var profile = PrideCoreMod.progression(source.getServer())
                .orElseThrow(() -> new IOException("Progression unavailable"))
                .get(target.getUuid());
            var state = finale.start(target.getUuid(),
                profile.facts().contains("pride:cael_battle_won"),
                profile.facts().contains("pride:clue_engine"));
            if (state.phase() == ResonanceFinaleController.Phase.LOCKED)
                throw new IllegalStateException("Requires actual Cael victory and engine evidence");
            LOGGER.info("Operator {} started finale rehearsal for {}", source.getName(), target.getUuid());
            return message(source, "Finale phase for " + target.getName().getString() +
                ": " + state.phase() + " (no story facts or rewards granted).");
        } catch (IOException | RuntimeException ex) {
            source.sendError(Text.literal("Finale rehearsal refused: " + safe(ex.getMessage())));
            return 0;
        }
    }

    private static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("pride_core");
    }
    private static Path storyFile() { return configDir().resolve("story.json"); }

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> commands() {
        var story = CommandManager.literal("story");
        story.then(CommandManager.literal("status").executes(ctx -> status(ctx.getSource())));
        story.then(CommandManager.literal("objective").executes(ctx -> objective(ctx.getSource())));
        story.then(CommandManager.literal("evidence").executes(ctx -> evidenceStatus(ctx.getSource())));
        // The original RCT League Thorn is now the sole physical encounter.
        // Never expose the retired duplicate TBCS "Story Thorn" battle path.
        // Battle outcomes and one-time rewards remain RCT/League-owned.
        story.then(CommandManager.literal("sync")
            .executes(ctx -> sync(ctx.getSource())));
        story.then(CommandManager.literal("validate")
            .requires(source -> source.hasPermissionLevel(4))
            .executes(ctx -> validate(ctx.getSource())));
        story.then(CommandManager.literal("native")
            .then(CommandManager.argument("npc", StringArgumentType.word())
                .then(CommandManager.argument("choice", StringArgumentType.word())
                    .executes(ctx -> nativeChoice(ctx.getSource(),
                        StringArgumentType.getString(ctx, "npc"),
                        StringArgumentType.getString(ctx, "choice"))))));
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
        story.then(CommandManager.literal("admin")
            .requires(source -> source.hasPermissionLevel(4))
            .then(CommandManager.literal("inspect")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> inspectTestState(ctx.getSource(),
                        EntityArgumentType.getPlayer(ctx, "player")))))
            .then(CommandManager.literal("reset-session")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> resetTestSession(ctx.getSource(),
                        EntityArgumentType.getPlayer(ctx, "player")))))
            .then(CommandManager.literal("finale-start")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> adminFinaleStart(ctx.getSource(),
                        EntityArgumentType.getPlayer(ctx, "player"))))));
        story.then(CommandManager.literal("admin")
            .requires(source -> source.hasPermissionLevel(4))
            .then(CommandManager.literal("encounters")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> adminEncounters(ctx.getSource(),
                        EntityArgumentType.getPlayer(ctx, "player"))))));
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
            return message(source, "Module 02 story=" + health + " | config=" + configStatus);
        try {
            var service = PrideCoreMod.progression(source.getServer())
                .orElseThrow(() -> new IOException("Module 01 unavailable"));
            var p = service.get(player.getUuid());
            return message(source, "Story module=" + health + " | config=" + configStatus + " | stage=" + p.storyStage()
                + " | owned facts=" + p.facts().size() + " | revision=" + p.revision());
        } catch (IOException ex) {
            LOGGER.warn("Story status load failed for {}", player.getUuid(), ex);
            source.sendError(Text.literal("Story profile unavailable; check server log"));
            return 0;
        }
    }

    /**
     * Player-only, read-only external onboarding reconciliation. Normal native
     * Cobblemon dialogues call this after their original action is successful.
     */
    private static int sync(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Story sync requires a real player"));
            return 0;
        }
        try {
            StoryEngine active = ready(source.getServer());
            List<String> changes = active.reconcileOnboarding(player.getUuid());
            boolean verdantWin = active.reconcileVerdantVictory(player.getUuid());
            StoryAdvancementBridge.synchronize(player, active);
            if (!changes.isEmpty() || verdantWin)
                sendNextObjective(player, active.currentStage(player.getUuid()));
            return 1;
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Onboarding sync refused for {}: {}", player.getUuid(), ex.toString());
            source.sendError(Text.literal("Story sync unavailable: " + safe(ex.getMessage())));
            return 0;
        }
    }

    /** Read-only investigator notebook: never fabricates or grants a clue. */
    private static int evidenceStatus(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return message(source, "In-game player required.");
        try {
            var service = PrideCoreMod.progression(source.getServer())
                .orElseThrow(() -> new IOException("Module 01 unavailable"));
            Set<String> clues = StoryEvidenceEngine.discovered(service.get(player.getUuid()));
            List<String> titles = new ArrayList<>();
            for (var clue : StoryEvidenceEngine.definitions())
                if (clues.contains(clue.id())) titles.add(clue.title());
            return message(source, "Evidence found " + clues.size() + "/"
                + StoryEvidenceEngine.definitions().size() + ": "
                + (titles.isEmpty() ? "None yet." : String.join("; ", titles)));
        } catch (IOException ex) {
            source.sendError(Text.literal("Evidence status unavailable."));
            return 0;
        }
    }

    /** Player-requested reminder: no cooldown, no mutation, no admin permission needed. */
    private static int objective(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return message(source, "In-game player required.");
        try {
            String stage = ready(source.getServer()).currentStage(player.getUuid());
            sendNextObjective(player, stage);
            return 1;
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Cannot read story objective for {}: {}", player.getUuid(), ex.toString());
            source.sendError(Text.literal("Story objective unavailable; check the server log."));
            return 0;
        }
    }

    private static void sendNextObjective(ServerPlayerEntity player, String stage) {
        player.sendMessage(Text.literal("[Pride Region] Next objective: ")
            .formatted(Formatting.GOLD)
            .append(Text.literal(StoryObjectiveGuide.forStage(stage))
                .formatted(Formatting.YELLOW)), false);
    }

    private static int validate(ServerCommandSource source) {
        try {
            StoryDefinition definition = StoryJson.load(storyFile());
            return message(source, "Story config valid: " + definition.npcs().size()
                + " NPCs. Legacy tags remain read-only.");
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Story config validation failed", ex);
            source.sendError(Text.literal("Story config invalid: " + safe(ex.getMessage())));
            return 0;
        }
    }

    /**
     * Called by an existing Cobblemon GUI option through its documented q.run_command
     * context: execute as <actual player> at @s run pridecore story native <npc> <choice>.
     * No player name accepted from command arguments, no OP privileges granted.
     * A matching tagged native Cobblemon NPC must be physically near the player.
     */
    // All 13 official League trainers retain their existing RCT battles and
    // first-win callbacks. Narrative synchronization reads verified League
    // tags; Pride Core never launches a second shadow trainer battle.

    private static int nativeChoice(ServerCommandSource source, String npc, String option) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Native story actions require a real player"));
            return 0;
        }
        try {
            if (!StoryDefinition.validId(npc) || !StoryDefinition.validId(option))
                throw new IllegalArgumentException("Unrecognized story action");
            String nativeTag = NativeNpcIdentity.tagFor(npc);
            boolean nearby = false;
            for (Entity candidate : player.getWorld().getOtherEntities(player,
                    player.getBoundingBox().expand(5.0),
                    e -> "cobblemon:npc".equals(Registries.ENTITY_TYPE.getId(e.getType()).toString())
                        && e.getCommandTags().contains(nativeTag))) {
                if (candidate.squaredDistanceTo(player) <= 36.0D) {
                    nearby = true;
                    break;
                }
            }
            if (!nearby)
                throw new IllegalStateException("Talk to the matching Cobblemon NPC in person");
            StoryEngine active = ready(source.getServer());
            // Cobblemon native dialogues are player-facing. Never echo internal
            // profile revisions or other developer details to regular players.
            // A previously completed NPC choice is a safe no-op rather than an
            // "Unknown dialogue choice" error on the repeat conversation.
            if (active.completedNpcs(player.getUuid()).contains(npc)) {
                StoryAdvancementBridge.synchronize(player, active);
                return 1;
            }
            String previousStage = active.currentStage(player.getUuid());
            active.begin(player.getUuid(), npc); // Refresh against actual owned state.
            active.choose(player.getUuid(), option);
            StoryAdvancementBridge.synchronize(player, active);
            String updatedStage = active.currentStage(player.getUuid());
            if (!previousStage.equals(updatedStage))
                sendNextObjective(player, updatedStage);
            LOGGER.debug("Native story checkpoint completed: npc={}, player={}", npc, player.getUuid());
            return 1;
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Native story action rejected for {} npc={} choice={}: {}",
                    player.getUuid(), npc, option, ex.toString());
            source.sendError(Text.literal("Story action unavailable: " + safe(ex.getMessage())));
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
            StoryEngine active = ready(source.getServer());
            String previousStage = active.currentStage(player.getUuid());
            show(player, active.choose(player.getUuid(), id));
            active.reconcileOnboarding(player.getUuid());
            StoryAdvancementBridge.synchronize(player, active);
            String updatedStage = active.currentStage(player.getUuid());
            if (!previousStage.equals(updatedStage))
                sendNextObjective(player, updatedStage);
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
            StoryEngine next = new StoryEngine(def, service, playerId -> {
                ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(playerId);
                return player == null ? Set.of() : Set.copyOf(player.getCommandTags());
            });
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
