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
        return Boolean.TRUE.equals(manager.getClass().getMethod("end", UUID.class)
            .invoke(manager, battleId));
    }

    public static UUID launch(String encounterId, Object serverPlayer,
                              Object livingTrainer, ClassLoader loader)
            throws ReflectiveOperationException, IOException {
        Objects.requireNonNull(serverPlayer);
        Objects.requireNonNull(livingTrainer);
        Objects.requireNonNull(loader);
        var roster = StoryBattleRosters.bundled().find(encounterId)
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
        boolean verified = false;
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
                    || !side1.contains(player) || !side2.contains(opponent)
                    || side1.contains(opponent) || side2.contains(player))
                throw new IllegalStateException("RCT registered the wrong story battle participants");
            Object cobbleBattle = state.getClass().getMethod("getBattle").invoke(state);
            UUID cobbleId = (UUID) cobbleBattle.getClass()
                .getMethod("getBattleId").invoke(cobbleBattle);
            UUID expectedPlayer = (UUID) serverPlayer.getClass()
                .getMethod("getUuid").invoke(serverPlayer);
            Object nativePlayers = cobbleBattle.getClass()
                .getMethod("getPlayerUUIDs").invoke(cobbleBattle);
            Set<UUID> actualPlayers = new HashSet<>();
            for (Object nativeId : (Iterable<?>) nativePlayers) {
                if (!(nativeId instanceof UUID playerId))
                    throw new IllegalStateException("Invalid Cobblemon battle player UUID");
                actualPlayers.add(playerId);
            }
            if (!actualPlayers.equals(Set.of(expectedPlayer)))
                throw new IllegalStateException("Cobblemon battle contains unexpected players");
            if (!id.equals(cobbleId))
                throw new IllegalStateException("RCT/Cobblemon battle ID mismatch");
            verified = true;
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
    }
}
