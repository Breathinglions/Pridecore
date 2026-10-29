package com.prideregion.core.story;

import com.prideregion.core.ProgressionService;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Durable story-finale completion boundary. Only trusted server-side engine
 * interaction handlers should invoke complete(), never chat commands or the
 * client's declared progress. A completed controller phase is necessary but
 * not sufficient: original boss win and world evidence remain mandatory.
 */
public final class ResonanceFinaleCompletion {
    public static final String FACT = "pride:finale_completed";
    private final ProgressionService progression;
    private final ResonanceFinaleController controller;

    public ResonanceFinaleCompletion(ProgressionService progression,
                                     ResonanceFinaleController controller) {
        this.progression = Objects.requireNonNull(progression);
        this.controller = Objects.requireNonNull(controller);
    }

    public synchronized boolean complete(UUID player) throws IOException {
        var profile = progression.get(player);
        if (!profile.facts().contains("pride:cael_battle_won")
                || !profile.facts().contains("pride:clue_engine"))
            throw new IllegalStateException("Finale story prerequisites missing");
        if (controller.snapshot(player).phase() != ResonanceFinaleController.Phase.COMPLETE)
            throw new IllegalStateException("Resonance Engine not fully stabilized");
        // A single atomic persistence step; never pays, changes badges or skips League.
        return progression.grant(player, FACT).changed();
    }

    public boolean completed(UUID player) throws IOException {
        return progression.get(player).facts().contains(FACT);
    }
}
