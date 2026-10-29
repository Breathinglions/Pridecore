package com.prideregion.core.story;

import java.util.*;

/**
 * Read-only encounter readiness snapshot, consistent with the server-side
 * battle launch guards. Never starts battles or grants progression facts.
 */
public final class StoryEncounterAvailability {
    public enum Status { COMPLETE, LOCKED, BUSY, READY }
    public static final List<String> CAMPAIGN_ORDER = List.of(
        "rin_route1", "rin_canyon", "rin_summit",
        "eclipse_lieutenant", "sable", "director_cael",
        "shield_alpha", "shield_beta", "shield_gamma");

    private StoryEncounterAvailability() {}

    public static Status status(StoryBattleProof.Encounter encounter, Set<String> facts,
                                boolean shieldPhase, boolean battleOccupied,
                                int ownedBadges, int requiredBadges) {
        Objects.requireNonNull(encounter);
        Objects.requireNonNull(facts);
        if (facts.contains(encounter.earnedFact())) return Status.COMPLETE;
        if (!facts.contains(encounter.requiredFact())) return Status.LOCKED;
        if (ownedBadges < requiredBadges) return Status.LOCKED;
        if (encounter.id().startsWith("shield_") && !shieldPhase) return Status.LOCKED;
        if (battleOccupied) return Status.BUSY;
        return Status.READY;
    }

    public static List<StoryBattleProof.Encounter> ordered() {
        Map<String, StoryBattleProof.Encounter> byId = new HashMap<>();
        for (var entry : StoryBattleProof.definitions()) {
            if (byId.putIfAbsent(entry.id(), entry) != null)
                throw new IllegalStateException("Duplicate story encounter");
        }
        if (!byId.keySet().equals(Set.copyOf(CAMPAIGN_ORDER)))
            throw new IllegalStateException("Story encounter order does not match authored battles");
        return CAMPAIGN_ORDER.stream().map(byId::get).toList();
    }
}
