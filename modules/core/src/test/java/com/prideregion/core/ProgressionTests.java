package com.prideregion.core;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Non-Minecraft tests run on every commit, before Gradle/Loom. */
public final class ProgressionTests {
    private static int assertions;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }

    private static void ioFailure(Throwing action) throws Exception {
        assertions++;
        try { action.run(); throw new AssertionError("Expected IOException"); }
        catch (IOException expected) { /* failures must be non-destructive */ }
    }

    private static void illegal(Throwing action) throws Exception {
        assertions++;
        try { action.run(); throw new AssertionError("Expected IllegalArgumentException"); }
        catch (IllegalArgumentException expected) { /* invalid input rejected */ }
    }

    @FunctionalInterface private interface Throwing { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        var root = Files.createTempDirectory("pride-core-qa-");
        var store = new ProfileStore(root);
        var service = new ProgressionService(store);
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();

        check(service.get(alice).equals(ProfileState.empty(alice)), "New player starts clean");
        check(!ProfileState.validKey("league_ghost"), "External tag is not owned progression");
        check(!ProfileState.validKey("../escape"), "Unsafe key rejected");
        check(!ProfileState.validKey("other:owned"), "Other namespace rejected");
        check(!ProfileState.validKey("pride:"), "Empty key rejected");
        illegal(() -> service.grant(alice, "league_ghost"));
        illegal(() -> service.setStage(alice, "../invalid"));

        var first = service.setStage(alice, "pride:met_mom");
        check(first.changed() && first.profile().revision() == 1, "Stage is one committed revision");
        var grant = service.grant(alice, "pride:met_mom");
        check(grant.changed() && grant.profile().revision() == 2, "Grant is committed");
        var repeated = service.grant(alice, "pride:met_mom");
        check(!repeated.changed() && repeated.profile().revision() == 2,
            "Duplicate grant never increments revision");
        var repeatStage = service.setStage(alice, "pride:met_mom");
        check(!repeatStage.changed(), "Same stage never increments revision");
        check(service.get(alice).equals(grant.profile()), "Read-after-write stable");
        check(service.get(bob).equals(ProfileState.empty(bob)), "UUID isolation");
        check(Files.notExists(store.path(bob)), "Reads never materialize empty records");

        var multi = service.transition(alice, "pride:registration",
            Set.of("pride:trainer_registered", "pride:starter_selected"), Set.of("pride:met_mom"));
        check(multi.changed() && multi.profile().revision() == 3,
            "Multi-field transition commits only once");
        check(multi.profile().facts().equals(Set.of("pride:trainer_registered", "pride:starter_selected")),
            "Multi-field transaction applies grants and revokes");
        check(!service.transition(alice, "pride:registration",
            Set.of("pride:trainer_registered"), Set.of("pride:met_mom")).changed(),
            "Atomic multi-field replay idempotent");
        illegal(() -> service.transition(alice, null,
            Set.of("pride:conflict"), Set.of("pride:conflict")));
        check(service.get(alice).equals(multi.profile()), "Failed conflict changes nothing");
        illegal(() -> service.transition(alice, "pride:valid",
            List.of("invalid"), List.of()));
        check(service.get(alice).equals(multi.profile()), "Invalid transition changes nothing");

        var tags = List.of("league_ghost", "pride_darkrai_portal_attuned",
            "admin", "league_ghost", "chat_exempt");
        ProgressionView view = service.view(alice, tags);
        check(view.legacyTags().equals(List.of("league_ghost", "pride_darkrai_portal_attuned")),
            "External legacy tags sorted and filtered");
        check(view.hasLegacyTag("league_ghost"), "Legacy tag visible");
        check(!view.hasOwnedFact("league_ghost"), "Legacy tag not imported");
        check(view.hasOwnedFact("pride:trainer_registered"), "Owned progression visible");
        check(view.provenance("league_ghost").equals("legacy-read-only"), "Provenance correct");
        check(view.provenance("pride:trainer_registered").equals("pride-owned"),
            "Owned provenance correct");
        check(view.provenance("pride:missing").equals("absent"), "Missing provenance correct");
        unsupported(() -> view.owned().facts().add("pride:bad"));

        ioFailure(() -> store.save(ProfileState.empty(alice).withFact("pride:stale", true)));
        check(service.get(alice).equals(multi.profile()), "Stale revisions never overwrite");

        var reopened = new ProgressionService(new ProfileStore(root));
        check(reopened.get(alice).equals(multi.profile()), "Progression survives service restart");
        var remove = reopened.revoke(alice, "pride:starter_selected");
        check(remove.changed() && remove.profile().revision() == 4, "Removal persisted");
        check(!reopened.revoke(alice, "pride:starter_selected").changed(),
            "Duplicate revoke no-op");
        var byteCopy = Files.readAllBytes(store.path(alice));
        byteCopy[10] ^= 0x5A;
        Files.write(store.path(alice), byteCopy);
        ioFailure(() -> reopened.get(alice));
        ioFailure(() -> reopened.grant(alice, "pride:overwrite_corrupt"));
        check(Files.readAllBytes(store.path(alice))[10] == byteCopy[10],
            "Corrupt record not overwritten");

        UUID third = UUID.randomUUID();
        var full = ProfileState.empty(third);
        for (int i = 0; i < ProfileState.MAX_FACTS; i++)
            full = full.withFact("pride:t" + i, true);
        check(full.facts().size() == ProfileState.MAX_FACTS, "Fact limit reachable");
        final ProfileState finalFull = full;
        illegal(() -> finalFull.withFact("pride:overflow", true));
        illegal(() -> new ProfileState(third, -1, "pride:x", Set.of()));
        check(Files.exists(store.path(alice)), "Existing data remains intact");
        System.out.println("PASS: " + assertions +
            " checks for ownership, persistence, idempotency, atomicity, replay and corruption");
    }

    private static void unsupported(Throwing action) throws Exception {
        assertions++;
        try { action.run(); throw new AssertionError("Expected UnsupportedOperationException"); }
        catch (UnsupportedOperationException expected) { /* immutable profile */ }
    }
}
