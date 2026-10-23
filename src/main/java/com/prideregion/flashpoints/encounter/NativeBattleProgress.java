package com.prideregion.flashpoints.encounter;

import java.util.*;

/** Immutable native event progression for one registered wild boss and player. */
public final class NativeBattleProgress {
    public enum Stage { ARMED, DEFEATED, PLAYER_LOST }
    public record Snapshot(EncounterLedger.Key dungeon,UUID bossPokemon,UUID player,
                           Stage stage,UUID battleId,long resolvedAt) {
        public Snapshot {
            Objects.requireNonNull(dungeon);Objects.requireNonNull(bossPokemon);
            Objects.requireNonNull(player);Objects.requireNonNull(stage);
            if(stage==Stage.ARMED && (battleId!=null || resolvedAt!=0) ||
               stage!=Stage.ARMED && (battleId==null || resolvedAt<=0))
                throw new IllegalArgumentException("Invalid battle journal state");
        }
        public Snapshot resolve(BattleOutcomeRules.Result result,UUID battle,long timestamp) {
            if(stage!=Stage.ARMED || battle==null || timestamp<=0)
                throw new IllegalStateException("Native boss battle was already resolved");
            return new Snapshot(dungeon,bossPokemon,player,
                result==BattleOutcomeRules.Result.DEFEATED?Stage.DEFEATED:Stage.PLAYER_LOST,
                battle,timestamp);
        }
    }
    private NativeBattleProgress(){}
}
