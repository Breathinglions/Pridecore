package com.prideregion.core.story;

import java.util.*;

/**
 * Chooses the appropriate authored chapter when a physical Cobblemon NPC
 * represents a recurring character. Never relies on Map iteration order.
 * A character with an unfulfilled chapter prerequisite is not available.
 */
public final class StoryNpcDialogueResolver {
    // These are native service NPCs; their own Cobblemon interaction must be
    // left alone (starter, registration, field kit and official Gym leader).
    private static final Set<String> NATIVE_SERVICES = Set.of(
        "pride_npc_professor_sequoia", "pride_npc_technician_theo",
        "pride_npc_avery", "pride_npc_thorn");

    // Latest chapter first. Eligibility is still checked against saved facts.
    private static final Map<String, List<String>> RECURRING = Map.of(
        "pride_npc_rin", List.of("rin_summit", "rin_canyon", "rin"),
        "pride_npc_ranger_rowan", List.of("ranger_epilogue", "ranger_rowan"));

    private StoryNpcDialogueResolver() {}

    public static boolean reservedNativeService(String tag) {
        return NATIVE_SERVICES.contains(tag);
    }

    public static Optional<String> resolve(StoryDefinition story, String marker,
                                           Set<String> earnedFacts) {
        Objects.requireNonNull(story);
        Objects.requireNonNull(earnedFacts);
        if (marker == null || reservedNativeService(marker)) return Optional.empty();
        List<String> candidates = RECURRING.get(marker);
        if (candidates == null) {
            candidates = new ArrayList<>();
            for (String id : NativeNpcIdentity.ALLOWED)
                if (NativeNpcIdentity.tagFor(id).equals(marker))
                    candidates.add(id);
        }
        // Prefer the newest eligible chapter the player has not finished.
        for (String id : candidates) {
            var npc = story.npcs().get(id);
            if (npc != null && earnedFacts.containsAll(npc.requires())
                    && !earnedFacts.contains(npc.completedFact()))
                return Optional.of(id);
        }
        // When all matching chapters are finished, replay the most recent
        // eligible authored conversation; never reopen a locked chapter.
        for (String id : candidates) {
            var npc = story.npcs().get(id);
            if (npc != null && earnedFacts.containsAll(npc.requires()))
                return Optional.of(id);
        }
        return Optional.empty();
    }
}
