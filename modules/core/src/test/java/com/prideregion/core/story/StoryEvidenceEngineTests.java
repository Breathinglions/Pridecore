package com.prideregion.core.story;

import com.prideregion.core.*;
import java.nio.file.Files;
import java.util.*;

/** Independent non-OP-equivalent server progression tests for world evidence. */
public final class StoryEvidenceEngineTests {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void rejects(Throwing task) throws Exception {
        checks++;
        try {
            task.run();
            throw new AssertionError("Expected server-side evidence refusal");
        } catch (IllegalStateException expected) { /* correct */ }
    }
    interface Throwing { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        var store = new ProgressionService(new ProfileStore(
            Files.createTempDirectory("pride-evidence-test-")));
        var tags = new HashMap<UUID,Set<String>>();
        var engine = new StoryEvidenceEngine(store,
            id -> tags.getOrDefault(id, Set.of()));
        check(StoryEvidenceEngine.definitions().size() == 9,
            "All nine evidence checkpoints registered");
        check(StoryEvidenceEngine.definition("fake") == null,
            "Unknown marker cannot resolve as evidence");

        for (var clue : StoryEvidenceEngine.definitions()) {
            UUID player = UUID.randomUUID();
            UUID outsider = UUID.randomUUID();
            UUID unbadged = UUID.randomUUID();
            tags.put(player, clue.requiresLeague() == null ? Set.of() :
                Set.of(clue.requiresLeague()));
            tags.put(outsider, tags.get(player));
            tags.put(unbadged, Set.of());
            rejects(() -> engine.complete(player, clue.id()));
            check(store.get(player).revision() == 0,
                "Denied discovery writes no profile");
            store.transition(player, "pride:test_story_stage",
                Set.of(clue.requiresFact()), Set.of());
            store.transition(unbadged, "pride:test_story_stage",
                Set.of(clue.requiresFact()), Set.of());
            if (clue.requiresLeague() != null) {
                rejects(() -> engine.complete(unbadged, clue.id()));
                check(!store.get(unbadged).facts().contains(clue.earnedFact()),
                    "Badge gate cannot be bypassed");
            }
            var first = engine.complete(player, clue.id());
            check(first.firstDiscovery(), "First authenticated discovery succeeds");
            check(first.clue().markerTag().equals("pride_clue_" + clue.id()),
                "Stable in-world interaction tag");
            var saved = store.get(player);
            check(saved.facts().contains(clue.earnedFact()),
                "Only owning player receives evidence");
            check(saved.storyStage().equals("pride:test_story_stage"),
                "Evidence cannot regress active narrative objective");
            var again = engine.complete(player, clue.id());
            check(!again.firstDiscovery() && store.get(player).revision() == saved.revision(),
                "Repeated clicks are idempotent and cannot duplicate rewards");
            check(!store.get(outsider).facts().contains(clue.earnedFact()),
                "Nearby unrelated players do not earn a discovery");
            check(StoryEvidenceEngine.discovered(store.get(player)).contains(clue.id()),
                "Clue is visible to FTB advancement projection");
            check(StoryEvidenceEngine.discovered(store.get(outsider)).isEmpty(),
                "No leaked FTB evidence for other player");
        }
        System.out.println("PASS: "+checks+" authenticated evidence gates, "
            + "no reward replay, chapter isolation and stage persistence");
    }
}
