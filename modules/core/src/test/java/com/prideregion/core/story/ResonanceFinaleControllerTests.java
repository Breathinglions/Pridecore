package com.prideregion.core.story;

import java.util.*;

public final class ResonanceFinaleControllerTests {
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("Finale invariant failed: " + checks);
    }
    private static void denied(Runnable action) {
        checks++;
        try { action.run(); throw new AssertionError("Expected phase refusal"); }
        catch (IllegalStateException expected) { }
    }
    public static void main(String[] args) {
        var finale = new ResonanceFinaleController();
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        check(finale.start(alice, false, true).phase() == ResonanceFinaleController.Phase.LOCKED);
        check(finale.start(alice, true, false).phase() == ResonanceFinaleController.Phase.LOCKED);
        denied(() -> finale.conduit(alice, "north"));
        check(finale.start(alice, true, true).phase() == ResonanceFinaleController.Phase.CONDUITS);
        check(finale.conduit(alice, "north").conduits() == 1);
        var stable = finale.checkpoint(alice).orElseThrow();
        check(finale.conduit(alice, "north").conduits() == 1);
        check(finale.checkpoint(alice).orElseThrow().equals(stable));
        for (String direction : List.of("south", "east", "west"))
            finale.conduit(alice, direction);
        check(finale.snapshot(alice).phase() == ResonanceFinaleController.Phase.SHIELDS);
        check(finale.snapshot(alice).conduits() == 4);
        denied(() -> finale.stabilize(alice));
        for (String shield : List.of("alpha", "beta", "gamma"))
            finale.shieldVictory(alice, shield);
        check(finale.snapshot(alice).phase() == ResonanceFinaleController.Phase.STABILIZE);
        check(finale.snapshot(bob).phase() == ResonanceFinaleController.Phase.LOCKED);
        check(finale.stabilize(alice).phase() == ResonanceFinaleController.Phase.COMPLETE);
        denied(() -> finale.stabilize(alice));
        var saved = finale.checkpoint(alice).orElseThrow();
        finale.disconnect(alice);
        check(finale.snapshot(alice).phase() == ResonanceFinaleController.Phase.LOCKED);
        check(finale.restore(saved).phase() == ResonanceFinaleController.Phase.COMPLETE);
        checks++;
        try {
            finale.restore(new ResonanceFinaleController.Checkpoint(
                bob, ResonanceFinaleController.Phase.COMPLETE,
                Set.of("north"), Set.of("alpha")));
            throw new AssertionError("Invalid checkpoint accepted");
        } catch (IllegalArgumentException expected) { }
        check(finale.snapshot(bob).phase() == ResonanceFinaleController.Phase.LOCKED);
        finale.disconnect(alice);
        check(finale.snapshot(alice).phase() == ResonanceFinaleController.Phase.LOCKED);
        finale.start(bob, true, true);
        finale.clear();
        check(finale.snapshot(bob).phase() == ResonanceFinaleController.Phase.LOCKED);
        System.out.println("PASS: " + checks + " resonance finale phase isolation assertions");
    }
}
