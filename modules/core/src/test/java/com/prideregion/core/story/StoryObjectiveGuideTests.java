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
        System.out.println("PASS: story objective follows authenticated Rin victory toward Ranger Mira");
    }
}
