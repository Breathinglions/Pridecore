package com.prideregion.core.story;

import java.util.*;

/**
 * Read-only authored marker check for local staging build clusters.
 * This is not a loaded-world scan: callers supply nearby physical entities.
 */
public final class StoryZoneReadiness {
    private static final Map<String, Set<String>> ZONES = Map.of(
        "hearthleaf", Set.of(
            "pride_npc_mom", "pride_npc_professor_sequoia",
            "pride_npc_technician_theo", "pride_npc_avery",
            "pride_npc_ranger", "pride_npc_thorn",
            "pride_npc_rin", "pride_battle_rin_route1"),
        "eclipse", Set.of(
            "pride_npc_eclipse_operative", "pride_npc_sable",
            "pride_npc_director_cael", "pride_battle_eclipse_lieutenant",
            "pride_battle_sable", "pride_battle_director_cael"),
        "finale", StoryArenaReadiness.REQUIRED
    );
    private StoryZoneReadiness() {}
    public static Set<String> zones() { return ZONES.keySet(); }
    public static Set<String> required(String area) {
        var required = ZONES.get(area);
        if (required == null) throw new IllegalArgumentException("Unknown story audit zone");
        return required;
    }
    public record Report(String area, Set<String> present, Set<String> missing,
                         Set<String> duplicates, int conflictingEntities) {
        public boolean markersReady() {
            return missing.isEmpty() && duplicates.isEmpty() && conflictingEntities == 0;
        }
    }
    public static Report inspect(String area, Collection<? extends Set<String>> entities) {
        Objects.requireNonNull(entities);
        Set<String> required = required(area);
        Map<String,Integer> counts = new HashMap<>();
        int mixed = 0;
        for (Set<String> tags : entities) {
            if (!StoryWorldMarkerCatalog.exclusiveStoryMarker(tags)) mixed++;
            for (String tag : tags) if (required.contains(tag))
                counts.merge(tag, 1, Integer::sum);
        }
        Set<String> missing = new TreeSet<>(required);
        missing.removeAll(counts.keySet());
        Set<String> duplicates = new TreeSet<>();
        counts.forEach((tag,n) -> { if (n > 1) duplicates.add(tag); });
        return new Report(area, Set.copyOf(counts.keySet()),
            Set.copyOf(missing), Set.copyOf(duplicates), mixed);
    }
}
