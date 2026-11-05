package com.prideregion.core.story;

import java.lang.reflect.*;
import java.io.IOException;
import java.util.*;

/**
 * Version-specific RCT API 0.16.0 / Cobblemon 1.8.1 launch boundary.
 * All arguments are trusted server objects; no command or client can call it.
 * Runs on the Minecraft server thread. Invoker owns the session reservation.
 */
public final class RctStoryBattleLauncher {
    private RctStoryBattleLauncher() {}

    // Only temporary Route 1 RCT NPC registrations are retained here.
    // The registry lookup is how RCTBattleAI resolves per-Pokemon gimmick rules.
    private static final Map<UUID, String> RIN_BATTLE_REGISTRATIONS = new HashMap<>();

    private static void unregisterRin(String registrationId, ClassLoader loader)
            throws ReflectiveOperationException {
        Class<?> apiType = Class.forName("com.gitlab.srcmc.rctapi.api.RCTApi", true, loader);
        Object api = apiType.getMethod("getInstance", String.class).invoke(null, "pridecore");
        if (api == null) throw new IllegalStateException("PrideCore RCT API is unavailable");
        Object registry = apiType.getMethod("getTrainerRegistry").invoke(api);
        registry.getClass().getMethod("unregisterById", String.class)
            .invoke(registry, registrationId);
    }

    /** Retire this single story NPC once its native battle has resolved. */
    public static void release(UUID battleId, ClassLoader loader)
            throws ReflectiveOperationException {
        String registrationId = RIN_BATTLE_REGISTRATIONS.get(battleId);
        if (registrationId == null) return;
        unregisterRin(registrationId, loader);
        RIN_BATTLE_REGISTRATIONS.remove(battleId);
    }

    /**
     * Abort an RCT battle whose returned UUID failed Pride Core ownership
     * confirmation. Never leave an untracked trainer battle running.
     */
    public static boolean abort(UUID battleId, ClassLoader loader)
            throws ReflectiveOperationException {
        Objects.requireNonNull(battleId);
        Class<?> apiType = Class.forName("com.gitlab.srcmc.rctapi.api.RCTApi", true, loader);
        Object api = apiType.getMethod("getInstance", String.class).invoke(null, "pridecore");
        if (api == null) return false;
        Object manager = apiType.getMethod("getBattleManager").invoke(api);
        try {
            return Boolean.TRUE.equals(manager.getClass().getMethod("end", UUID.class)
                .invoke(manager, battleId));
        } finally {
            release(battleId, loader);
        }
    }

    /**
     * RCT 0.16 TrainerNPC has an explicit per-Pokemon GimmicksMap. When
     * we introduce the player to basic battles on Route 1, disable NPC-side
     * Dynamax, Gmax and Tera, even if a future RCT default changes.
     *
     * Fail CLOSED on an API mismatch. A battle must never silently start with
     * unknown transformation permissions. This does not affect unrelated RCT
     * NPCs, player permissions, or third-party mods modifying battles later.
     */
    private static void requireNoGimmicksForRoute1(
            Object opponent, Object[] team, Class<?> pokemonType, ClassLoader loader)
            throws ReflectiveOperationException {
        Class<?> gimmickType = Class.forName(
            "com.gitlab.srcmc.rctapi.api.models.Gimmicks", true, loader);
        Object none = gimmickType.getConstructor(String.class, boolean.class, boolean.class)
            .newInstance(null, false, false);
        Object map = opponent.getClass().getMethod("getGimmicks").invoke(opponent);
        Method set = map.getClass().getMethod("to", pokemonType, gimmickType);
        Method get = map.getClass().getMethod("of", pokemonType);
        for (Object pokemon : team) {
            set.invoke(map, pokemon, none);
            Object actual = get.invoke(map, pokemon);
            if (!Objects.equals(actual, none))
                throw new IllegalStateException(
                    "RCT did not accept Rin Route 1's no-gimmicks policy");
            Object tera = gimmickType.getMethod("tera").invoke(actual);
            Object dynamax = gimmickType.getMethod("dynamax").invoke(actual);
            Object gmax = gimmickType.getMethod("gmax").invoke(actual);
            if (tera != null || !Boolean.FALSE.equals(dynamax)
                    || !Boolean.FALSE.equals(gmax))
                throw new IllegalStateException(
                    "Rin Route 1 gimmicks remain enabled; battle refused");
        }
        System.out.println("[PrideCore] Rin Route 1 RCT gimmick policy verified: "
            + "tera=off, dynamax=off, gmax=off");
    }

    public static UUID launch(String encounterId, Object serverPlayer,
                              Object livingTrainer, UUID expectedPlayerId, ClassLoader loader)
            throws ReflectiveOperationException, IOException {
        Objects.requireNonNull(serverPlayer);
        Objects.requireNonNull(expectedPlayerId);
        Objects.requireNonNull(livingTrainer);
        Objects.requireNonNull(loader);
        var roster = StoryBattleRosters.active().find(encounterId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown story roster"));
        // Minecraft class names are remapped in production; find constructor
        // parameter types from the installed RCT bytecode instead of guessing.

        Object[] team = CobblemonRosterFactory.create(roster, loader);
        Class<?> apiType = Class.forName("com.gitlab.srcmc.rctapi.api.RCTApi", true, loader);
        Object api = apiType.getMethod("getInstance", String.class).invoke(null, "pridecore");
        if (api == null)
            api = apiType.getMethod("initInstance", String.class).invoke(null, "pridecore");
        if (api == null)
            throw new IllegalStateException("RCT API instance initialization failed");
        Object manager = apiType.getMethod("getBattleManager").invoke(api);
        Class<?> trainerType = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.Trainer", true, loader);
        Class<?> trainerPlayer = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer", true, loader);
        Class<?> trainerNpc = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC", true, loader);
        Class<?> bagType = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerBag", true, loader);
        Class<?> pokemonType = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon", true, loader);
        Class<?> battleAiType = Class.forName("com.cobblemon.mod.common.api.battles.model.ai.BattleAI", true, loader);
        Constructor<?> playerCtor = Arrays.stream(trainerPlayer.getConstructors())
            .filter(ctor -> ctor.getParameterCount() == 1
                && ctor.getParameterTypes()[0].isInstance(serverPlayer))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Not a server player"));
        Object player = playerCtor.newInstance(serverPlayer);
        Object bag = bagType.getConstructor().newInstance();
        Class<?> teamType = Array.newInstance(pokemonType, 0).getClass();
        Constructor<?> npcCtor = Arrays.stream(trainerNpc.getConstructors())
            .filter(ctor -> Arrays.equals(Arrays.copyOf(ctor.getParameterTypes(), 4),
                new Class<?>[] {String.class, teamType, bagType, battleAiType})
                && ctor.getParameterCount() == 5
                && ctor.getParameterTypes()[4].isInstance(livingTrainer))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Not a living trainer"));
        // RCT 0.16.0 exposes a public no-arg RCTBattleAI constructor.
        // A null AI would leave battle-turn selection undefined.
        Class<?> rctAiType = Class.forName(
            "com.gitlab.srcmc.rctapi.api.ai.RCTBattleAI", true, loader);
        Object ai = rctAiType.getConstructor().newInstance();
        if (!battleAiType.isInstance(ai))
            throw new IllegalStateException("RCT trainer AI incompatible with Cobblemon");
        Object opponent = npcCtor.newInstance(
            "pride_story:" + encounterId, team, bag, ai, livingTrainer);
        // Route 1 is deliberately a basic first-partner battle. Make its
        // no-gimmicks policy explicit in RCT rather than relying on defaults
        // (and never change the user's team or later Gym battles).
        String temporaryRegistryId = null;
        boolean verified = false;
        try {
            if ("rin_route1".equals(encounterId)) {
                requireNoGimmicksForRoute1(opponent, team, pokemonType, loader);
                // RCTBattleAI resolves the owner via TrainerRegistry.getByOT().
                // A directly constructed TrainerNPC has no registered original
                // trainer, so its verified GimmicksMap is otherwise ignored.
                Object registry = apiType.getMethod("getTrainerRegistry").invoke(api);
                temporaryRegistryId = "rin_route1_" +
                    UUID.randomUUID().toString().replace("-", "");
                registry.getClass().getMethod("registerNPC", String.class, trainerNpc)
                    .invoke(registry, temporaryRegistryId, opponent);
                for (Object pokemon : team) {
                    Object resolved = registry.getClass().getMethod("getByOT", pokemonType)
                        .invoke(registry, pokemon);
                    if (resolved != opponent)
                        throw new IllegalStateException(
                            "Rin Route 1 RCT trainer ownership not registered; battle refused");
                }
                System.out.println("[PrideCore] Rin Route 1 RCT NPC registered and "
                    + "original-trainer lookup verified: " + temporaryRegistryId);
            }
            Class<?> format = Class.forName("com.gitlab.srcmc.rctapi.api.battle.BattleFormat", true, loader);
        Class<?> rules = Class.forName("com.gitlab.srcmc.rctapi.api.battle.BattleRules", true, loader);
        // Verified from the supplied RCT API 0.16.0 JAR:
        // BattleFormat is an enum implementing BattleFormatProvider.
        Object singles = format.getField("GEN_9_SINGLES").get(null);
        Object defaultRules = manager.getClass().getMethod("getDefaultRules").invoke(manager);
        if (defaultRules == null) defaultRules = rules.getConstructor().newInstance();
        // Use the exact 0.16.0 API method signature. Reflection against the
        // concrete BattleFormat enum avoids overload resolution ambiguity.
        Object battle = manager.getClass().getMethod("startBattle",
            List.class, List.class, format, rules).invoke(manager,
                List.of(player), List.of(opponent), singles, defaultRules);
        if (!(battle instanceof UUID id))
            throw new IllegalStateException("RCT rejected story trainer battle launch");
        // RCT owns the encounter state. Validate before giving the battle
        // to the story router, and abort *all* validation failures, not
        // only mismatched UUIDs.
        try {
            Object state = manager.getClass().getMethod("getState", UUID.class)
                .invoke(manager, id);
            if (state == null)
                throw new IllegalStateException("RCT did not register the launched battle");
            Object participants1 = state.getClass()
                .getMethod("getParticipants1").invoke(state);
            Object participants2 = state.getClass()
                .getMethod("getParticipants2").invoke(state);
            if (!(participants1 instanceof List<?> side1)
                    || !(participants2 instanceof List<?> side2)
                    || !StoryPokemonOnlyParties.valid(side1, side2, player, opponent))
                throw new IllegalStateException(
                    "RCT story encounter must be a Pokémon-only 1v1 trainer match");
            Object cobbleBattle = state.getClass().getMethod("getBattle").invoke(state);
            UUID cobbleId = (UUID) cobbleBattle.getClass()
                .getMethod("getBattleId").invoke(cobbleBattle);
            Object nativePlayers = cobbleBattle.getClass()
                .getMethod("getPlayerUUIDs").invoke(cobbleBattle);
            Set<UUID> actualPlayers = new HashSet<>();
            for (Object nativeId : (Iterable<?>) nativePlayers) {
                if (!(nativeId instanceof UUID playerId))
                    throw new IllegalStateException("Invalid Cobblemon battle player UUID");
                actualPlayers.add(playerId);
            }
            if (!actualPlayers.equals(Set.of(expectedPlayerId)))
                throw new IllegalStateException("Cobblemon battle contains unexpected players");
            if (!id.equals(cobbleId))
                throw new IllegalStateException("RCT/Cobblemon battle ID mismatch");
            verified = true;
            if (temporaryRegistryId != null)
                RIN_BATTLE_REGISTRATIONS.put(id, temporaryRegistryId);
            return id;
        } finally {
            if (!verified) {
                try {
                    manager.getClass().getMethod("end", UUID.class).invoke(manager, id);
                } catch (ReflectiveOperationException | RuntimeException abortFailure) {
                    System.err.println("[PrideCore] Failed to abort unverified RCT battle " +
                        id + ": " + abortFailure.getMessage());
                }
            }
        }
        } finally {
            // If launch or ownership verification fails, never leave a
            // temporary NPC in RCT's registry after aborting the battle.
            if (!verified && temporaryRegistryId != null) {
                try {
                    unregisterRin(temporaryRegistryId, loader);
                } catch (ReflectiveOperationException | RuntimeException cleanupError) {
                    System.err.println("[PrideCore] Failed to release unverified Rin "
                        + "RCT trainer: " + cleanupError);
                }
            }
        }
    }
}
