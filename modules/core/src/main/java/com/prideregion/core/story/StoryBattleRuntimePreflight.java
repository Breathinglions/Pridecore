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
        Class<?> bag = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerBag", false, loader);
        bag.getConstructor();
        Class<?> ai = Class.forName("com.gitlab.srcmc.rctapi.api.ai.RCTBattleAI", false, loader);
        ai.getConstructor();
        if (Arrays.stream(npc.getConstructors()).noneMatch(ctor ->
                ctor.getParameterCount() == 5
                && ctor.getParameterTypes()[0] == String.class
                && ctor.getParameterTypes()[1] == Array.newInstance(pokemon, 0).getClass()
                && ctor.getParameterTypes()[2] == bag))
            throw new NoSuchMethodException("RCT TrainerNPC(String, Pokemon[], TrainerBag, BattleAI, LivingEntity)");
    }
}
