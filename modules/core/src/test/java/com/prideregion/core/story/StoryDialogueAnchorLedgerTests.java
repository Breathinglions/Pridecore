package com.prideregion.core.story;

import java.util.*;

public final class StoryDialogueAnchorLedgerTests {
    private static int count;
    private static void check(boolean value) {
        count++;
        if (!value) throw new AssertionError("NPC conversation proximity invariant " + count);
    }
    public static void main(String[] args) {
        var ledger = new StoryDialogueAnchorLedger();
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        UUID actor = UUID.randomUUID(), imposter = UUID.randomUUID();
        check(ledger.get(alice).isEmpty());
        ledger.bind(alice, actor, "pride_npc_rin", "minecraft:overworld");
        check(ledger.get(bob).isEmpty());
        var anchor = ledger.get(alice).orElseThrow();
        check(StoryDialogueAnchorLedger.permits(anchor, actor, "pride_npc_rin",
            "minecraft:overworld", 0));
        check(StoryDialogueAnchorLedger.permits(anchor, actor, "pride_npc_rin",
            "minecraft:overworld", 36D));
        check(!StoryDialogueAnchorLedger.permits(anchor, actor, "pride_npc_rin",
            "minecraft:overworld", 36.0001D));
        check(!StoryDialogueAnchorLedger.permits(anchor, actor, "pride_npc_rin",
            "minecraft:overworld", -1D));
        check(!StoryDialogueAnchorLedger.permits(anchor, actor, "pride_npc_rin",
            "minecraft:overworld", Double.NaN));
        check(!StoryDialogueAnchorLedger.permits(anchor, actor, "pride_npc_rin",
            "minecraft:overworld", Double.POSITIVE_INFINITY));
        check(!StoryDialogueAnchorLedger.permits(anchor, imposter, "pride_npc_rin",
            "minecraft:overworld", 2D));
        check(!StoryDialogueAnchorLedger.permits(anchor, actor, "pride_npc_sable",
            "minecraft:overworld", 2D));
        check(!StoryDialogueAnchorLedger.permits(anchor, actor, "pride_npc_rin",
            "minecraft:the_nether", 2D));
        check(!StoryDialogueAnchorLedger.permits(null, actor, "pride_npc_rin",
            "minecraft:overworld", 2D));
        ledger.bind(bob, imposter, "pride_npc_sable", "minecraft:overworld");
        check(ledger.get(alice).orElseThrow().entity().equals(actor));
        ledger.clear(alice);
        check(ledger.get(alice).isEmpty());
        check(ledger.get(bob).isPresent());
        ledger.clearAll();
        check(ledger.get(bob).isEmpty());
        boolean failed = false;
        try { ledger.bind(alice, actor, "not_a_story_marker", "minecraft:overworld"); }
        catch (IllegalArgumentException expected) { failed = true; }
        check(failed);
        System.out.println("PASS: " + count + " physical story dialogue security checks");
    }
}
