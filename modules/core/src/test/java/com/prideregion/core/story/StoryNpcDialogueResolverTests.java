package com.prideregion.core.story;

import com.prideregion.core.*;
import java.nio.file.Files;
import java.util.*;

public final class StoryNpcDialogueResolverTests {
    private static int checks;
    private static void check(boolean good) {
        checks++;
        if (!good) throw new AssertionError("Bound NPC dialogue test " + checks);
    }
    private static StoryDefinition.Npc npc(String id, String requires, String earned) {
        var leave = new StoryDefinition.Option("goodbye", "See you soon.",
            null, null, Set.of(), Set.of(), Set.of());
        var main = new StoryDefinition.Node("hello", "Something is happening in the region.",
            List.of(leave));
        var repeat = new StoryDefinition.Node("return", "It is good to see you again.",
            List.of(leave));
        return new StoryDefinition.Npc(id, id, requires.isEmpty() ? Set.of()
            : Set.of(requires), earned, "hello", "return",
            Map.of("hello", main, "return", repeat));
    }

    public static void main(String[] args) throws Exception {
        var npcs = Map.of(
            "rin", npc("rin", "pride:met_avery", "pride:met_rin"),
            "rin_canyon", npc("rin_canyon", "pride:eclipse_contact", "pride:rival_theory"),
            "rin_summit", npc("rin_summit", "pride:frozen_warning", "pride:rival_ally"),
            "ranger_rowan", npc("ranger_rowan", "pride:water_evidence", "pride:ranger_probe"),
            "ranger_epilogue", npc("ranger_epilogue", "pride:cael_confronted",
                "pride:crisis_intel_complete"),
            "mom", npc("mom", "", "pride:met_mom"),
            "sequoia", npc("sequoia", "pride:met_mom", "pride:met_sequoia"));
        var story = new StoryDefinition(npcs);
        var facts = new HashSet<String>();
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_rin", facts).isEmpty());
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_mom", facts)
            .orElseThrow().equals("mom"));
        check(StoryNpcDialogueResolver.reservedNativeService(
            "pride_npc_professor_sequoia"));
        check(StoryNpcDialogueResolver.reservedNativeService("pride_npc_technician_theo"));
        check(StoryNpcDialogueResolver.reservedNativeService("pride_npc_avery"));
        check(StoryNpcDialogueResolver.reservedNativeService("pride_npc_thorn"));
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_professor_sequoia",
            Set.of("pride:met_mom")).isEmpty());
        check(!StoryNpcDialogueResolver.reservedNativeService("pride_npc_mom"));
        facts.add("pride:met_avery");
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_rin", facts)
            .orElseThrow().equals("rin"));
        facts.add("pride:met_rin");
        facts.add("pride:eclipse_contact");
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_rin", facts)
            .orElseThrow().equals("rin_canyon"));
        facts.add("pride:rival_theory");
        facts.add("pride:frozen_warning");
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_rin", facts)
            .orElseThrow().equals("rin_summit"));
        facts.add("pride:rival_ally");
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_rin", facts)
            .orElseThrow().equals("rin_summit"));
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_ranger_rowan", facts)
            .isEmpty());
        facts.add("pride:water_evidence");
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_ranger_rowan", facts)
            .orElseThrow().equals("ranger_rowan"));
        facts.add("pride:cael_confronted");
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_ranger_rowan", facts)
            .orElseThrow().equals("ranger_epilogue"));
        check(StoryNpcDialogueResolver.resolve(story, "pride_npc_fake", facts).isEmpty());

        var progression = new ProgressionService(new ProfileStore(
            Files.createTempDirectory("pride-story-native-clicks")));
        var engine = new StoryEngine(story, progression);
        UUID player = UUID.randomUUID();
        check(engine.beginBoundNpc(player, "pride_npc_rin").isEmpty());
        progression.grant(player, "pride:met_avery");
        check(engine.beginBoundNpc(player, "pride_npc_rin").orElseThrow()
            .npcId().equals("rin"));
        progression.grant(player, "pride:met_rin");
        progression.grant(player, "pride:eclipse_contact");
        check(engine.beginBoundNpc(player, "pride_npc_rin").orElseThrow()
            .npcId().equals("rin_canyon"));
        check(engine.beginBoundNpc(player, "pride_npc_professor_sequoia").isEmpty());
        System.out.println("PASS: " + checks + " authored native NPC chapter selections");
    }
}
