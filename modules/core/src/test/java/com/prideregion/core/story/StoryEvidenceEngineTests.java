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
        // Physical Verdant crystal click must complete the investigation
        // stage and unlock a fresh objective while preserving all later stages.
        UUID verdant = UUID.randomUUID();
        tags.put(verdant, Set.of("league_grass"));
        store.transition(verdant, "pride:forest_investigation",
            Set.of("pride:forest_evidence"), Set.of());
        long firstRevision = store.get(verdant).revision();
        check(engine.complete(verdant, "forest").firstDiscovery(),
            "Verified first crystal click records the forest discovery");
        check(store.get(verdant).storyStage().equals("pride:forest_traces_discovered"),
            "Amethyst clue advances the active objective toward Stonehaven");
        check(store.get(verdant).facts().contains("pride:clue_forest"),
            "Pride clue fact is persisted for per-player FTB advancement");
        check(store.get(verdant).revision() == firstRevision + 1,
            "Discovery fact and new stage commit atomically in one revision");
        long finishedRevision = store.get(verdant).revision();
        check(!engine.complete(verdant, "forest").firstDiscovery()
                && store.get(verdant).revision() == finishedRevision,
            "Repeat clicks don't announce or rewrite a completed crystal clue");

        UUID advanced = UUID.randomUUID();
        tags.put(advanced, Set.of("league_grass"));
        store.transition(advanced, "pride:rock_investigation",
            Set.of("pride:forest_evidence"), Set.of());
        check(engine.complete(advanced, "forest").firstDiscovery(),
            "Previously skipped forest evidence can be recovered legitimately");
        check(store.get(advanced).storyStage().equals("pride:rock_investigation"),
            "Late crystal click cannot regress a player already in Stonehaven");
        check(store.get(advanced).facts().contains("pride:clue_forest"),
            "Late evidence preserves story objective and fills missing advancement");

        // Stonehaven's survey-paper interaction completes the investigation
        // objective. The original Rock Gym badge remains external authority.
        UUID miner = UUID.randomUUID();
        tags.put(miner, Set.of("league_rock"));
        store.transition(miner, "pride:rock_investigation",
            Set.of("pride:mine_records"), Set.of());
        long mineBefore = store.get(miner).revision();
        check(engine.complete(miner, "mine").firstDiscovery(),
            "First authenticated survey page click records Rock Mine evidence");
        check(store.get(miner).storyStage().equals("pride:mine_evidence_discovered"),
            "Mine evidence moves objective to the Eclipse Scout");
        check(store.get(miner).facts().contains("pride:clue_mine"),
            "Mine clue is stored for FTB advancement projection");
        check(store.get(miner).revision() == mineBefore + 1,
            "Mine evidence and its objective commit atomically in one revision");
        long mineSaved = store.get(miner).revision();
        check(!engine.complete(miner, "mine").firstDiscovery()
                && store.get(miner).revision() == mineSaved,
            "Repeated mine clicks neither duplicate progress nor change the stage");

        UUID oldMine = UUID.randomUUID();
        tags.put(oldMine, Set.of("league_rock"));
        store.transition(oldMine, "pride:rock_investigation",
            Set.of("pride:mine_records", "pride:clue_mine"), Set.of());
        long oldMineRevision = store.get(oldMine).revision();
        check(StoryEvidenceEngine.reconcileSavedObjective(store, oldMine),
            "Already-discovered survey pages repair an older stale objective");
        check(store.get(oldMine).storyStage().equals("pride:mine_evidence_discovered")
                && store.get(oldMine).revision() == oldMineRevision + 1,
            "Mine objective repair persists with no second evidence fact");
        check(!StoryEvidenceEngine.reconcileSavedObjective(store, oldMine),
            "Repeated login/sync repair is a safe no-op");

        UUID repeatedOldMine = UUID.randomUUID();
        tags.put(repeatedOldMine, Set.of("league_rock"));
        store.transition(repeatedOldMine, "pride:rock_investigation",
            Set.of("pride:mine_records", "pride:clue_mine"), Set.of());
        check(!engine.complete(repeatedOldMine, "mine").firstDiscovery()
                && store.get(repeatedOldMine).storyStage().equals("pride:mine_evidence_discovered"),
            "Legacy player clicking a known mine clue repairs the objective once");

        UUID tooEarlyMine = UUID.randomUUID();
        tags.put(tooEarlyMine, Set.of("league_rock"));
        store.transition(tooEarlyMine, "pride:rock_investigation",
            Set.of("pride:mine_records"), Set.of());
        check(!StoryEvidenceEngine.reconcileSavedObjective(store, tooEarlyMine)
                && store.get(tooEarlyMine).storyStage().equals("pride:rock_investigation"),
            "Objective cannot advance merely because Lyra was consulted");

        UUID laterMine = UUID.randomUUID();
        tags.put(laterMine, Set.of("league_rock"));
        store.transition(laterMine, "pride:rival_debate",
            Set.of("pride:mine_records", "pride:clue_mine"), Set.of());
        check(!StoryEvidenceEngine.reconcileSavedObjective(store, laterMine)
                && store.get(laterMine).storyStage().equals("pride:rival_debate"),
            "Saved mine evidence never regresses a later chapter");
        check(!engine.complete(laterMine, "mine").firstDiscovery()
                && store.get(laterMine).storyStage().equals("pride:rival_debate"),
            "Repeated old clue click cannot roll back advanced players");

        System.out.println("PASS: "+checks+" authenticated evidence gates, "
            + "no reward replay, chapter isolation and stage persistence");
    }
}
