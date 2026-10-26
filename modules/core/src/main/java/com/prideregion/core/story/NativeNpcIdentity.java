package com.prideregion.core.story;

import java.util.Map;

/** Native Cobblemon NPC tag identifiers; existing League spawn helpers use these. */
public final class NativeNpcIdentity {
    private static final Map<String, String> EXISTING = Map.of(
        "sequoia", "pride_npc_professor_sequoia",
        "theo", "pride_npc_technician_theo",
        "avery", "pride_npc_avery",
        "ranger", "pride_npc_researcher_mira"
    );
    private NativeNpcIdentity() {}
    public static String tagFor(String storyId) {
        if (!StoryDefinition.validId(storyId))
            throw new IllegalArgumentException("Invalid story NPC identifier");
        return EXISTING.getOrDefault(storyId, "pride_npc_" + storyId);
    }
}
