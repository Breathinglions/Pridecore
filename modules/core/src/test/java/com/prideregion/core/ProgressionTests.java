package com.prideregion.core;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Pure Java regression tests, independent of Fabric runtime. */
public final class ProgressionTests {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static void expectIOException(RunnableWithIOException call) throws Exception {
        try { call.run(); throw new AssertionError("Expected IOException"); }
        catch (IOException expected) { /* fail closed */ }
    }
    @FunctionalInterface private interface RunnableWithIOException { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("pride-core-test-");
        var store = new ProfileStore(directory);
        UUID one = UUID.randomUUID(), two = UUID.randomUUID();
        var blank = store.load(one);
        require(blank.revision() == 0 && blank.facts().isEmpty(), "New players start empty");
        require(!ProfileState.validKey("league_ghost"), "Must not import legacy tags into owned facts");
        require(!ProfileState.validKey("../escape"), "Must reject unsafe keys");
        require(!ProfileState.validKey("other:owned"), "Must reject foreign namespaces");
        var first = blank.withStage("pride:met_mom").withFact("pride:intro_done", true);
        // Each edit increases revision. Persist each transaction independently.
        var stage = blank.withStage("pride:met_mom");
        require(store.save(stage), "Stage save");
        var granted = stage.withFact("pride:intro_done", true);
        require(store.save(granted), "Fact save");
        require(!store.save(granted), "Idempotent same-revision repeat should be a no-op");
        var reloaded = store.load(one);
        require(reloaded.equals(granted), "Read-after-write");
        require(reloaded.withFact("pride:intro_done", true) == reloaded, "Idempotent grants");
        require(reloaded.withStage("pride:met_mom") == reloaded, "Idempotent stage");
        require(store.load(two).facts().isEmpty(), "Isolated player UUIDs");
        require(LegacyTagView.progressionTags(List.of("league_ghost", "pride_darkrai_portal_attuned", "admin", "league_ghost")).equals(
            List.of("league_ghost", "pride_darkrai_portal_attuned")), "Read-only legacy tag filter");
        expectIOException(() -> store.save(blank.withFact("pride:out_of_date", true)));
        require(store.load(one).equals(granted), "Stale save must not change current state");
        byte[] bytes = Files.readAllBytes(store.path(one));
        bytes[10] ^= (byte) 0x5A;
        Files.write(store.path(one), bytes);
        expectIOException(() -> store.load(one));
        expectIOException(() -> store.save(granted.withFact("pride:corrupt_override", true)));
        require(Files.readAllBytes(store.path(one))[10] == bytes[10], "Corrupt record never overwritten");
        System.out.println("PASS: profile validation, UUID isolation, durable load, revisions, idempotency, legacy view, corruption fail-closed");
    }
}
