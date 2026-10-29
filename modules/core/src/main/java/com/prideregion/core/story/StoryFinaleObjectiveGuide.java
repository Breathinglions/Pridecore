package com.prideregion.core.story;

import java.util.*;

/** Read-only per-player directions derived from authenticated finale checkpoints. */
public final class StoryFinaleObjectiveGuide {
    private static final Set<String> CONDUITS = Set.of("north","south","east","west");
    private static final Set<String> SHIELDS = Set.of("alpha","beta","gamma");
    private StoryFinaleObjectiveGuide() {}

    public static String next(ResonanceFinaleController.Snapshot state,
                              Optional<ResonanceFinaleController.Checkpoint> checkpoint) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(checkpoint);
        var saved = checkpoint.orElse(null);
        if (saved != null && !saved.player().equals(state.player()))
            throw new IllegalArgumentException("Finale checkpoint belongs to another player");
        return switch (state.phase()) {
            case LOCKED -> "Defeat Director Cael and investigate the engine.";
            case CONDUITS -> "Charge remaining conduits: "
                + pending(CONDUITS, saved == null ? Set.of() : saved.conduits()) + ".";
            case SHIELDS -> "Defeat remaining defenders: "
                + pending(SHIELDS, saved == null ? Set.of() : saved.shields()) + ".";
            case STABILIZE -> "All shields down. Interact with the Resonance Engine core.";
            case COMPLETE -> "The Resonance Engine has been stabilized.";
        };
    }
    private static String pending(Set<String> required, Set<String> done) {
        var remaining = new TreeSet<>(required);
        remaining.removeAll(done);
        return remaining.isEmpty() ? "none" : String.join(", ", remaining);
    }
}
