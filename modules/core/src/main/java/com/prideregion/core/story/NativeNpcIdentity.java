package com.prideregion.core.story;

import java.util.Map;
import java.util.Set;

/** Native Cobblemon NPC tag identifiers; existing League spawn helpers use these. */
public final class NativeNpcIdentity {
    private static final Map<String, String> EXISTING = Map.of(
        "sequoia", "pride_npc_professor_sequoia",
        "theo", "pride_npc_technician_theo",
        "avery", "pride_npc_avery",
        // One physical actor; multiple story checkpoints for the same person.
        "rin_canyon", "pride_npc_rin",
        "rin_summit", "pride_npc_rin",
        "ranger_epilogue", "pride_npc_ranger_rowan"
    );
    // Complete authored Module 02 NPC roster; no generic Minecraft NPC IDs
    // or player-supplied strings can become story dialogue identities.
    public static final Set<String> ALLOWED = Set.of(
        "mom", "sequoia", "theo", "avery", "ranger", "thorn", "rin",
        "forest_warden", "miner_lyra", "eclipse_scout", "rin_canyon",
        "heat_technician", "meridian_scientist", "waterkeeper", "ranger_rowan",
        "vega_engineer", "cael_aide", "draven_historian", "ice_archivist",
        "rin_summit", "ghost_medium", "eclipse_operative", "sable",
        "director_cael", "ranger_epilogue", "league_guide",
        "hall_of_fame_guide");
    public static boolean isAllowed(String id) { return ALLOWED.contains(id); }

    private NativeNpcIdentity() {}

    /**
     * Native NPC classes are not equivalent to their command tags, display
     * names, skins or ForcedResourceIdentifier. Only the canonical class
     * may own a story role; a retagged Resource Ranger is not Mom.
     *
     * Original RCT Thorn and service NPC identities remain with their own
     * providers and are not guessed here.
     */
    public static String requiredClassForStoryTag(String tag) {
        if (tag == null) return null;
        for (String id : ALLOWED) {
            if (Set.of("sequoia", "theo", "avery", "thorn").contains(id))
                continue;
            if (!tagFor(id).equals(tag)) continue;
            String canonical = switch (id) {
                case "rin_canyon", "rin_summit" -> "rin";
                case "ranger_epilogue" -> "ranger_rowan";
                default -> id;
            };
            return "pride_story:" + canonical;
        }
        return null;
    }

    public static boolean matchesRequiredClass(String tag, String actualClass) {
        String required = requiredClassForStoryTag(tag);
        return required == null || required.equals(actualClass);
    }

    public static String tagFor(String storyId) {
        if (!StoryDefinition.validId(storyId) || !isAllowed(storyId))
            throw new IllegalArgumentException("Invalid story NPC identifier");
        return EXISTING.getOrDefault(storyId, "pride_npc_" + storyId);
    }
}
