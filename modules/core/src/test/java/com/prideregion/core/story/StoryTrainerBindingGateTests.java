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
        // Three late-game bosses each use one real Cobblemon NPC tagged
        // with the canonical narrative role, never a second battle marker.
        for (var pair : Map.of(
                "eclipse_lieutenant", "pride_npc_eclipse_operative",
                "sable", "pride_npc_sable",
                "director_cael", "pride_npc_director_cael").entrySet()) {
            String encounter = pair.getKey(), npc = pair.getValue();
            check(StoryTrainerBindingGate.permitsCanonicalBoss(
                encounter, Set.of(npc, "other_mod_tag"), true, false, 36D));
            check(!StoryTrainerBindingGate.permitsCanonicalBoss(
                encounter, Set.of(npc), true, false, 36.001D));
            check(!StoryTrainerBindingGate.permitsCanonicalBoss(
                encounter, Set.of(npc), false, false, 0D));
            check(!StoryTrainerBindingGate.permitsCanonicalBoss(
                encounter, Set.of(npc), true, true, 0D));
            check(!StoryTrainerBindingGate.permitsCanonicalBoss(
                encounter, Set.of(npc, "pride_battle_" + encounter), true, false, 1D));
            check(!StoryTrainerBindingGate.permitsCanonicalBoss(
                encounter, Set.of(npc, "pride_npc_rin"), true, false, 1D));
            for (String other : Map.of(
                    "eclipse_lieutenant", "pride_npc_eclipse_operative",
                    "sable", "pride_npc_sable",
                    "director_cael", "pride_npc_director_cael").keySet())
                if (!other.equals(encounter))
                    check(!StoryTrainerBindingGate.permitsCanonicalBoss(
                        other, Set.of(npc), true, false, 1D));
        }
        check(!StoryTrainerBindingGate.permitsCanonicalBoss(
            "rin_route1", Set.of("pride_npc_rin"), true, false, 0D));
        check(!StoryTrainerBindingGate.permitsCanonicalBoss(
            "director_cael", Set.of("pride_battle_director_cael"), true, false, 0D));
        System.out.println("PASS: " + count + " physical RCT trainer launch checks");
    }
}
