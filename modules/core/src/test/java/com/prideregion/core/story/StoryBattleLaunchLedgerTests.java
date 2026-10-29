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
        ok(!ledger.reserve(launch));
        ok(ledger.launched(alice,UUID.randomUUID(),battle).isEmpty());
        ok(ledger.launched(bob,npc,battle).isEmpty());
        var verified=ledger.launched(alice,npc,battle).orElseThrow();
        ok(verified.battleId().equals(battle));
        ok(ledger.owns(battle, alice));
        ok(!ledger.owns(battle, bob));
        ok(verified.trainerIdentity().equals("pride_story:rin_route1"));
        ok(ledger.launched(alice,npc,battle).isEmpty());
        ok(!ledger.reserve(new StoryBattleLaunchLedger.Launch(alice,"sable",npc)));
        ledger.finished(battle);
        ok(!ledger.owns(battle, alice));
        ok(ledger.reserve(new StoryBattleLaunchLedger.Launch(alice,"sable",npc)));
        ledger.cancelPlayer(alice);
        ok(ledger.launched(alice,npc,UUID.randomUUID()).isEmpty());
        ok(ledger.reserve(new StoryBattleLaunchLedger.Launch(bob,"shield_gamma",npc)));
        ledger.clear();
        ok(ledger.launched(bob,npc,UUID.randomUUID()).isEmpty());
        System.out.println("PASS: "+checks+" RCT launch isolation assertions");
    }
}
