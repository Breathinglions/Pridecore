package com.prideregion.core.story;

import java.util.*;

public final class StoryBattleLaunchLedgerTests {
    private static int checks;
    private static void ok(boolean success) {
        if (!success) throw new AssertionError("RCT launch check " + (++checks));
        checks++;
    }
    public static void main(String[] args) {
        var ledger = new StoryBattleLaunchLedger();
        UUID alice=UUID.randomUUID(), bob=UUID.randomUUID();
        UUID npc=UUID.randomUUID(), battle=UUID.randomUUID();
        var launch=new StoryBattleLaunchLedger.Launch(alice,"rin_route1",npc);
        ok(ledger.reserve(launch));
        ok(ledger.pending(alice));
        ok(ledger.trainerBusy(npc));
        ok(!ledger.reserve(new StoryBattleLaunchLedger.Launch(
            bob, "rin_route1", npc)));

        ok(!ledger.active(alice));
        ok(!ledger.reserve(launch));
        ok(ledger.launched(alice,UUID.randomUUID(),battle).isEmpty());
        ok(ledger.launched(bob,npc,battle).isEmpty());
        var verified=ledger.launched(alice,npc,battle).orElseThrow();
        ok(verified.battleId().equals(battle));
        ok(ledger.owns(battle, alice));
        ok(ledger.owner(battle).orElseThrow().equals(alice));
        ok(ledger.activeBattles(alice).equals(Set.of(battle)));
        ok(ledger.activeBattles(bob).isEmpty());
        ok(ledger.activeBattles(null).isEmpty());
        ok(ledger.owner(UUID.randomUUID()).isEmpty());
        ok(!ledger.pending(alice));
        ok(ledger.active(alice));
        ok(ledger.trainerBusy(npc));
        ok(!ledger.reserve(new StoryBattleLaunchLedger.Launch(
            bob, "shield_gamma", npc)));

        ok(!ledger.owns(battle, bob));
        ok(verified.trainerIdentity().equals("pride_story:rin_route1"));
        ok(ledger.matches(verified));
        ok(!ledger.matches(new StoryBattleSessionRouter.NativeStart(
            battle, alice, "sable", "pride_story:sable")));
        ok(!ledger.matches(new StoryBattleSessionRouter.NativeStart(
            battle, bob, verified.encounterId(), verified.trainerIdentity())));
        ok(!ledger.matches(new StoryBattleSessionRouter.NativeStart(
            battle, alice, verified.encounterId(), "pride_story:forged")));

        ok(ledger.launched(alice,npc,battle).isEmpty());
        ok(!ledger.reserve(new StoryBattleLaunchLedger.Launch(alice,"sable",npc)));
        ledger.finished(battle);
        ok(ledger.activeBattles(alice).isEmpty());
        ok(!ledger.matches(verified));
        ok(ledger.owner(battle).isEmpty());
        ok(!ledger.trainerBusy(npc));

        ok(!ledger.owns(battle, alice));
        ok(!ledger.active(alice));
        ok(ledger.reserve(new StoryBattleLaunchLedger.Launch(alice,"sable",npc)));
        ledger.cancelPlayer(alice);
        ok(ledger.launched(alice,npc,UUID.randomUUID()).isEmpty());
        ok(ledger.reserve(new StoryBattleLaunchLedger.Launch(bob,"shield_gamma",npc)));
        ledger.clear();
        ok(ledger.launched(bob,npc,UUID.randomUUID()).isEmpty());
        UUID trainerAfterDisconnect = UUID.randomUUID();
        UUID activeBattle = UUID.randomUUID();
        ok(ledger.reserve(new StoryBattleLaunchLedger.Launch(alice,
            "rin_canyon", trainerAfterDisconnect)));
        ok(ledger.launched(alice, trainerAfterDisconnect, activeBattle).isPresent());
        ok(ledger.trainerBusy(trainerAfterDisconnect));
        ok(!ledger.reserve(new StoryBattleLaunchLedger.Launch(bob,
            "sable", trainerAfterDisconnect)));
        ledger.cancelPlayer(alice);
        ok(ledger.activeBattles(alice).isEmpty());
        ok(!ledger.trainerBusy(trainerAfterDisconnect));
        ok(!ledger.owns(activeBattle, alice));
        ok(ledger.reserve(new StoryBattleLaunchLedger.Launch(bob,
            "sable", trainerAfterDisconnect)));
        ledger.clear();
        ok(!ledger.trainerBusy(trainerAfterDisconnect));
        System.out.println("PASS: "+checks+" RCT launch isolation assertions");
    }
}
