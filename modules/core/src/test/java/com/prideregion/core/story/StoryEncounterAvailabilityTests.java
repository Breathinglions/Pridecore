package com.prideregion.core.story;

import java.util.*;

public final class StoryEncounterAvailabilityTests {
    private static int checks;
    private static void ok(boolean success) {
        checks++;
        if (!success) throw new AssertionError("Story encounter status invariant " + checks);
    }

    public static void main(String[] args) {
        var ordered = StoryEncounterAvailability.ordered();
        ok(ordered.size() == 9);
        ok(ordered.getFirst().id().equals("rin_route1"));
        ok(ordered.getLast().id().equals("shield_gamma"));
        for (var encounter : ordered) {
            Set<String> empty = Set.of();
            ok(StoryEncounterAvailability.status(encounter, empty, true, false)
                == StoryEncounterAvailability.Status.LOCKED);
            Set<String> unlocked = Set.of(encounter.requiredFact());
            if (encounter.id().startsWith("shield_")) {
                ok(StoryEncounterAvailability.status(encounter, unlocked, false, false)
                    == StoryEncounterAvailability.Status.LOCKED);
            }
            ok(StoryEncounterAvailability.status(encounter, unlocked, true, false)
                == StoryEncounterAvailability.Status.READY);
            ok(StoryEncounterAvailability.status(encounter, unlocked, true, true)
                == StoryEncounterAvailability.Status.BUSY);
            Set<String> won = Set.of(encounter.requiredFact(), encounter.earnedFact());
            ok(StoryEncounterAvailability.status(encounter, won, false, true)
                == StoryEncounterAvailability.Status.COMPLETE);
        }
        System.out.println("PASS: " + checks + " story encounter readiness checks");
    }
}
