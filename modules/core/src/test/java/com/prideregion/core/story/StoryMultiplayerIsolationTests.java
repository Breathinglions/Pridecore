package com.prideregion.core.story;

import com.prideregion.core.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Same-server multiplayer race simulation using real progression/profile store.
 * The RCT event bus itself is tested later inside Minecraft.
 */
public final class StoryMultiplayerIsolationTests {
    private static int assertions;
    private static void check(boolean ok) {
        assertions++;
        if (!ok) throw new AssertionError("Multiplayer Module 02 isolation " + assertions);
    }
    public static void main(String[] args) throws Exception {
        var service = new ProgressionService(new ProfileStore(
            Files.createTempDirectory("pride-multiplayer-story-")));
        var ledger = new StoryBattleLaunchLedger();
        var router = new StoryBattleSessionRouter(new StoryBattleProof(service));
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        UUID trainerA = UUID.randomUUID(), trainerB = UUID.randomUUID();
        UUID battleA = UUID.randomUUID(), battleB = UUID.randomUUID();
        service.grant(a, "pride:met_rin");
        service.grant(b, "pride:met_rin");
        check(ledger.reserve(new StoryBattleLaunchLedger.Launch(a, "rin_route1", trainerA)));
        check(!ledger.reserve(new StoryBattleLaunchLedger.Launch(b, "rin_route1", trainerA)));
        check(ledger.reserve(new StoryBattleLaunchLedger.Launch(b, "rin_route1", trainerB)));
        var startA = ledger.launched(a, trainerA, battleA).orElseThrow();
        var startB = ledger.launched(b, trainerB, battleB).orElseThrow();
        check(ledger.matches(startA) && ledger.matches(startB));
        check(router.started(startA));
        check(router.started(startB));
        check(!ledger.matches(new StoryBattleSessionRouter.NativeStart(
            battleA, a, "sable", "pride_story:sable")));
        check(!router.ended(battleA, b)); // wrong winner retires only Alice's ticket
        ledger.finished(battleA);
        check(!service.get(a).facts().contains("pride:rin_route1_won"));
        check(router.ended(battleB, b));
        ledger.finished(battleB);
        check(service.get(b).facts().contains("pride:rin_route1_won"));
        check(!service.get(a).facts().contains("pride:rin_route1_won"));
        check(!ledger.trainerBusy(trainerA) && !ledger.trainerBusy(trainerB));
        UUID rematch = UUID.randomUUID();
        check(ledger.reserve(new StoryBattleLaunchLedger.Launch(a, "rin_route1", trainerA)));
        var startRetry = ledger.launched(a, trainerA, rematch).orElseThrow();
        check(router.started(startRetry));
        check(router.ended(rematch, a));
        ledger.finished(rematch);
        check(service.get(a).facts().contains("pride:rin_route1_won"));
        check(!ledger.active(a) && !ledger.active(b));

        // Two players entering the same physical finale retain separate phases.
        var finale = new ResonanceFinaleController();
        service.grant(a, "pride:cael_battle_won");
        service.grant(a, "pride:clue_engine");
        service.grant(b, "pride:cael_battle_won");
        service.grant(b, "pride:clue_engine");
        check(finale.start(a, true, true).conduits() == 0);
        check(finale.start(b, true, true).conduits() == 0);
        finale.conduit(a, "north");
        check(finale.snapshot(a).conduits() == 1);
        check(finale.snapshot(b).conduits() == 0);
        for (String cardinal : List.of("south","east","west"))
            finale.conduit(a, cardinal);
        check(finale.snapshot(a).phase() == ResonanceFinaleController.Phase.SHIELDS);
        check(finale.snapshot(b).phase() == ResonanceFinaleController.Phase.CONDUITS);
        for (String shield : List.of("alpha","beta","gamma"))
            finale.shieldVictory(a, shield);
        finale.stabilize(a);
        var completion = new ResonanceFinaleCompletion(service, finale);
        check(completion.complete(a));
        check(!completion.completed(b));
        finale.disconnect(b);
        check(finale.snapshot(a).phase() == ResonanceFinaleController.Phase.COMPLETE);
        check(finale.snapshot(b).phase() == ResonanceFinaleController.Phase.LOCKED);
        check(completion.completed(a) && !completion.completed(b));
        System.out.println("PASS: " + assertions + " concurrent story trainer and finale isolation checks");
    }
}
