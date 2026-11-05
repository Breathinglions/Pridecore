package com.prideregion.core.story;

import java.util.Set;

public final class StoryObjectiveGuideTests {
    private static void check(boolean condition, String explanation) {
        if (!condition) throw new AssertionError(explanation);
    }

    public static void main(String[] args) {
        String beginning = StoryObjectiveGuide.forStage(
            "pride:route1_orientation", Set.of("pride:met_rin"));
        check(beginning.contains("Rin is somewhere"),
            "Before the battle, the objective should still direct the player toward Rin");
        String victorious = StoryObjectiveGuide.forStage(
            "pride:route1_orientation", Set.of("pride:rin_route1_won"));
        check(victorious.contains("Ranger Mira") && !victorious.contains("Rin is somewhere"),
            "A verified Rin victory must direct the player to Ranger Mira");
        check(!victorious.equals(beginning), "Victory must update the objective");
        check(StoryObjectiveGuide.forStage("pride:verdant_approach",
            Set.of("pride:rin_route1_won")).contains("Thorn"),
            "An advanced story stage must override the earlier Rin milestone");
        check(StoryObjectiveGuide.forStage("pride:route1_orientation", Set.of())
                .equals(StoryObjectiveGuide.forStage("pride:route1_orientation")),
            "Old stage-only objective behavior must remain a compatible fallback");
        String noCatch = StoryObjectiveGuide.forStage(
            "pride:route1_orientation", Set.of("pride:rin_route1_won"), Set.of());
        check(noCatch.contains("Catch your first wild Pokémon"),
            "After Rin, the first-catch objective must be explicit if not yet complete");
        String caughtAndWon = StoryObjectiveGuide.forStage(
            "pride:route1_orientation", Set.of("pride:rin_route1_won"), Set.of("first_catch"));
        check(caughtAndWon.contains("Ranger Mira") && !caughtAndWon.contains("Catch your first wild"),
            "The already completed first catch must not be repeated after Rin");
        String caughtBeforeRin = StoryObjectiveGuide.forStage(
            "pride:route1_orientation", Set.of(), Set.of("first_catch"));
        check(caughtBeforeRin.contains("Challenge Rin"),
            "The first catch may be completed before the rival battle");
        System.out.println("PASS: story objective follows authenticated Rin victory toward Ranger Mira");
    }
}
