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
            case LOCKED -> "Director Cael is waiting at Meridian Institute. The engine cannot remain as it is.";
            case CONDUITS -> "The old conduits still pulse at "
                + pending(CONDUITS, saved == null ? Set.of() : saved.conduits()) + ".";
            case SHIELDS -> "The engine is still guarded by the "
                + pending(SHIELDS, saved == null ? Set.of() : saved.shields()) + " seals.";
            case STABILIZE -> "The last seal is silent. Only the heart of the Resonance Engine remains.";
            case COMPLETE -> "The Resonance Engine is quiet at last. The region can begin to heal.";
        };
    }
    private static String pending(Set<String> required, Set<String> done) {
        var remaining = new TreeSet<>(required);
        remaining.removeAll(done);
        return remaining.isEmpty() ? "none" : String.join(", ", remaining);
    }
}
