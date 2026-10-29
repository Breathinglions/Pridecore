package com.prideregion.core.story;

import com.prideregion.core.*;
import java.nio.file.Files;
import java.util.*;

/** Verifies the reusable battle-proof boundary without pretending to simulate Cobblemon. */
public final class StoryBattleProofTests {
    private static int checks;
    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("Story battle-proof invariant failed at " + checks);
    }
    public static void main(String[] args) throws Exception {
        var progression = new ProgressionService(
            new ProfileStore(Files.createTempDirectory("pride-battle-proof-")));
        var proofs = new StoryBattleProof(progression);
        check(StoryBattleProof.definitions().size() == 9);
        for (var definition : StoryBattleProof.definitions()) {
            UUID owner = UUID.randomUUID(), other = UUID.randomUUID();
            UUID battle = UUID.randomUUID();
            boolean locked = false;
            try { proofs.arm(owner, battle, definition.id()); }
            catch (IllegalStateException expected) { locked = true; }
            check(locked);
            progression.grant(owner, definition.requiredFact());
            var ticket = proofs.arm(owner, battle, definition.id());
            // The underlying Cobblemon battle UUID must never be shared by
            // two different players or assigned to another story encounter.
            progression.grant(other, definition.requiredFact());
            boolean collisionDenied = false;
            try { proofs.arm(other, battle, definition.id()); }
            catch (IllegalStateException expected) { collisionDenied = true; }
            check(collisionDenied);
            check(proofs.arm(owner, battle, definition.id()).equals(ticket));
            check(!proofs.victory(ticket, UUID.randomUUID(), owner));
            check(!proofs.victory(ticket, battle, other));
            check(!progression.get(owner).facts().contains(definition.earnedFact()));
            check(!progression.get(other).facts().contains(definition.earnedFact()));
            check(proofs.victory(ticket, battle, owner));
            check(progression.get(owner).facts().contains(definition.earnedFact()));
            check(!proofs.victory(ticket, battle, owner));
            UUID second = UUID.randomUUID();
            boolean replayDenied = false;
            try { proofs.arm(owner, second, definition.id()); }
            catch (IllegalStateException expected) { replayDenied = true; }
            check(replayDenied);
            check(progression.get(owner).facts().contains(definition.earnedFact()));
            UUID disconnectBattle = UUID.randomUUID();
            var dropped = proofs.arm(other, disconnectBattle, definition.id());
            proofs.cancelPlayer(other);
            check(!proofs.victory(dropped, disconnectBattle, other));
            UUID third = UUID.randomUUID();
            progression.grant(third, definition.requiredFact());
            var transferred = proofs.arm(third, disconnectBattle, definition.id());
            check(transferred.player().equals(third));
            proofs.clear();
            check(!proofs.victory(transferred, disconnectBattle, third));
            UUID externallyCredited = UUID.randomUUID();
            UUID staleBattle = UUID.randomUUID();
            progression.grant(externallyCredited, definition.requiredFact());
            var staleTicket = proofs.arm(externallyCredited, staleBattle, definition.id());
            progression.grant(externallyCredited, definition.earnedFact());
            check(!proofs.victory(staleTicket, staleBattle, externallyCredited));
            check(!proofs.victory(staleTicket, staleBattle, externallyCredited));
            // A stale ticket must not obstruct later unrelated story encounters.
            proofs.cancelPlayer(externallyCredited);
        }
        // A playthrough reset can revoke an encounter's prerequisite while a
        // trainer fight is active. The stale result must not lock future fights.
        UUID resetPlayer = UUID.randomUUID();
        String gate = "pride:met_rin";
        progression.grant(resetPlayer, gate);
        UUID abandonedId = UUID.randomUUID();
        var abandoned = proofs.arm(resetPlayer, abandonedId, "rin_route1");
        progression.revoke(resetPlayer, gate);
        check(!proofs.victory(abandoned, abandonedId, resetPlayer));
        check(!progression.get(resetPlayer).facts().contains("pride:rin_route1_won"));
        progression.grant(resetPlayer, gate);
        UUID retryId = UUID.randomUUID();
        var retryTicket = proofs.arm(resetPlayer, retryId, "rin_route1");
        check(retryTicket.battle().equals(retryId));
        check(proofs.victory(retryTicket, retryId, resetPlayer));
        System.out.println("PASS: " + checks + " story battle proof assertions");
    }
}
