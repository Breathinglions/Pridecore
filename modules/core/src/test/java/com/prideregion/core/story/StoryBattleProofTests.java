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
        check(StoryBattleProof.definitions().size() == 6);
        for (var definition : StoryBattleProof.definitions()) {
            UUID owner = UUID.randomUUID(), other = UUID.randomUUID();
            UUID battle = UUID.randomUUID();
            boolean locked = false;
            try { proofs.arm(owner, battle, definition.id()); }
            catch (IllegalStateException expected) { locked = true; }
            check(locked);
            progression.grant(owner, definition.requiredFact());
            var ticket = proofs.arm(owner, battle, definition.id());
            check(!proofs.victory(ticket, UUID.randomUUID(), owner));
            check(!proofs.victory(ticket, battle, other));
            check(!progression.get(owner).facts().contains(definition.earnedFact()));
            check(!progression.get(other).facts().contains(definition.earnedFact()));
            check(proofs.victory(ticket, battle, owner));
            check(progression.get(owner).facts().contains(definition.earnedFact()));
            check(!proofs.victory(ticket, battle, owner));
            UUID second = UUID.randomUUID();
            var pending = proofs.arm(owner, second, definition.id());
            proofs.cancel(owner, second);
            check(!proofs.victory(pending, second, owner));
            check(progression.get(owner).facts().contains(definition.earnedFact()));
        }
        System.out.println("PASS: " + checks + " story battle proof assertions");
    }
}
