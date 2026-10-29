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

    public static UUID launch(String encounterId, Object serverPlayer,
                              Object livingTrainer, ClassLoader loader)
            throws ReflectiveOperationException, IOException {
        Objects.requireNonNull(serverPlayer);
        Objects.requireNonNull(livingTrainer);
        Objects.requireNonNull(loader);
        var roster = StoryBattleRosters.bundled().find(encounterId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown story roster"));
        Class<?> playerType = Class.forName("net.minecraft.server.network.ServerPlayerEntity", true, loader);
        Class<?> livingType = Class.forName("net.minecraft.entity.LivingEntity", true, loader);
        if (!playerType.isInstance(serverPlayer) || !livingType.isInstance(livingTrainer))
            throw new IllegalArgumentException("Invalid native player or trainer");
        Object[] team = CobblemonRosterFactory.create(roster, loader);
        Class<?> apiType = Class.forName("com.gitlab.srcmc.rctapi.api.RCTApi", true, loader);
        Object api = apiType.getMethod("getInstance", String.class).invoke(null, "pridecore");
        if (api == null)
            throw new IllegalStateException("Pride Core RCT API instance not initialized");
        Object manager = apiType.getMethod("getBattleManager").invoke(api);
        Class<?> trainerType = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.Trainer", true, loader);
        Class<?> trainerPlayer = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer", true, loader);
        Class<?> trainerNpc = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC", true, loader);
        Class<?> bagType = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerBag", true, loader);
        Class<?> pokemonType = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon", true, loader);
        Class<?> battleAiType = Class.forName("com.cobblemon.mod.common.api.battles.model.ai.BattleAI", true, loader);
        Object player = trainerPlayer.getConstructor(playerType).newInstance(serverPlayer);
        Object bag = bagType.getConstructor().newInstance();
        Object opponent = trainerNpc.getConstructor(String.class,
            Array.newInstance(pokemonType, 0).getClass(), bagType, battleAiType, livingType)
            .newInstance("pride_story:" + encounterId, team, bag, null, livingTrainer);
        Class<?> format = Class.forName("com.gitlab.srcmc.rctapi.api.battle.BattleFormat", true, loader);
        Class<?> rules = Class.forName("com.gitlab.srcmc.rctapi.api.battle.BattleRules", true, loader);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object singles = Enum.valueOf((Class<? extends Enum>)format.asSubclass(Enum.class), "GEN_9_SINGLES");
        Object defaultRules = manager.getClass().getMethod("getDefaultRules").invoke(manager);
        if (defaultRules == null) defaultRules = rules.getConstructor().newInstance();
        Object battle = manager.getClass().getMethod("startBattle",
            List.class, List.class, format, rules).invoke(manager,
                List.of(player), List.of(opponent), singles, defaultRules);
        if (!(battle instanceof UUID id))
            throw new IllegalStateException("RCT rejected story trainer battle launch");
        return id;
    }
}
