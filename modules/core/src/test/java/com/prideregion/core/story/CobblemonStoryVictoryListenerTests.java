package com.prideregion.core.story;

import java.util.*;

public final class CobblemonStoryVictoryListenerTests {
    public static void main(String[] args) {
        UUID a=UUID.randomUUID(), b=UUID.randomUUID();
        if (!CobblemonStoryVictoryListener.soloWinner(Set.of(a),Set.of(a)).equals(Optional.of(a)))
            throw new AssertionError("Solo win refused");
        for (var invalid : List.of(
            CobblemonStoryVictoryListener.soloWinner(Set.of(a,b),Set.of(a)),
            CobblemonStoryVictoryListener.soloWinner(Set.of(a),Set.of(b)),
            CobblemonStoryVictoryListener.soloWinner(Set.of(a),Set.of()),
            CobblemonStoryVictoryListener.soloWinner(Set.of(),Set.of(a)),
            CobblemonStoryVictoryListener.soloWinner(Set.of(a,b),Set.of(a,b)))) {
            if (invalid.isPresent()) throw new AssertionError("Unauthorized story victory");
        }
        // A verified trainer result must identify exactly one player winner
        // and one *different* NPC loser, not merely nonempty loser metadata.
        if (!CobblemonStoryVictoryListener.soloNpcVictory(
                Set.of(a), Set.of(a), Set.of(b)).equals(Optional.of(a)))
            throw new AssertionError("Legitimate player/NPC result rejected");
        for (var invalid : List.of(
            CobblemonStoryVictoryListener.soloNpcVictory(Set.of(a), Set.of(a), Set.of(a)),
            CobblemonStoryVictoryListener.soloNpcVictory(Set.of(a), Set.of(b), Set.of(a)),
            CobblemonStoryVictoryListener.soloNpcVictory(Set.of(a,b), Set.of(a), Set.of(b)),
            CobblemonStoryVictoryListener.soloNpcVictory(Set.of(a), Set.of(a,b), Set.of(b)),
            CobblemonStoryVictoryListener.soloNpcVictory(Set.of(a), Set.of(a), Set.of()),
            CobblemonStoryVictoryListener.soloNpcVictory(Set.of(a), Set.of(), Set.of(b)),
            CobblemonStoryVictoryListener.soloNpcVictory(Set.of(a), Set.of(a), Set.of(a,b))
        )) if (invalid.isPresent())
            throw new AssertionError("Ambiguous Cobblemon victory was accepted");
        System.out.println("PASS: solo Cobblemon story victory eligibility");
    }
}
