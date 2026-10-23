package com.prideregion.flashpoints.encounter;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable, staging-only weapon combat health independent of Cobblemon HP.
 * No vanilla/Cobblemon damage, awards, or capture manipulation here.
 */
public final class ActionBossState {
    public enum Phase { ACTIVE, WEAKENED, CAPTURED, FINISHED }

    public record Snapshot(EncounterLedger.Key dungeon, UUID pokemon, UUID operator,
                           int maxHp, int hp, int hits, Phase phase,
                           UUID capturedBy, long changedAt) {
        public Snapshot {
            Objects.requireNonNull(dungeon);
            Objects.requireNonNull(pokemon);
            Objects.requireNonNull(operator);
            Objects.requireNonNull(phase);
            if(maxHp<20 || maxHp>5000 || hp<0 || hp>maxHp || hits<0 ||
                hits>10000 || changedAt<0)
                throw new IllegalArgumentException("Invalid combat meter values");
            if(phase==Phase.ACTIVE && hp<=threshold(maxHp)
                || phase!=Phase.ACTIVE && hp>threshold(maxHp)
                || phase==Phase.CAPTURED && capturedBy==null
                || phase!=Phase.CAPTURED && capturedBy!=null)
                throw new IllegalArgumentException("Invalid action combat phase");
        }

        /** Clamp at 20%; the actual Pokemon is never killed by these hits. */
        public Snapshot hit(int damage,long time) {
            if(phase!=Phase.ACTIVE)throw new IllegalStateException("Boss not in ACTIVE phase");
            if(damage<1 || damage>50 || time<0)throw new IllegalArgumentException("Invalid attack");
            int next=Math.max(threshold(maxHp),hp-damage);
            return new Snapshot(dungeon,pokemon,operator,maxHp,next,hits+1,
                next==threshold(maxHp)?Phase.WEAKENED:Phase.ACTIVE,null,time);
        }

        public Snapshot capture(UUID pokemonId,UUID playerId,long time) {
            if(phase!=Phase.WEAKENED || !pokemon.equals(pokemonId) ||
                !operator.equals(playerId) || time<=0)
                throw new IllegalStateException("Action boss is not capture-ready for this player/Pokemon");
            return new Snapshot(dungeon,pokemon,operator,maxHp,hp,hits,
                Phase.CAPTURED,playerId,time);
        }

        public Snapshot finish(UUID playerId,long time) {
            if(phase!=Phase.WEAKENED || !operator.equals(playerId) || time<=0)
                throw new IllegalStateException("Only the registered operator can finish WEAKENED boss");
            return new Snapshot(dungeon,pokemon,operator,maxHp,hp,hits,
                Phase.FINISHED,null,time);
        }
    }

    private ActionBossState(){}
    public static int threshold(int maxHp) {return Math.max(1,(int)Math.ceil(maxHp*0.2));}
    public static Snapshot arm(EncounterLedger.Key key,UUID pokemon,UUID player,long time) {
        return new Snapshot(key,pokemon,player,300,300,0,Phase.ACTIVE,null,time);
    }
}
