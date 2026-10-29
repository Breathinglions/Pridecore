package com.prideregion.core.story;

import java.util.*;

/**
 * Atmospheric text for authenticated environmental discoveries.
 * Does not authorize clues, grant facts or change Pokémon battle state.
 */
public final class StoryClueNarration {
    public record Passage(String discovered, String revisit) {
        public Passage {
            if (discovered == null || discovered.isBlank()
                    || revisit == null || revisit.isBlank())
                throw new IllegalArgumentException("Missing story passage");
        }
    }
    private static final Map<String, Passage> LINES = Map.ofEntries(
        Map.entry("forest", new Passage(
            "A strange silver dust glimmers beneath the ferns. Even the oldest roots have begun to glow.",
            "The silver dust still clings to the roots. The forest has not forgotten.")),
        Map.entry("mine", new Passage(
            "A torn survey page is wedged between the stones. Someone erased an entire passage from the map.",
            "The missing passage remains a mystery. The stone remembers where the ink was removed.")),
        Map.entry("foundry", new Passage(
            "The furnace is cold, yet its pipes hum with warmth. Something far away is still feeding the fire.",
            "An unseen current still moves through the silent pipes.")),
        Map.entry("river", new Passage(
            "A pulse of pale light travels upstream against the current, disappearing beneath the old bridge.",
            "Beneath the bridge, the water flickers with the same unnatural rhythm.")),
        Map.entry("grid", new Passage(
            "A hidden cable splits from the power grid, vanishing toward Meridian's sealed research grounds.",
            "The concealed cable still carries power to the forgotten facility.")),
        Map.entry("ward", new Passage(
            "Ancient symbols shimmer across the weathered stone. Their warning is older than any modern map.",
            "The old symbols are quiet now, but the warning remains.")),
        Map.entry("ice", new Passage(
            "Beneath the ice lies a report marked CONFIDENTIAL. The last page carries a warning no one answered.",
            "The frozen report still holds the words Meridian tried to bury.")),
        Map.entry("echo", new Passage(
            "A forgotten voice seems to rise from the empty laboratory, repeating the final moments of an experiment.",
            "The old laboratory echoes with memories of the same unfinished warning.")),
        Map.entry("engine", new Passage(
            "The Engine's readings stretch beyond the safe limits. Someone kept it running long after the alarms began.",
            "The impossible readings still burn across the abandoned display."))
    );
    private StoryClueNarration() {}

    public static Passage forClue(String clueId) {
        var passage = LINES.get(clueId);
        if (passage == null) throw new IllegalArgumentException("Unknown authored clue");
        return passage;
    }

    public static Set<String> authoredIds() { return LINES.keySet(); }
}
