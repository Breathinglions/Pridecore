package com.prideregion.core.story;

import java.lang.reflect.*;
import java.util.*;

/** Binary compatibility audit performed when Module 02 is enabled. */
public final class StoryBattleRuntimePreflight {
    private StoryBattleRuntimePreflight() {}
    public static void verify(ClassLoader loader) throws ReflectiveOperationException {
        Objects.requireNonNull(loader);
        Class<?> pokemon = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon", false, loader);
        Class<?> properties = Class.forName(
            "com.cobblemon.mod.common.api.pokemon.PokemonProperties", false, loader);
        Class<?> companion = Class.forName(
            "com.cobblemon.mod.common.api.pokemon.PokemonProperties$Companion", false, loader);
        companion.getMethod("parse", String.class);
        properties.getMethod("create");
        Class<?> api = Class.forName("com.gitlab.srcmc.rctapi.api.RCTApi", false, loader);
        api.getMethod("getInstance", String.class);
        api.getMethod("initInstance", String.class);
        api.getMethod("getBattleManager");
        Class<?> manager = Class.forName("com.gitlab.srcmc.rctapi.api.battle.BattleManager", false, loader);
        Class<?> format = Class.forName("com.gitlab.srcmc.rctapi.api.battle.BattleFormat", false, loader);
        Class<?> rules = Class.forName("com.gitlab.srcmc.rctapi.api.battle.BattleRules", false, loader);
        format.getField("GEN_9_SINGLES");
        manager.getMethod("startBattle", List.class, List.class, format, rules);
        manager.getMethod("getState", UUID.class);
        manager.getMethod("end", UUID.class);
        manager.getMethod("getDefaultRules");
        Class<?> state = Class.forName("com.gitlab.srcmc.rctapi.api.battle.BattleState", false, loader);
        state.getMethod("getBattle");
        state.getMethod("getParticipants1");
        state.getMethod("getParticipants2");
        Class<?> events = Class.forName(
            "com.cobblemon.mod.common.api.events.CobblemonEvents", false, loader);
        events.getField("BATTLE_VICTORY");
        // Verify the exact callback and resolved result shape, not just the
        // presence of an event constant.
        Class<?> victory = Class.forName(
            "com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent", false, loader);
        Class<?> battle = Class.forName(
            "com.cobblemon.mod.common.api.battles.model.PokemonBattle", false, loader);
        Class<?> actor = Class.forName(
            "com.cobblemon.mod.common.api.battles.model.actor.BattleActor", false, loader);
        victory.getMethod("getBattle");
        victory.getMethod("getWinners");
        victory.getMethod("getLosers");
        victory.getMethod("getWasWildCapture");
        battle.getMethod("getBattleId");
        battle.getMethod("getPlayerUUIDs");
        actor.getMethod("getUuid");
        Class<?> subscription = Class.forName(
            "com.cobblemon.mod.common.api.reactive.ObservableSubscription", false, loader);
        Class<?> observable = Class.forName(
            "com.cobblemon.mod.common.api.reactive.SimpleObservable", false, loader);
        observable.getMethod("subscribe", java.util.function.Consumer.class);
        observable.getMethod("unsubscribe", subscription);
        Class<?> npc = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC", false, loader);
        Class<?> trainerPlayer = Class.forName(
            "com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer", false, loader);
        if (Arrays.stream(trainerPlayer.getConstructors()).noneMatch(ctor ->
                ctor.getParameterCount() == 1))
            throw new NoSuchMethodException("RCT TrainerPlayer(server player) missing");
        Class<?> battleAi = Class.forName(
            "com.cobblemon.mod.common.api.battles.model.ai.BattleAI", false, loader);
        Class<?> bag = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerBag", false, loader);
        bag.getConstructor();
        Class<?> ai = Class.forName("com.gitlab.srcmc.rctapi.api.ai.RCTBattleAI", false, loader);
        ai.getConstructor();
        if (!battleAi.isAssignableFrom(ai))
            throw new NoSuchMethodException("RCTBattleAI must implement Cobblemon BattleAI");
        if (Arrays.stream(npc.getConstructors()).noneMatch(ctor ->
                ctor.getParameterCount() == 5
                && ctor.getParameterTypes()[0] == String.class
                && ctor.getParameterTypes()[1] == Array.newInstance(pokemon, 0).getClass()
                && ctor.getParameterTypes()[2] == bag
                && ctor.getParameterTypes()[3] == battleAi))
            throw new NoSuchMethodException("RCT TrainerNPC(String, Pokemon[], TrainerBag, BattleAI, LivingEntity)");
    }
    /** Optional visual-phase API checks; must not fail core battle startup. */
    public static boolean supportsFaintEvents(ClassLoader loader) {
        try {
            Class<?> events = Class.forName(
                "com.cobblemon.mod.common.api.events.CobblemonEvents", false, loader);
            events.getField("BATTLE_FAINTED");
            Class<?> actor = Class.forName(
                "com.cobblemon.mod.common.api.battles.model.actor.BattleActor", false, loader);
        actor.getMethod("getPlayerUUIDs");
        Class<?> faint = Class.forName(
            "com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent", false, loader);
        Class<?> battlePokemon = Class.forName(
            "com.cobblemon.mod.common.battles.pokemon.BattlePokemon", false, loader);
        faint.getMethod("getBattle");
        faint.getMethod("getKilled");
        battlePokemon.getMethod("getActor");
        battlePokemon.getMethod("getUuid");
        // Verified against Cobblemon 1.8.1: the player actor UUID is the
        // player UUID, not an arbitrary opponent or temporary battle ID.
        Class<?> playerActor = Class.forName(
            "com.cobblemon.mod.common.battles.actor.PlayerBattleActor", false, loader);
        if (!actor.isAssignableFrom(playerActor))
            throw new NoSuchMethodException("PlayerBattleActor must extend BattleActor");
        playerActor.getMethod("getPlayerUUIDs");
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError missing) {
            return false;
        }
    }

}
