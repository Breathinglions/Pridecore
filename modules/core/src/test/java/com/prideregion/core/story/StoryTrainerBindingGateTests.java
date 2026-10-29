package com.prideregion.core.story;

import java.util.*;

public final class StoryTrainerBindingGateTests {
    private static int count;
    private static void check(boolean condition) {
        count++;
        if (!condition) throw new AssertionError("Physical RCT launch gate " + count);
    }
    public static void main(String[] args) {
        for (String encounter : StoryBattleSessionRouter.acceptedTrainers().keySet()) {
            String tag = "pride_battle_" + encounter;
            check(StoryTrainerBindingGate.permits(
                encounter, Set.of(tag, "third_party_tag"), true, false, 36.0D));
            check(!StoryTrainerBindingGate.permits(
                encounter, Set.of(tag), true, false, 36.001D));
            check(!StoryTrainerBindingGate.permits(
                encounter, Set.of(tag), false, false, 0D));
            check(!StoryTrainerBindingGate.permits(
                encounter, Set.of(tag), true, true, 0D));
            check(!StoryTrainerBindingGate.permits(
                encounter, Set.of(tag, "pride_clue_forest"), true, false, 1D));
            check(!StoryTrainerBindingGate.permits(
                encounter, Set.of("pride_battle_forged"), true, false, 1D));
            check(!StoryTrainerBindingGate.permits(
                encounter, Set.of(tag), true, false, Double.NaN));
            check(!StoryTrainerBindingGate.permits(
                encounter, Set.of(tag), true, false, -1D));
        }
        check(!StoryTrainerBindingGate.permits(
            "invalid", Set.of("pride_battle_invalid"), true, false, 1D));
        check(!StoryTrainerBindingGate.permits(
            "sable", null, true, false, 1D));
        System.out.println("PASS: " + count + " physical RCT trainer launch checks");
    }
}
