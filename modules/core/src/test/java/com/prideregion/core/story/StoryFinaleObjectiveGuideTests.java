package com.prideregion.core.story;

import java.util.*;

public final class StoryFinaleObjectiveGuideTests {
    private static int checks;
    private static void check(boolean good) {
        checks++;
        if (!good) throw new AssertionError("Finale player objective " + checks);
    }
    public static void main(String[] args) {
        var controller = new ResonanceFinaleController();
        UUID player = UUID.randomUUID();
        check(StoryFinaleObjectiveGuide.next(controller.snapshot(player),
            controller.checkpoint(player)).contains("Director Cael"));
        controller.start(player,true,true);
        String all = StoryFinaleObjectiveGuide.next(
            controller.snapshot(player), controller.checkpoint(player));
        for (String cardinal : List.of("north","south","east","west"))
            check(all.contains(cardinal));
        controller.conduit(player,"north");
        String remaining = StoryFinaleObjectiveGuide.next(
            controller.snapshot(player), controller.checkpoint(player));
        check(!remaining.contains("north"));
        check(remaining.contains("south"));
        for (String cardinal : List.of("south","east","west"))
            controller.conduit(player, cardinal);
        check(StoryFinaleObjectiveGuide.next(
            controller.snapshot(player),controller.checkpoint(player))
            .contains("alpha, beta, gamma"));
        controller.shieldVictory(player,"beta");
        String defender = StoryFinaleObjectiveGuide.next(
            controller.snapshot(player),controller.checkpoint(player));
        check(defender.contains("alpha") && defender.contains("gamma"));
        check(!defender.contains("beta"));
        controller.shieldVictory(player,"alpha");
        controller.shieldVictory(player,"gamma");
        check(StoryFinaleObjectiveGuide.next(
            controller.snapshot(player),controller.checkpoint(player))
            .contains("heart of the Resonance Engine"));
        controller.stabilize(player);
        check(StoryFinaleObjectiveGuide.next(
            controller.snapshot(player),controller.checkpoint(player))
            .contains("quiet at last"));
        boolean rejected = false;
        try {
            StoryFinaleObjectiveGuide.next(controller.snapshot(player),
                Optional.of(new ResonanceFinaleController.Checkpoint(
                    UUID.randomUUID(), ResonanceFinaleController.Phase.CONDUITS,
                    Set.of(), Set.of())));
        } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected);
        System.out.println("PASS: " + checks + " player-specific finale direction checks");
    }
}
