package com.prideregion.core.story;

import java.util.*;

public final class StoryZoneReadinessTests {
    private static int count;
    private static void check(boolean valid) {
        count++;
        if (!valid) throw new AssertionError("Staging zone " + count);
    }
    public static void main(String[] args) {
        check(StoryZoneReadiness.zones().equals(Set.of("hearthleaf", "eclipse", "finale")));
        check(StoryZoneReadiness.required("hearthleaf").size() == 8);
        check(StoryZoneReadiness.required("eclipse").size() == 6);
        check(StoryZoneReadiness.required("finale").equals(StoryArenaReadiness.REQUIRED));
        for (String area : StoryZoneReadiness.zones()) {
            var empty = StoryZoneReadiness.inspect(area, List.of());
            check(!empty.markersReady());
            check(empty.missing().equals(StoryZoneReadiness.required(area)));
            List<Set<String>> markers = new ArrayList<>();
            for (String id : StoryZoneReadiness.required(area))
                markers.add(Set.of(id, "unrelated_tag"));
            var ready = StoryZoneReadiness.inspect(area, markers);
            check(ready.markersReady());
            check(ready.missing().isEmpty());
            check(ready.duplicates().isEmpty());
            markers.add(Set.of(markers.getFirst().stream()
                .filter(StoryWorldMarkerCatalog::isStoryTag).findFirst().orElseThrow()));
            check(StoryZoneReadiness.inspect(area, markers).duplicates().size() == 1);
            markers.add(Set.of("pride_battle_fake", "pride_npc_fake",
                StoryZoneReadiness.required(area).iterator().next()));
            var invalid = StoryZoneReadiness.inspect(area, markers);
            check(invalid.conflictingEntities() == 1);
            check(!invalid.markersReady());
        }
        boolean rejected = false;
        try { StoryZoneReadiness.required("forged"); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected);
        System.out.println("PASS: " + count + " authored staging zone readiness checks");
    }
}
