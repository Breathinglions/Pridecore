package com.prideregion.core.story;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.prideregion.core.PrideCoreMod;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.world.ServerWorld;
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
    private static volatile StoryBattleLaunchLedger launchLedger;
    private static volatile CobblemonStoryVictoryListener victoryListener;
    private static volatile ResonanceFinaleController finale;
    private static volatile ResonanceCheckpointStore finaleStore;
    private static volatile String health = "disabled";
    private static volatile String configStatus = "unchecked";
    private static final StoryCinematicTimeline CINEMATICS = new StoryCinematicTimeline();
    private static final StoryBattlePhaseTracker BATTLE_PHASES = new StoryBattlePhaseTracker();

    private StoryModule() {}

    public static void registerHooks() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity sp) || hand != Hand.MAIN_HAND)
                return ActionResult.PASS;
            // Physical finale devices are explicitly tagged interaction entities.
            // No world coordinates are guessed; builders place each device.
            if (entity.getType() == EntityType.INTERACTION &&
                    entity.getCommandTags().stream().anyMatch(t ->
                        t.startsWith("pride_finale_"))) {
                if (entity.squaredDistanceTo(sp) > 36.0D
                        || !entity.getWorld().getRegistryKey().equals(sp.getWorld().getRegistryKey()))
                    return ActionResult.FAIL;
                var markers = entity.getCommandTags().stream()
                    .filter(t -> t.startsWith("pride_finale_")).toList();
                if (markers.size() != 1) return ActionResult.FAIL;
                try {
                    if (finale == null || liveServer != sp.getServer())
                        throw new IllegalStateException("Story finale is disabled");
                    restoreFinale(sp.getUuid());
                    reconcileFinaleShields(sp.getUuid());
                    String marker = markers.getFirst();
                    // A normal eligible player can initiate the operation by
                    // using its first conduit. Operator rehearsal is optional.
                    if (marker.startsWith("pride_finale_conduit_")
                            && finale.snapshot(sp.getUuid()).phase()
                                == ResonanceFinaleController.Phase.LOCKED) {
                        var progression = PrideCoreMod.progression(sp.getServer())
                            .orElseThrow(() -> new IOException("Progression unavailable"));
                        var facts = progression.get(sp.getUuid()).facts();
                        var started = finale.start(sp.getUuid(),
                            facts.contains("pride:cael_battle_won"),
                            facts.contains("pride:clue_engine"));
                        if (started.phase() == ResonanceFinaleController.Phase.LOCKED)
                            throw new IllegalStateException(
                                "Defeat Director Cael and investigate the engine first");
                        persistFinale(sp.getUuid());
                    }
                    if (marker.equals("pride_finale_core")) {
                        var beforeCore = finale.snapshot(sp.getUuid());
                        if (beforeCore.phase() != ResonanceFinaleController.Phase.STABILIZE
                                && beforeCore.phase() != ResonanceFinaleController.Phase.COMPLETE)
                            throw new IllegalStateException(
                                "Disable all three shield defenders before stabilizing the core");
                        if (beforeCore.phase() == ResonanceFinaleController.Phase.STABILIZE)
                            finale.stabilize(sp.getUuid());
                        persistFinale(sp.getUuid());
                        completeResonanceFinale(sp.getServer(), sp.getUuid());
                        if (beforeCore.phase() == ResonanceFinaleController.Phase.STABILIZE) {
                            CINEMATICS.cancel(sp.getUuid());
                            CINEMATICS.begin(sp.getUuid(), "finale_stabilized");
                        }
                        // Repeat core use is safe but should not replay the
                        // cinematic burst after completion.
                        if (beforeCore.phase() == ResonanceFinaleController.Phase.STABILIZE
                                && sp.getWorld() instanceof ServerWorld fxWorld) {
                            StoryEffects.emit(fxWorld, "finale.end_rod", ParticleTypes.END_ROD,
                                entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                                140, 1.5D, 1.5D, 1.5D, 0.09D);
                            StoryEffects.emit(fxWorld, "finale.totem", ParticleTypes.TOTEM_OF_UNDYING,
                                entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                                80, 1.2D, 1.3D, 1.2D, 0.05D);
                        }
                        if (finaleStore != null) finaleStore.delete(sp.getUuid());
                    } else if (marker.startsWith("pride_finale_conduit_")) {
                        String direction = marker.substring("pride_finale_conduit_".length());
                        var priorConduits = finale.snapshot(sp.getUuid());
                        var state = finale.conduit(sp.getUuid(), direction);
                        persistFinale(sp.getUuid());
                        if (priorConduits.phase() == ResonanceFinaleController.Phase.CONDUITS
                                && state.phase() == ResonanceFinaleController.Phase.SHIELDS) {
                            CINEMATICS.cancel(sp.getUuid());
                            CINEMATICS.begin(sp.getUuid(), "finale_conduits_complete");
                        }
                        if (sp.getWorld() instanceof ServerWorld fxWorld) {
                            StoryEffects.emit(fxWorld, "finale.conduit", ParticleTypes.ELECTRIC_SPARK,
                                entity.getX(), entity.getY() + 0.6D, entity.getZ(),
                                36, 0.65D, 0.8D, 0.65D, 0.055D);
                            StoryEffects.emit(fxWorld, "finale.end_rod", ParticleTypes.END_ROD,
                                entity.getX(), entity.getY() + 0.6D, entity.getZ(),
                                12, 0.45D, 0.6D, 0.45D, 0.02D);
                        }
                        sp.sendMessage(Text.literal("[Pride Region] Resonance conduits " +
                            state.conduits() + "/4; phase " + state.phase())
                            .formatted(Formatting.AQUA), false);
                    } else {
                        throw new IllegalArgumentException("Unknown finale device");
                    }
                } catch (IOException | RuntimeException ex) {
                    sp.sendMessage(Text.literal("Finale interaction denied: " +
                        safe(ex.getMessage())).formatted(Formatting.RED), false);
                }
                return ActionResult.SUCCESS;
            }
            // Environmental evidence uses ONLY intentionally placed interaction
            // entities. Never intercept unrelated world props or native NPCs.
            if (entity.getType() == EntityType.INTERACTION) {
                List<StoryEvidenceEngine.Clue> clues = StoryEvidenceEngine.definitions()
                    .stream().filter(clue -> entity.getCommandTags().contains(clue.markerTag()))
                    .toList();
                if (clues.isEmpty()) return ActionResult.PASS;
                if (clues.size() != 1 || entity.squaredDistanceTo(sp) > 36.0D
                        || !entity.getWorld().getRegistryKey().equals(sp.getWorld().getRegistryKey()))
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
            // Intentionally placed story battle trainers. Only living
            // entities carrying one exact, registered encounter tag qualify.
            // Unrelated RCT NPCs and official League trainers are untouched.
            if (entity instanceof net.minecraft.entity.LivingEntity trainer) {
                final String battlePrefix = "pride_battle_";
                var battleTags = entity.getCommandTags().stream()
                    .filter(t -> t.startsWith(battlePrefix)).toList();
                if (!battleTags.isEmpty()) {
                    if (battleTags.size() != 1 || entity.squaredDistanceTo(sp) > 36.0D
                            || !entity.getWorld().getRegistryKey().equals(sp.getWorld().getRegistryKey()))
                        return ActionResult.FAIL;
                    String encounterId = battleTags.getFirst().substring(battlePrefix.length());
                    if (!StoryBattleSessionRouter.acceptedTrainers().containsKey(encounterId))
                        return ActionResult.FAIL;
                    try {
                        var launched = launchRctStoryBattle(sp.getServer(), sp, trainer, encounterId);
                        if (launched.isEmpty())
                            sp.sendMessage(Text.literal(
                                "[Pride Region] This encounter isn't available yet, or is already completed.")
                                .formatted(Formatting.YELLOW), false);
                    } catch (IOException | RuntimeException ex) {
                        LOGGER.warn("Story trainer battle launch denied for {} / {}",
                            sp.getUuid(), encounterId, ex);
                        sp.sendMessage(Text.literal("Story battle unavailable: " +
                            safe(ex.getMessage())).formatted(Formatting.RED), false);
                    }
                    return ActionResult.SUCCESS;
                }
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
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (liveServer != server || engine == null) return;
            for (var scene : CINEMATICS.tick()) {
                ServerPlayerEntity viewer = server.getPlayerManager().getPlayer(scene.player());
                if (viewer == null) { CINEMATICS.cancel(scene.player()); continue; }
                try {
                    viewer.sendMessage(Text.literal(scene.cue().title())
                        .formatted(Formatting.GOLD), true);
                    if (viewer.getWorld() instanceof ServerWorld fxWorld) {
                        // Short vanilla audio sting on each authenticated scene
                        // entrance. No client-only sound mod is required.
                        if (scene.cue().tick() == 0)
                            fxWorld.playSound(null, viewer.getBlockPos(),
                                scene.encounter().startsWith("finale_")
                                    ? SoundEvents.BLOCK_BEACON_ACTIVATE
                                    : SoundEvents.BLOCK_BEACON_DEACTIVATE,
                                SoundCategory.PLAYERS, 0.55F, 0.85F);
                        var fallback = scene.cue().effectKey().equals("battle.electric")
                            || scene.cue().effectKey().equals("finale.shield")
                            ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.ENCHANT;
                        StoryEffects.emit(fxWorld, scene.cue().effectKey(), fallback,
                            viewer.getX(), viewer.getY() + 1.1D, viewer.getZ(),
                            scene.cue().particles(), 0.8D, 0.9D, 0.8D, 0.035D);
                    }
                } catch (RuntimeException visualError) {
                    LOGGER.warn("Story cinematic cue could not display for {}",
                        scene.player(), visualError);
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            CINEMATICS.cancel(handler.getPlayer().getUuid());
            BATTLE_PHASES.cancelPlayer(handler.getPlayer().getUuid());
            StoryEngine running = liveServer == server ? engine : null;
            if (running != null) running.cancel(handler.getPlayer().getUuid());
            StoryBattleSessionRouter router = liveServer == server ? battleRouter : null;
            if (router != null) router.disconnect(handler.getPlayer().getUuid());
            if (launchLedger != null) launchLedger.cancelPlayer(handler.getPlayer().getUuid());
            ResonanceFinaleController activeFinale = liveServer == server ? finale : null;
            if (activeFinale != null) activeFinale.disconnect(handler.getPlayer().getUuid());
        });
    }

    public static void start(MinecraftServer server) {
        liveServer = server;
        CINEMATICS.clear();
        BATTLE_PHASES.clear();
        engine = null;
        battleProof = null;
        battleRouter = null;
        launchLedger = null;
        finale = null;
        finaleStore = null;
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
            StoryEffects.reset();
            for (String key : StoryEffects.keys()) {
                String configuredParticle = props.getProperty("effects." + key, "").trim();
                if (!StoryEffects.configure(key, configuredParticle))
                    LOGGER.warn("Unknown story particle override effects.{}={}, using vanilla fallback",
                        key, configuredParticle);
            }
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
            // Fail closed if the runtime cannot load all approved trainer teams.
            StoryBattleRosters.bundled();
            StoryBattleRuntimePreflight.verify(StoryModule.class.getClassLoader());
            battleProof = new StoryBattleProof(service);
            battleRouter = new StoryBattleSessionRouter(battleProof);
            launchLedger = new StoryBattleLaunchLedger();
            finale = new ResonanceFinaleController();
            finaleStore = new ResonanceCheckpointStore(directory.resolve("finale_checkpoints"));
            // Native Cobblemon 1.8.1 callback; only already-registered RCT
            // battles can reach the authenticated story victory router.
            victoryListener = CobblemonStoryVictoryListener.install(
                StoryModule.class.getClassLoader(), new CobblemonStoryVictoryListener.Sink() {
                    public boolean isRegistered(UUID battleId) {
                        return battleRouter != null &&
                            battleRouter.encounterForBattle(battleId).isPresent();
                    }
                    public void victory(UUID battleId, UUID playerId) {
                        server.execute(() -> {
                            try { nativeStoryBattleEnded(server, battleId, playerId); }
                            catch (IOException | RuntimeException ex) {
                                LOGGER.error("Verified Cobblemon story result failed", ex);
                            }
                        });
                    }
                    public void cancelled(UUID battleId) {
                        server.execute(() -> nativeStoryBattleCancelled(server, battleId));
                    }
                    public void fainted(UUID battleId, UUID pokemonId) {
                        server.execute(() -> nativeStoryOpponentFainted(server, battleId, pokemonId));
                    }
                });
            engine = loaded;
            health = "active";
            LOGGER.info("Pride Story module active: {} NPC definitions", loaded.npcCount());
        } catch (IOException | ReflectiveOperationException | LinkageError | RuntimeException ex) {
            if (victoryListener != null) {
                try { victoryListener.uninstall(); }
                catch (ReflectiveOperationException cleanup) {
                    LOGGER.error("Story startup failed and battle listener cleanup failed", cleanup);
                }
                victoryListener = null;
            }
            if (launchLedger != null) launchLedger.clear();
            launchLedger = null;
            if (battleRouter != null) battleRouter.clear();
            engine = null;
            battleProof = null;
            battleRouter = null;
            finale = null;
            finaleStore = null;
            CINEMATICS.clear();
            BATTLE_PHASES.clear();
            health = "error (see server log)";
            configStatus = "error";
            LOGGER.error("Pride Story initialization refused; no world/NPC changes made", ex);
        }
    }

    public static void stop(MinecraftServer server) {
        if (liveServer == server) {
            CINEMATICS.clear();
            BATTLE_PHASES.clear();
            if (victoryListener != null) {
                try { victoryListener.uninstall(); }
                catch (ReflectiveOperationException ex) {
                    LOGGER.error("Could not unsubscribe Cobblemon story listener", ex);
                }
                victoryListener = null;
            }
            if (finale != null) finale.clear();
            finale = null;
            finaleStore = null;
            if (battleRouter != null) battleRouter.clear();
            battleRouter = null;
            if (launchLedger != null) launchLedger.clear();
            launchLedger = null;
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
        if (start.encounterId().startsWith("shield_")) {
            restoreFinale(start.playerId());
            if (finale == null || finale.snapshot(start.playerId()).phase()
                    != ResonanceFinaleController.Phase.SHIELDS)
                return false;
        }
        return battleRouter.started(start);
    }

    /**
     * Called by trusted server-side RCT launch code before invoking startBattle.
     * Never expose this bridge through a client packet or player command.
     */
    public static boolean reserveStoryBattleLaunch(MinecraftServer server, UUID player,
            String encounterId, UUID trainerEntity) throws IOException {
        if (liveServer != server || engine == null || launchLedger == null) return false;
        if (!server.isOnThread()) throw new IllegalStateException("Must run on server thread");
        if (server.getPlayerManager().getPlayer(player) == null) return false;
        var profile = PrideCoreMod.progression(server)
            .orElseThrow(() -> new IOException("Progression unavailable")).get(player);
        var def = StoryBattleProof.definitions().stream()
            .filter(e -> e.id().equals(encounterId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown story encounter"));
        if (!profile.facts().contains(def.requiredFact()) ||
                profile.facts().contains(def.earnedFact())) return false;
        if (encounterId.startsWith("shield_")) {
            restoreFinale(player);
            if (finale == null || finale.snapshot(player).phase()
                    != ResonanceFinaleController.Phase.SHIELDS) return false;
        }
        return launchLedger.reserve(new StoryBattleLaunchLedger.Launch(
            player, encounterId, trainerEntity));
    }

    /** Called only after the trusted RCT launcher returns the real battle UUID. */
    public static boolean confirmStoryBattleLaunch(MinecraftServer server, UUID player,
            UUID trainerEntity, UUID nativeBattle) throws IOException {
        if (liveServer != server || launchLedger == null) return false;
        if (!server.isOnThread()) throw new IllegalStateException("Must run on server thread");
        var start = launchLedger.launched(player, trainerEntity, nativeBattle);
        if (start.isEmpty()) return false;
        boolean ok = nativeStoryBattleStarted(server, start.get());
        if (!ok) launchLedger.finished(nativeBattle);
        return ok;
    }

    public static void cancelPendingStoryBattleLaunch(MinecraftServer server, UUID player) {
        if (liveServer != server || launchLedger == null) return;
        if (!server.isOnThread()) throw new IllegalStateException("Must run on server thread");
        launchLedger.cancelPlayer(player);
    }

    /**
     * Trusted server-only launch for a pre-identified story NPC. Invoked by
     * the native NPC adapter after it validates encounter ownership.
     * No player or admin command can launch this through client input.
     */
    public static Optional<UUID> launchRctStoryBattle(MinecraftServer server,
            ServerPlayerEntity player, net.minecraft.entity.LivingEntity trainer,
            String encounterId) throws IOException {
        if (liveServer != server || engine == null || launchLedger == null)
            return Optional.empty();
        if (!server.isOnThread()) throw new IllegalStateException("Must run on server thread");
        if (!reserveStoryBattleLaunch(server, player.getUuid(),
                encounterId, trainer.getUuid())) return Optional.empty();
        try {
            UUID battleId = RctStoryBattleLauncher.launch(encounterId, player, trainer,
                player.getUuid(), StoryModule.class.getClassLoader());
            if (!confirmStoryBattleLaunch(server, player.getUuid(),
                    trainer.getUuid(), battleId)) {
                nativeStoryBattleCancelled(server, battleId);
                try {
                    if (!RctStoryBattleLauncher.abort(battleId,
                            StoryModule.class.getClassLoader()))
                        LOGGER.error("Unconfirmed RCT battle {} could not be aborted", battleId);
                } catch (ReflectiveOperationException abortError) {
                    LOGGER.error("Failed to abort unconfirmed RCT battle {}", battleId, abortError);
                }
                throw new IOException("RCT battle launched without valid story ownership");
            }
            try {
                var roster = StoryBattleRosters.bundled().find(encounterId).orElseThrow();
                BATTLE_PHASES.register(battleId, player.getUuid(), encounterId,
                    roster.team().size());
            } catch (IOException | RuntimeException phaseSetupFailure) {
                LOGGER.warn("Optional faint-based boss phase setup failed for {}",
                    battleId, phaseSetupFailure);
            }
            CINEMATICS.begin(player.getUuid(), encounterId);
            // Optional cinematic presentation must never unwind a confirmed
            // battle launch: after ownership confirmation the battle is live.
            try {
                if (player.getWorld() instanceof ServerWorld fxWorld) {
                    StoryEffects.emit(fxWorld, encounterId.startsWith("rin_") ? "battle.rin" :
                        encounterId.equals("sable") ? "battle.sable" :
                        encounterId.equals("director_cael") ? "battle.cael" :
                        "battle.enchant", ParticleTypes.ENCHANT,
                        trainer.getX(), trainer.getY() + 1.15D, trainer.getZ(),
                        28, 0.7D, 1.0D, 0.7D, 0.04D);
                    if (encounterId.equals("director_cael") ||
                            encounterId.equals("sable") ||
                            encounterId.startsWith("shield_")) {
                        StoryEffects.emit(fxWorld, "battle.electric", ParticleTypes.ELECTRIC_SPARK,
                            trainer.getX(), trainer.getY() + 1.1D, trainer.getZ(),
                            35, 0.9D, 1.0D, 0.9D, 0.06D);
                    }
                }
                var roster = StoryBattleRosters.bundled().find(encounterId).orElseThrow();
                player.sendMessage(Text.literal("[Pride Region] " + roster.intro())
                    .formatted(Formatting.GOLD), false);
            } catch (IOException | RuntimeException presentationFailure) {
                LOGGER.warn("RCT battle {} launched but cinematic introduction failed",
                    battleId, presentationFailure);
            }
            return Optional.of(battleId);
        } catch (IOException | ReflectiveOperationException | RuntimeException ex) {
            // Release failed reservations without discarding a previously
            // confirmed active battle (which belongs to the result callback).
            if (launchLedger != null && launchLedger.pending(player.getUuid())
                    && !launchLedger.active(player.getUuid()))
                cancelPendingStoryBattleLaunch(server, player.getUuid());
            if (ex instanceof IOException io) throw io;
            throw new IOException("RCT story battle launch failed", ex);
        }
    }

    /** Process only genuine resolved native battle results. */
    public static boolean nativeStoryBattleEnded(MinecraftServer server,
            UUID battleId, UUID winnerId) throws IOException {
        if (liveServer != server || engine == null || battleRouter == null) return false;
        if (!server.isOnThread()) throw new IllegalStateException("Must run on server thread");
        // Native callbacks must match the precise battle reserved by our RCT
        // launcher, even if another adapter accidentally armed a router ticket.
        if (winnerId != null && (launchLedger == null ||
                !launchLedger.owns(battleId, winnerId))) {
            nativeStoryBattleCancelled(server, battleId);
            return false;
        }
        UUID battleOwner = launchLedger == null ? null :
            launchLedger.owner(battleId).orElse(null);
        String resolvedEncounter = battleRouter.encounterForBattle(battleId).orElse(null);
        boolean result = battleRouter.ended(battleId, winnerId);
        BATTLE_PHASES.finish(battleId);
        // Once the authoritative Cobblemon result arrives, stop any pending
        // battle-intro cues. Never overlay an ended battle with stale prompts.
        if (battleOwner != null) CINEMATICS.cancel(battleOwner);
        if (launchLedger != null) launchLedger.finished(battleId);
        if (result && resolvedEncounter != null && resolvedEncounter.startsWith("shield_")) {
            if (finale != null && winnerId != null) {
                // The victory fact was already persisted by StoryBattleProof.
                // Reconciliation is idempotent and also covers the case where
                // a checkpoint write failed after an earlier native result.
                restoreFinale(winnerId);
                reconcileFinaleShields(winnerId);
                var shieldState = finale.snapshot(winnerId);
                if (shieldState.phase() == ResonanceFinaleController.Phase.STABILIZE) {
                    CINEMATICS.cancel(winnerId);
                    CINEMATICS.begin(winnerId, "finale_shields_complete");
                }
                ServerPlayerEntity victor = server.getPlayerManager().getPlayer(winnerId);
                if (victor != null) {
                    if (victor.getWorld() instanceof ServerWorld fxWorld) {
                        StoryEffects.emit(fxWorld, "finale.shield", ParticleTypes.ELECTRIC_SPARK,
                            victor.getX(), victor.getY() + 1.0D, victor.getZ(),
                            52, 1.0D, 1.2D, 1.0D, 0.08D);
                        StoryEffects.emit(fxWorld, "finale.end_rod", ParticleTypes.END_ROD,
                            victor.getX(), victor.getY() + 1.0D, victor.getZ(),
                            30, 0.9D, 1.0D, 0.9D, 0.035D);
                    }
                    victor.sendMessage(Text.literal("[Pride Region] Resonance shields " +
                        shieldState.shields() + "/3" +
                        (shieldState.phase() == ResonanceFinaleController.Phase.STABILIZE
                            ? " — All shields disabled. Stabilize the engine core!"
                            : " — Defeat the remaining shield defenders."))
                        .formatted(Formatting.AQUA), false);
                }
            }
        }
        if (result && winnerId != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(winnerId);
            if (player != null) {
                String victoryLine = "Story battle victory recorded.";
                try {
                    if (resolvedEncounter != null)
                        victoryLine = StoryBattleRosters.bundled()
                            .find(resolvedEncounter).map(StoryBattleRosters.Roster::win)
                            .orElse(victoryLine);
                } catch (IOException | RuntimeException presentationError) {
                    LOGGER.warn("Story battle victory saved but dialogue unavailable",
                        presentationError);
                }
                // Credit was already durably written. Presentation and
                // advancement UI synchronization may fail independently.
                try {
                    player.sendMessage(Text.literal("[Pride Region] " + victoryLine)
                        .formatted(Formatting.GREEN), false);
                    if (engine != null) StoryAdvancementBridge.synchronize(player, engine);
                } catch (RuntimeException presentationError) {
                    LOGGER.warn("Verified story victory {} persisted but player UI update failed",
                        battleId, presentationError);
                }
            }
        }
        return result;
    }

    /** Only the authoritative Cobblemon BATTLE_FAINTED adapter invokes this. */
    private static void nativeStoryOpponentFainted(MinecraftServer server, UUID battleId,
                                                   UUID pokemonId) {
        if (liveServer != server || engine == null || launchLedger == null ||
                !server.isOnThread()) return;
        UUID owner = launchLedger.owner(battleId).orElse(null);
        if (owner == null) return;
        BATTLE_PHASES.opponentFainted(battleId, pokemonId).ifPresent(beat -> {
            if (!beat.player().equals(owner)) return;
            if (server.getPlayerManager().getPlayer(owner) == null) return;
            // Only scene overlays advance; no HP, status or trainer AI changes.
            CINEMATICS.cancel(owner);
            CINEMATICS.begin(owner, beat.phase() == 1
                ? "battle_phase_one" : "battle_phase_two");
        });
    }

    public static void nativeStoryBattleCancelled(MinecraftServer server, UUID battleId) {
        if (liveServer != server || battleRouter == null) return;
        if (!server.isOnThread()) throw new IllegalStateException("Must run on server thread");
        if (launchLedger != null)
            launchLedger.owner(battleId).ifPresent(CINEMATICS::cancel);
        battleRouter.cancel(battleId);
        BATTLE_PHASES.finish(battleId);
        if (launchLedger != null) launchLedger.finished(battleId);
    }

    /** Show each encounter's prerequisite and verified-win status; never grants anything. */
    /** Player-facing chapter combat checklist; all facts originate server-side. */
    private static int playerEncounterStatus(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) return message(source, "Run this command in-game as a player.");
            if (liveServer != source.getServer() || engine == null)
                return message(source, "The Resonance Crisis campaign is currently unavailable.");
            var facts = PrideCoreMod.progression(source.getServer())
                .orElseThrow(() -> new IOException("Progression unavailable"))
                .get(player.getUuid()).facts();
            source.sendFeedback(() -> Text.literal("Resonance Crisis story battles:"), false);
            restoreFinale(player.getUuid());
            boolean shieldPhase = finale != null && finale.snapshot(player.getUuid()).phase()
                == ResonanceFinaleController.Phase.SHIELDS;
            boolean battleOccupied = launchLedger != null &&
                (launchLedger.pending(player.getUuid()) || launchLedger.active(player.getUuid()));
            int won = 0;
            for (var encounter : StoryEncounterAvailability.ordered()) {
                var state = StoryEncounterAvailability.status(encounter, facts,
                    shieldPhase, battleOccupied);
                if (state == StoryEncounterAvailability.Status.COMPLETE) won++;
                source.sendFeedback(() -> Text.literal(" - " + encounter.id()
                    + ": " + state), false);
            }
            int completed = won;
            source.sendFeedback(() -> Text.literal("Story battle victories: "
                + completed + "/" + StoryBattleProof.definitions().size()), false);
            return 1;
        } catch (IOException | RuntimeException ex) {
            source.sendError(Text.literal("Story battle status unavailable: "
                + safe(ex.getMessage())));
            return 0;
        }
    }

    /** Player-facing, read-only finale objective: never unlocks or advances a phase. */
    private static int finaleStatus(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) return message(source, "Run this command in-game as a player.");
            if (liveServer != source.getServer() || finale == null)
                return message(source, "The Resonance Engine operation is currently unavailable.");
            restoreFinale(player.getUuid());
            reconcileFinaleShields(player.getUuid());
            var state = finale.snapshot(player.getUuid());
            String next = switch (state.phase()) {
                case LOCKED -> "Defeat Director Cael and investigate the engine.";
                case CONDUITS -> "Activate all four conduits around the Resonance Engine.";
                case SHIELDS -> "Defeat Alpha, Beta and Gamma shield defenders.";
                case STABILIZE -> "Interact with the engine core to stabilize it.";
                case COMPLETE -> "The Resonance Engine has been stabilized.";
            };
            return message(source, "Resonance Engine: " + state.phase()
                + " | Conduits " + state.conduits() + "/4"
                + " | Shields " + state.shields() + "/3. " + next);
        } catch (IOException | RuntimeException ex) {
            source.sendError(Text.literal("Finale status unavailable: " + safe(ex.getMessage())));
            return 0;
        }
    }

    /**
     * Operator-only world binding. This edits exactly one explicitly selected
     * entity and never creates players, badges, NPC battles or story facts.
     */
    private static int bindWorldMarker(ServerCommandSource source, String kind,
                                        String id, Entity entity) {
        try {
            String tag = StoryWorldMarkerCatalog.marker(kind, id);
            if (!source.getWorld().getRegistryKey().equals(entity.getWorld().getRegistryKey()))
                throw new IllegalStateException("The selected entity must be in your dimension");
            ServerPlayerEntity actor = source.getPlayer();
            if (actor != null && actor.squaredDistanceTo(entity) > 1024.0D)
                throw new IllegalStateException("Move within 32 blocks of the entity");
            boolean livingNonPlayer = entity instanceof net.minecraft.entity.LivingEntity
                && !(entity instanceof net.minecraft.entity.player.PlayerEntity);
            boolean cobblemonNpc = "cobblemon:npc".equals(
                Registries.ENTITY_TYPE.getId(entity.getType()).toString());
            if (!StoryWorldMarkerCatalog.validOnEntity(tag, livingNonPlayer,
                    cobblemonNpc, entity.getType() == EntityType.INTERACTION))
                throw new IllegalArgumentException(
                    "Marker requires the correct entity type: npc=registered Cobblemon NPC, "
                    + "battle=living non-player, clue/finale=minecraft:interaction");
            if (!StoryWorldMarkerCatalog.canBind(entity.getCommandTags(), tag))
                throw new IllegalStateException(
                    "Remove the conflicting story tag with admin unbind first");
            if (entity.getCommandTags().contains(tag))
                return message(source, "Already bound " + tag + " to " + entity.getUuid());
            if (!entity.addCommandTag(tag))
                throw new IllegalStateException("Entity rejected the story marker");
            return message(source, "Bound " + tag + " to entity " + entity.getUuid()
                + " in " + entity.getWorld().getRegistryKey().getValue());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            source.sendError(Text.literal("World binding refused: " + safe(ex.getMessage())));
            return 0;
        }
    }

    /** Explicit operator cleanup removes only Module 02 world markers. */
    private static int unbindWorldMarker(ServerCommandSource source, Entity entity) {
        if (!source.getWorld().getRegistryKey().equals(entity.getWorld().getRegistryKey())) {
            source.sendError(Text.literal("Selected entity must be in your dimension"));
            return 0;
        }
        ServerPlayerEntity actor = source.getPlayer();
        if (actor != null && actor.squaredDistanceTo(entity) > 1024.0D) {
            source.sendError(Text.literal("Move within 32 blocks of the entity"));
            return 0;
        }
        List<String> previous = entity.getCommandTags().stream()
            .filter(StoryWorldMarkerCatalog::isStoryTag).toList();
        for (String tag : previous) entity.removeCommandTag(tag);
        return message(source, "Cleared " + previous.size() +
            " story world marker(s) on entity " + entity.getUuid());
    }

    /** Local staging audit: detect malformed and conflicting physical props. */
    private static int auditWorldMarkers(ServerCommandSource source) {
        ServerPlayerEntity actor = source.getPlayer();
        if (actor == null) return message(source, "Run marker audit in-game as an operator.");
        var tagged = actor.getWorld().getOtherEntities(actor,
            actor.getBoundingBox().expand(32.0D),
            e -> e.getCommandTags().stream().anyMatch(StoryWorldMarkerCatalog::isStoryTag));
        int healthy = 0, invalid = 0;
        for (Entity entity : tagged) {
            List<String> tags = entity.getCommandTags().stream()
                .filter(StoryWorldMarkerCatalog::isStoryTag).toList();
            boolean livingNonPlayer = entity instanceof net.minecraft.entity.LivingEntity
                && !(entity instanceof net.minecraft.entity.player.PlayerEntity);
            boolean cobblemonNpc = "cobblemon:npc".equals(
                Registries.ENTITY_TYPE.getId(entity.getType()).toString());
            boolean valid = tags.size() == 1
                && StoryWorldMarkerCatalog.validOnEntity(tags.getFirst(),
                    livingNonPlayer, cobblemonNpc,
                    entity.getType() == EntityType.INTERACTION);
            if (valid) healthy++; else invalid++;
            if (!valid)
                source.sendFeedback(() -> Text.literal(
                    "INVALID story marker on " + entity.getUuid() + ": " + tags), false);
        }
        return message(source, "Story marker audit (32 blocks): " +
            healthy + " valid, " + invalid + " invalid, " + tagged.size() + " tagged total");
    }

    /**
     * Non-mutating RCT/Cobblemon compatibility check. Confirms binary
     * signatures and authored rosters, NOT battle behavior in Minecraft.
     */
    private static int adminRuntimePreflight(ServerCommandSource source) {
        try {
            StoryBattleRuntimePreflight.verify(StoryModule.class.getClassLoader());
            var rosters = StoryBattleRosters.bundled();
            int expected = StoryBattleSessionRouter.acceptedTrainers().size();
            if (rosters.all().size() != expected)
                throw new IllegalStateException("Story battle roster count mismatch");
            for (String id : StoryBattleSessionRouter.acceptedTrainers().keySet()) {
                if (rosters.find(id).isEmpty())
                    throw new IllegalStateException("Missing story battle roster: " + id);
            }
            return message(source, "Module 02 class/API preflight passed: " + expected
                + " authored battle rosters; " + StoryEvidenceEngine.definitions().size()
                + " evidence markers; " + StoryWorldMarkerCatalog.finaleIds().size()
                + " finale devices. This does NOT verify an actual RCT battle.");
        } catch (IOException | ReflectiveOperationException | LinkageError | RuntimeException ex) {
            LOGGER.error("Module 02 operator RCT runtime preflight failed", ex);
            source.sendError(Text.literal("Module 02 runtime preflight FAILED: "
                + safe(ex.getMessage()) + " (see server log)"));
            return 0;
        }
    }

    private static int adminEncounters(ServerCommandSource source, ServerPlayerEntity target) {
        try {
            var profile = PrideCoreMod.progression(source.getServer())
                .orElseThrow(() -> new IOException("Progression unavailable"))
                .get(target.getUuid());
            source.sendFeedback(() -> Text.literal(
                "Module 02 scripted encounters for " + target.getName().getString() + ":"), false);
            restoreFinale(target.getUuid());
            boolean shieldPhase = finale != null && finale.snapshot(target.getUuid()).phase()
                == ResonanceFinaleController.Phase.SHIELDS;
            boolean battleOccupied = launchLedger != null &&
                (launchLedger.pending(target.getUuid()) || launchLedger.active(target.getUuid()));
            for (var entry : StoryEncounterAvailability.ordered()) {
                var status = StoryEncounterAvailability.status(entry, profile.facts(),
                    shieldPhase, battleOccupied);
                source.sendFeedback(() -> Text.literal(" - " + entry.id() + ": " + status +
                    " [requires " + entry.requiredFact() + "]"), false);
            }
            return 1;
        } catch (IOException | RuntimeException ex) {
            source.sendError(Text.literal("Encounter inspection failed: " + safe(ex.getMessage())));
            return 0;
        }
    }

    private static StoryPlaythroughReset resetService(MinecraftServer server)
            throws IOException {
        if (liveServer != server || engine == null)
            throw new IllegalStateException("Story module disabled");
        var service = PrideCoreMod.progression(server)
            .orElseThrow(() -> new IOException("Progression unavailable"));
        return new StoryPlaythroughReset(service, StoryJson.load(storyFile()));
    }

    private static int previewPlaythroughReset(ServerCommandSource source,
                                                ServerPlayerEntity target) {
        try {
            var preview = resetService(source.getServer()).preview(target.getUuid());
            return message(source, "Module 02 reset preview for " +
                target.getName().getString() + ": stage=" + preview.currentStage() +
                ", campaign facts to revoke=" + preview.campaignFacts() +
                ", unrelated Pride Core facts preserved=" + preview.preserved().size() +
                ". Confirm with /pridecore story admin reset-playthrough " +
                target.getName().getString() + " confirm. External badges, Pokémon, " +
                "legacy tags and FTB data WILL NOT be reset.");
        } catch (IOException | RuntimeException ex) {
            source.sendError(Text.literal("Reset preview refused: " + safe(ex.getMessage())));
            return 0;
        }
    }

    private static int resetPlaythrough(ServerCommandSource source,
                                        ServerPlayerEntity target) {
        try {
            if (!source.getServer().isOnThread())
                throw new IllegalStateException("Reset must run on server thread");
            var reset = resetService(source.getServer()).reset(target.getUuid());
            if (engine != null) engine.cancel(target.getUuid());
            if (battleRouter != null) battleRouter.disconnect(target.getUuid());
            if (finale != null) finale.disconnect(target.getUuid());
            if (finaleStore != null) finaleStore.delete(target.getUuid());
            LOGGER.warn("Operator {} reset Module 02 campaign for player {} to unstarted",
                source.getName(), target.getUuid());
            return message(source, "Module 02 campaign reset for " +
                target.getName().getString() + "; revision=" +
                reset.profile().revision() + ". Talk to Mom to begin. IMPORTANT: " +
                "League badges, existing starter, external onboarding tags and FTB " +
                "quest data were NOT reset; use a clean staging test profile for a " +
                "true first-time onboarding playthrough.");
        } catch (IOException | RuntimeException ex) {
            source.sendError(Text.literal("Playthrough reset refused: " + safe(ex.getMessage())));
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
            restoreFinale(target.getUuid());
            var state = finale.start(target.getUuid(),
                profile.facts().contains("pride:cael_battle_won"),
                profile.facts().contains("pride:clue_engine"));
            if (state.phase() == ResonanceFinaleController.Phase.LOCKED)
                throw new IllegalStateException("Requires actual Cael victory and engine evidence");
            persistFinale(target.getUuid());
            LOGGER.info("Operator {} started finale rehearsal for {}", source.getName(), target.getUuid());
            return message(source, "Finale phase for " + target.getName().getString() +
                ": " + state.phase() + " (no story facts or rewards granted).");
        } catch (IOException | RuntimeException ex) {
            source.sendError(Text.literal("Finale rehearsal refused: " + safe(ex.getMessage())));
            return 0;
        }
    }

    /**
     * Trusted world-event hook for the final authenticated engine interaction.
     * World event code must verify the physical core marker and reach first.
     * This method cannot skip any required phase or fabricate a boss victory.
     */
    public static boolean completeResonanceFinale(MinecraftServer server, UUID playerId)
            throws IOException {
        if (liveServer != server || engine == null || finale == null) return false;
        if (!server.isOnThread())
            throw new IllegalStateException("Finale must complete on server thread");
        if (server.getPlayerManager().getPlayer(playerId) == null) return false;
        var progression = PrideCoreMod.progression(server)
            .orElseThrow(() -> new IOException("Progression unavailable"));
        boolean changed = new ResonanceFinaleCompletion(progression, finale).complete(playerId);
        if (changed) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            player.sendMessage(Text.literal("[Pride Region] Resonance Engine stabilized! Return to the League Guide.")
                .formatted(Formatting.GREEN), false);
        }
        return changed;
    }

    private static void persistFinale(UUID player) throws IOException {
        if (finaleStore == null || finale == null)
            throw new IOException("Finale checkpoint storage unavailable");
        var checkpoint = finale.checkpoint(player);
        if (checkpoint.isPresent()) finaleStore.save(checkpoint.get());
    }

    private static void restoreFinale(UUID player) throws IOException {
        if (finaleStore == null || finale == null) return;
        if (finale.checkpoint(player).isPresent()) return;
        var saved = finaleStore.load(player);
        if (saved.isEmpty()) return;
        var progression = PrideCoreMod.progression(liveServer)
            .orElseThrow(() -> new IOException("Progression unavailable"));
        var facts = progression.get(player).facts();
        if (!facts.contains("pride:cael_battle_won") ||
                !facts.contains("pride:clue_engine"))
            throw new IOException("Finale resume prerequisites missing");
        try {
            finale.restore(saved.get());
            reconcileFinaleShields(player);
        }
        catch (IllegalArgumentException ex) {
            throw new IOException("Finale checkpoint invalid", ex);
        }
    }

    /**
     * Reconcile authenticated shield wins even if the live controller already
     * exists. This covers a failed checkpoint write without requiring relog.
     * The victory fact is the authority; a world click cannot forge it.
     */
    private static void reconcileFinaleShields(UUID player) throws IOException {
        if (finale == null || finaleStore == null || liveServer == null) return;
        if (finale.snapshot(player).phase() != ResonanceFinaleController.Phase.SHIELDS)
            return;
        var progression = PrideCoreMod.progression(liveServer)
            .orElseThrow(() -> new IOException("Progression unavailable"));
        var facts = progression.get(player).facts();
        if (!facts.contains("pride:cael_battle_won") ||
                !facts.contains("pride:clue_engine")) return;
        var checkpoint = finale.checkpoint(player).orElseThrow();
        for (String shield : List.of("alpha", "beta", "gamma")) {
            if (facts.contains("pride:shield_" + shield + "_won") &&
                    !checkpoint.shields().contains(shield) &&
                    finale.snapshot(player).phase() == ResonanceFinaleController.Phase.SHIELDS)
                finale.shieldVictory(player, shield);
        }
        if (!checkpoint.equals(finale.checkpoint(player).orElseThrow()))
            persistFinale(player);
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
        story.then(CommandManager.literal("finale")
            .executes(ctx -> finaleStatus(ctx.getSource())));
        story.then(CommandManager.literal("encounters")
            .executes(ctx -> playerEncounterStatus(ctx.getSource())));
        // The original RCT League Thorn is now the sole physical encounter.
        // Never expose the retired duplicate TBCS "Story Thorn" battle path.
        // Battle outcomes and one-time rewards remain RCT/League-owned.
        story.then(CommandManager.literal("sync")
            .executes(ctx -> sync(ctx.getSource())));
        story.then(CommandManager.literal("validate")
            .requires(source -> source.hasPermissionLevel(4))
            .executes(ctx -> validate(ctx.getSource())));
        story.then(CommandManager.literal("admin")
            .requires(source -> source.hasPermissionLevel(4))
            .then(CommandManager.literal("runtime-preflight")
                .executes(ctx -> adminRuntimePreflight(ctx.getSource()))));
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
        // Staging world setup: bind only known authored markers to actual
        // entities. No general-purpose entity-tag injection command is exposed.
        story.then(CommandManager.literal("admin")
            .requires(source -> source.hasPermissionLevel(4))
            .then(CommandManager.literal("bind")
                .then(CommandManager.argument("kind", StringArgumentType.word())
                    .then(CommandManager.argument("id", StringArgumentType.word())
                        .then(CommandManager.argument("entity", EntityArgumentType.entity())
                            .executes(ctx -> bindWorldMarker(ctx.getSource(),
                                StringArgumentType.getString(ctx, "kind"),
                                StringArgumentType.getString(ctx, "id"),
                                EntityArgumentType.getEntity(ctx, "entity")))))))
            .then(CommandManager.literal("unbind")
                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                    .executes(ctx -> unbindWorldMarker(ctx.getSource(),
                        EntityArgumentType.getEntity(ctx, "entity")))))
            .then(CommandManager.literal("audit-markers")
                .executes(ctx -> auditWorldMarkers(ctx.getSource()))));
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
        story.then(CommandManager.literal("admin")
            .requires(source -> source.hasPermissionLevel(4))
            .then(CommandManager.literal("reset-preview")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> previewPlaythroughReset(ctx.getSource(),
                        EntityArgumentType.getPlayer(ctx, "player")))))
            .then(CommandManager.literal("reset-playthrough")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.literal("confirm")
                        .executes(ctx -> resetPlaythrough(ctx.getSource(),
                            EntityArgumentType.getPlayer(ctx, "player")))))));
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
