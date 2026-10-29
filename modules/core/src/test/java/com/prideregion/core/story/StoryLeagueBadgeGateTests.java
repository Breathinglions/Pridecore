package com.prideregion.core.story;

import java.util.*;

public final class StoryLeagueBadgeGateTests {
    private static int assertions;
    private static void check(boolean success) {
        assertions++;
        if (!success) throw new AssertionError("Legacy Gym badge gate " + assertions);
    }
    public static void main(String[] args) {
        check(StoryLeagueBadgeGate.GYM_TAGS.size() == 8);
        check(StoryLeagueBadgeGate.count(Set.of()) == 0);
        check(StoryLeagueBadgeGate.count(Set.of("league_champion",
            "trainer_card_received", "league_started")) == 0);
        Set<String> collected = new HashSet<>();
        check(StoryLeagueBadgeGate.allows(0, collected));
        int count = 0;
        for (String badge : StoryLeagueBadgeGate.GYM_TAGS) {
            collected.add(badge);
            count++;
            check(StoryLeagueBadgeGate.count(collected) == count);
            check(StoryLeagueBadgeGate.allows(count, collected));
            if (count < 8) check(!StoryLeagueBadgeGate.allows(count + 1, collected));
        }
        collected.add("league_champion");
        collected.add("league_ghost_bogus");
        check(StoryLeagueBadgeGate.count(collected) == 8);
        boolean invalid = false;
        try { StoryLeagueBadgeGate.allows(9, collected); }
        catch (IllegalArgumentException expected) { invalid = true; }
        check(invalid);
        System.out.println("PASS: " + assertions + " original League badge gate assertions");
    }
}
