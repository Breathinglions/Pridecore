package com.prideregion.core.story;

import java.util.*;

/** Read-only, explicitly allowlisted legacy onboarding/League tags. */
public final class LegacyOnboardingGate {
    private static final Set<String> ALLOWED = Set.of(
        "journey_started","sequoia_met","starter_chosen",
        "trainer_card_received","pokepedia_received","trainer_guide_received",
        "field_kit_received","first_catch","first_battle_win","league_started",
        "league_grass","league_rock","league_fire","league_water",
        "league_electric","league_dragon","league_ice","league_ghost"
    );
    private LegacyOnboardingGate() {}
    public static boolean allowed(String tag) { return tag != null && ALLOWED.contains(tag); }
    public static Set<String> filter(Collection<String> tags) {
        Objects.requireNonNull(tags, "tags");
        Set<String> matches = new HashSet<>();
        for (String tag : tags) if (allowed(tag)) matches.add(tag);
        return Set.copyOf(matches);
    }
}
