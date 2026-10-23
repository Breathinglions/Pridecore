package com.prideregion.flashpoints.encounter;

import java.util.*;

/**
 * Pure, fail-closed classification of one Cobblemon BATTLE_VICTORY event.
 * Requires a single PLAYER actor opposite a single WILD actor.
 * Capture-end events and unrelated actors do not count.
 */
public final class BattleOutcomeRules {
    public enum Result { DEFEATED, PLAYER_LOST }
    public record Actor(boolean wild, boolean player, Set<UUID> pokemon, Set<UUID> playerIds) {
        public Actor {
            pokemon=Set.copyOf(pokemon);
            playerIds=Set.copyOf(playerIds);
        }
    }
    private BattleOutcomeRules(){}

    public static Optional<Result> classify(UUID boss,UUID player,List<Actor> winners,
                                             List<Actor> losers,boolean wildCapture) {
        if(wildCapture || winners.size()!=1 || losers.size()!=1)return Optional.empty();
        Actor w=winners.get(0),l=losers.get(0);
        if(validPlayer(w,player) && validWild(l,boss))
            return Optional.of(Result.DEFEATED);
        if(validWild(w,boss) && validPlayer(l,player))
            return Optional.of(Result.PLAYER_LOST);
        return Optional.empty();
    }
    private static boolean validWild(Actor a,UUID uuid) {
        return a.wild() && !a.player() && a.playerIds().isEmpty() &&
            a.pokemon().size()==1 && a.pokemon().contains(uuid);
    }
    private static boolean validPlayer(Actor a,UUID player) {
        return a.player() && !a.wild() && a.playerIds().size()==1 &&
            a.playerIds().contains(player);
    }
}
