package com.prideregion.core.story;

import java.util.*;

/**
 * Read-only inspection of an intentionally assembled finale arena.
 * Counts already-loaded physical world markers; never creates world content,
 * edits player profiles, or assumes where a builder placed the arena.
 */
public final class StoryArenaReadiness {
    public static final Set<String> REQUIRED = Set.of(
        "pride_finale_conduit_north",
        "pride_finale_conduit_south",
        "pride_finale_conduit_east",
        "pride_finale_conduit_west",
        "pride_finale_core",
        "pride_battle_shield_alpha",
        "pride_battle_shield_beta",
        "pride_battle_shield_gamma"
    );

    public record Result(Set<String> present, Set<String> missing,
                         Set<String> duplicate, int examined) {
        public boolean ready() {
            return missing.isEmpty() && duplicate.isEmpty();
        }
    }
    private StoryArenaReadiness() {}

    /** Every entry must represent one loaded physical entity's story tags. */
    public static Result check(Collection<? extends Set<String>> entityTags) {
        Objects.requireNonNull(entityTags);
        Map<String,Integer> counts = new HashMap<>();
        int examined = 0;
        for (Set<String> tags : entityTags) {
            Objects.requireNonNull(tags);
            examined++;
            for (String tag : tags)
                if (REQUIRED.contains(tag)) counts.merge(tag, 1, Integer::sum);
        }
        Set<String> missing = new TreeSet<>();
        Set<String> duplicate = new TreeSet<>();
        for (String required : REQUIRED) {
            int count = counts.getOrDefault(required, 0);
            if (count == 0) missing.add(required);
            else if (count > 1) duplicate.add(required);
        }
        return new Result(Set.copyOf(counts.keySet()), Set.copyOf(missing),
            Set.copyOf(duplicate), examined);
    }
}
