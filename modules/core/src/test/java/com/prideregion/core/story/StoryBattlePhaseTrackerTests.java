package com.prideregion.core.story;

import java.util.*;

public final class StoryBattlePhaseTrackerTests {
    private static int checks;
    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("Boss phase tracker invariant " + checks);
    }
    public static void main(String[] args) {
        var tracker = new StoryBattlePhaseTracker();
        for (String id : StoryBattleSessionRouter.acceptedTrainers().keySet()) {
            UUID battle = UUID.randomUUID(), player = UUID.randomUUID();
            check(tracker.register(battle, player, id, 5));
            check(!tracker.register(battle, player, id, 5));
            UUID first = UUID.randomUUID();
            var beat = tracker.opponentFainted(battle, first).orElseThrow();
            check(beat.phase() == 1 && beat.defeated() == 1);
            check(beat.player().equals(player));
            check(tracker.opponentFainted(battle, first).isEmpty());
            check(tracker.opponentFainted(battle, UUID.randomUUID()).isEmpty());
            check(tracker.opponentFainted(battle, UUID.randomUUID())
                .orElseThrow().phase() == 2);
            check(tracker.opponentFainted(battle, UUID.randomUUID()).isEmpty());
            check(tracker.opponentFainted(battle, UUID.randomUUID()).isEmpty());
            check(tracker.opponentFainted(battle, UUID.randomUUID()).isEmpty());
            tracker.finish(battle);
            check(tracker.opponentFainted(battle, UUID.randomUUID()).isEmpty());
        }
        UUID shortBattle=UUID.randomUUID(), player=UUID.randomUUID();
        check(tracker.register(shortBattle, player, "shield_alpha", 2));
        check(tracker.opponentFainted(shortBattle, UUID.randomUUID()).orElseThrow().phase()==1);
        check(tracker.opponentFainted(shortBattle, UUID.randomUUID()).isEmpty());
        tracker.finish(shortBattle);
        check(!tracker.register(UUID.randomUUID(), player, "unknown_boss", 2));
        check(!tracker.register(UUID.randomUUID(), player, "sable", 1));
        check(!tracker.register(UUID.randomUUID(), player, "sable", 7));
        check(tracker.activeCount() == 0);
        System.out.println("PASS: " + checks + " knockout-driven boss phases");
    }
}
