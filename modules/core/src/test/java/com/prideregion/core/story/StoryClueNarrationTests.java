package com.prideregion.core.story;

import java.util.*;

public final class StoryClueNarrationTests {
    private static int checks;
    private static void check(boolean success) {
        checks++;
        if (!success) throw new AssertionError("Story discovery narration " + checks);
    }
    public static void main(String[] args) {
        Set<String> defined = new HashSet<>();
        for (var clue : StoryEvidenceEngine.definitions())
            defined.add(clue.id());
        check(StoryClueNarration.authoredIds().equals(defined));
        check(defined.size() == 9);
        for (String id : defined) {
            var scene = StoryClueNarration.forClue(id);
            check(scene.discovered().length() > 25);
            check(scene.revisit().length() > 25);
            check(!scene.discovered().equals(scene.revisit()));
            for (String forbidden : List.of("quest", "battle", "press ",
                "defeat ", "item id", "scoreboard", "complete the")) {
                check(!scene.discovered().toLowerCase(Locale.ROOT).contains(forbidden));
                check(!scene.revisit().toLowerCase(Locale.ROOT).contains(forbidden));
            }
        }
        boolean rejected = false;
        try { StoryClueNarration.forClue("fabricated"); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected);
        System.out.println("PASS: " + checks + " authored world clue narration assertions");
    }
}
