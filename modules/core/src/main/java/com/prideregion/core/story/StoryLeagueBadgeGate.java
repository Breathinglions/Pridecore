package com.prideregion.core.story;

import java.util.*;

/**
 * Read-only badge progression from the original eight Gym/League entity tags.
 * Module 02 neither grants badges nor changes RCT gym and League rewards.
 */
public final class StoryLeagueBadgeGate {
    public static final Set<String> GYM_TAGS = Set.of(
        "league_grass", "league_rock", "league_fire", "league_water",
        "league_electric", "league_dragon", "league_ice", "league_ghost");
    private StoryLeagueBadgeGate() {}

    public static int count(Collection<String> playerTags) {
        Objects.requireNonNull(playerTags);
        int found = 0;
        for (String tag : GYM_TAGS)
            if (playerTags.contains(tag)) found++;
        return found;
    }

    public static boolean allows(int requiredBadges, Collection<String> playerTags) {
        if (requiredBadges < 0 || requiredBadges > 8)
            throw new IllegalArgumentException("Invalid Gym badge threshold");
        return count(playerTags) >= requiredBadges;
    }
}
