package com.prideregion.core.story;

import java.util.Set;

/**
 * The only world tags an operator may bind through Module 02 setup commands.
 * No client-provided ID can be turned into an arbitrary scoreboard tag.
 * Contains no Minecraft dependencies so validation can run in standalone CI.
 */
public final class StoryWorldMarkerCatalog {
    private static final Set<String> FINALE_IDS =
        Set.of("north", "south", "east", "west", "core");

    private StoryWorldMarkerCatalog() {}

    public static String marker(String kind, String id) {
        if (kind == null || id == null) throw new IllegalArgumentException("Missing marker");
        return switch (kind) {
            case "battle" -> {
                if (!StoryBattleSessionRouter.acceptedTrainers().containsKey(id))
                    throw new IllegalArgumentException("Unknown story battle " + id);
                yield "pride_battle_" + id;
            }
            case "clue" -> {
                StoryEvidenceEngine.Clue clue = StoryEvidenceEngine.definition(id);
                if (clue == null) throw new IllegalArgumentException("Unknown story clue " + id);
                yield clue.markerTag();
            }
            case "finale" -> {
                if (!FINALE_IDS.contains(id))
                    throw new IllegalArgumentException("Unknown finale device " + id);
                yield id.equals("core") ? "pride_finale_core"
                    : "pride_finale_conduit_" + id;
            }
            default -> throw new IllegalArgumentException("Unknown marker type " + kind);
        };
    }

    /** Reject conflicting or unrecognized story tags, including stale typos. */
    public static boolean isStoryTag(String tag) {
        return tag != null && (tag.startsWith("pride_battle_")
            || tag.startsWith("pride_clue_")
            || tag.startsWith("pride_finale_"));
    }

    public static boolean canBind(Set<String> existing, String marker) {
        if (existing == null || marker == null || !isStoryTag(marker)) return false;
        for (String tag : existing)
            if (isStoryTag(tag) && !tag.equals(marker)) return false;
        return true;
    }

    public static Set<String> finaleIds() { return FINALE_IDS; }
}
