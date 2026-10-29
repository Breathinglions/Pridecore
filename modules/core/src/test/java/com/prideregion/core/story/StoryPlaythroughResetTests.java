package com.prideregion.core.story;

import com.prideregion.core.*;
import java.nio.file.Files;
import java.util.*;

public final class StoryPlaythroughResetTests {
    private static int checks;
    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Reset test failed at " + checks);
    }
    public static void main(String[] args) throws Exception {
        var service = new ProgressionService(new ProfileStore(
            Files.createTempDirectory("story-playthrough-reset-")));
        var npc = new StoryDefinition.Npc("mom", "Mom", Set.of(),
            "pride:met_mom", "start", "repeat", Map.of(
                "start", new StoryDefinition.Node("start", "Hello", List.of(
                    new StoryDefinition.Option("go", "Go", null, "pride:visit_sequoia",
                        Set.of("pride:met_mom"), Set.of(), Set.of()))),
                "repeat", new StoryDefinition.Node("repeat", "Again", List.of())));
        var reset = new StoryPlaythroughReset(service,
            new StoryDefinition(Map.of("mom", npc)));
        UUID player = UUID.randomUUID(), other = UUID.randomUUID();
        service.transition(player, "pride:engine_confrontation",
            Set.of("pride:met_mom", "pride:sable_battle_won", "pride:clue_engine",
                "pride:finale_completed", "pride:unrelated_module_fact"), Set.of());
        service.grant(other, "pride:met_mom");
        var preview = reset.preview(player);
        check(preview.campaignFacts() == 4);
        check(preview.preserved().equals(Set.of("pride:unrelated_module_fact")));
        long rev = service.get(player).revision();
        var changed = reset.reset(player);
        check(changed.changed());
        check(changed.profile().revision() == rev + 1);
        check(changed.profile().storyStage().equals("pride:unstarted"));
        check(changed.profile().facts().equals(Set.of("pride:unrelated_module_fact")));
        check(service.get(other).facts().contains("pride:met_mom"));
        check(!reset.reset(player).changed());
        check(service.get(player).revision() == rev + 1);
        System.out.println("PASS: " + checks + " selective playthrough reset checks");
    }
}
