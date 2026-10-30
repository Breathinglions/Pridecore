package com.prideregion.core.story;

import java.util.*;

/**
 * Per-player Rin dialogue selector. Only the original eight verified League
 * tags count toward badges; existing badges cannot skip narrative milestones.
 * Returned dialogue IDs are fixed server-owned resources, not client input.
 */
public final class RinChapterGate {
    public enum Scene {
        ROUTE1("rin_route1", "pride_story:rin_route1"),
        CANYON("rin_canyon", "pride_story:rin_canyon"),
        SUMMIT("rin_summit", "pride_story:rin_summit"),
        RETURN(null, "pride_story:rin_return");

        private final String encounter;
        private final String dialogue;
        Scene(String encounter, String dialogue) {
            this.encounter = encounter;
            this.dialogue = dialogue;
        }
        public String encounter() { return encounter; }
        public String dialogue() { return dialogue; }
    }

    private RinChapterGate() {}

    public static Scene choose(Set<String> facts, Collection<String> leagueTags) {
        Objects.requireNonNull(facts);
        int badges = StoryLeagueBadgeGate.count(leagueTags);
        // Completing later chapters never authorizes skipping an earlier win.
        if (facts.contains("pride:rin_canyon_won")
                && facts.contains("pride:rin_route1_won")
                && facts.contains("pride:frozen_warning") && badges >= 7
                && !facts.contains("pride:rin_summit_won")) return Scene.SUMMIT;
        if (facts.contains("pride:rin_route1_won")
                && facts.contains("pride:eclipse_contact") && badges >= 2
                && !facts.contains("pride:rin_canyon_won")) return Scene.CANYON;
        if (facts.contains("pride:met_rin") && facts.contains("pride:met_avery")
                && !facts.contains("pride:rin_route1_won")) return Scene.ROUTE1;
        return Scene.RETURN;
    }
}
