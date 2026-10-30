package com.prideregion.core.story;

import java.util.*;

public final class StoryPrivateSceneDirectorTests {
    private static int assertions;
    private static void ok(boolean b) {
        assertions++;
        if (!b) throw new AssertionError("Private Rin scene invariant #" + assertions);
    }
    public static void main(String[] args) {
        var director = new StoryPrivateSceneDirector();
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        var a = director.begin(alice, "minecraft:overworld",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        ok(a.started() && a.session() != null);
        var same = director.begin(alice, "minecraft:overworld",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        ok(!same.started() && a.session().equals(same.session()));
        var b = director.begin(bob, "minecraft:overworld",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        ok(b.started() && !a.session().equals(b.session()));
        var starts = director.tick();
        ok(starts.size() == 2);
        ok(starts.stream().anyMatch(x -> x.owner().equals(alice)
            && x.session().equals(a.session())
            && x.kind() == StoryPrivateSceneDirector.Kind.DIALOGUE));
        ok(starts.stream().anyMatch(x -> x.owner().equals(bob)
            && x.session().equals(b.session())));
        // Only Bob's scene is invalidated by his leaving the Lab's world.
        ok(director.checkDimension(bob, "minecraft:the_nether").isPresent());
        ok(director.active(bob).isEmpty() && director.active(alice).isPresent());
        ok(director.skip(bob, b.session()).isEmpty());
        ok(director.skip(alice, b.session()).isEmpty());
        var reduced = director.skip(alice, a.session());
        ok(reduced.stream().filter(e -> e.kind() ==
            StoryPrivateSceneDirector.Kind.DIALOGUE).count() == 4);
        ok(reduced.stream().filter(e -> e.kind() ==
            StoryPrivateSceneDirector.Kind.OPEN_NATIVE_STARTER).count() == 1);
        ok(reduced.stream().noneMatch(e -> e.kind() ==
            StoryPrivateSceneDirector.Kind.ACTOR_ENTER));
        ok(director.skip(alice, a.session()).isEmpty()); // one-use handoff
        ok(director.size() == 0);
        // Full duration yields the same one-use native UI request, no facts.
        var next = director.begin(alice, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        int opens = 0, entrances = 0, removals = 0, exits = 0;
        for (int i = 0; i < 205; i++) {
            for (var event : director.tick()) {
                ok(event.owner().equals(alice) && event.session().equals(next.session()));
                if (event.kind() == StoryPrivateSceneDirector.Kind.OPEN_NATIVE_STARTER) {
                    opens++;
                    ok(i == 165); // Resume only after the exit walk.
                }
                if (event.kind() == StoryPrivateSceneDirector.Kind.ACTOR_ENTER) entrances++;
                if (event.kind() == StoryPrivateSceneDirector.Kind.ACTOR_MOVE
                        && "lab_exit".equals(event.anchor())) {
                    exits++;
                    ok(i == 138); // 27 ticks until disappearance, enough for gait.
                }
                if (event.kind() == StoryPrivateSceneDirector.Kind.ACTOR_REMOVE) {
                    removals++;
                    ok(i == 165);
                }
            }
        }
        ok(opens == 1 && entrances == 1 && removals == 1 && exits == 1);
        ok(director.size() == 0);
        ok(director.cancel(alice).isEmpty());
        // Disconnect/shutdown does not send starter UI continuation.
        director.begin(alice, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        director.begin(bob, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        var cleanup = director.clear();
        ok(cleanup.size() == 2 && director.size() == 0);
        ok(cleanup.stream().allMatch(e -> e.kind() ==
            StoryPrivateSceneDirector.Kind.ACTOR_REMOVE));
        // Cap all private projections and free their slots without leaking states.
        var viewers = new ArrayList<UUID>();
        for (int i = 0; i < StoryPrivateSceneDirector.MAX_ACTIVE; i++) {
            UUID viewer = UUID.randomUUID();
            viewers.add(viewer);
            ok(director.begin(viewer, "pride:staging",
                StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO).started());
        }
        UUID excess = UUID.randomUUID();
        var rejected = director.begin(excess, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        ok(!rejected.started() && rejected.session() == null);
        ok(director.cancel(viewers.getFirst()).isPresent());
        ok(director.begin(excess, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO).started());
        ok(director.clear().size() == StoryPrivateSceneDirector.MAX_ACTIVE);
        boolean invalidDimension = false;
        try {
            director.begin(alice, "  ",
                StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        } catch (IllegalArgumentException expected) { invalidDimension = true; }
        ok(invalidDimension && director.size() == 0);
        // One canonical Rin identity, distinct staged appearances. No later
        // chapter can request a second starter UI or forge a story checkpoint.
        for (var scene : List.of(
                StoryPrivateSceneDirector.SceneId.RIN_CANYON_REUNION,
                StoryPrivateSceneDirector.SceneId.RIN_SUMMIT_REUNION)) {
            var later = director.begin(alice, "pride:staging", scene);
            ok(later.started());
            var fallback = director.skip(alice, later.session());
            ok(fallback.stream().filter(e -> e.kind() ==
                StoryPrivateSceneDirector.Kind.RESUME_STORY_DIALOGUE).count() == 1);
            ok(fallback.stream().noneMatch(e -> e.kind() ==
                StoryPrivateSceneDirector.Kind.OPEN_NATIVE_STARTER));
            ok(fallback.stream().allMatch(e -> e.scene() == scene
                && e.owner().equals(alice) && e.session().equals(later.session())));
        }
        var live = director.begin(alice, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_SUMMIT_REUNION);
        director.begin(bob, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_CANYON_REUNION);
        var reaped = director.reapInvalid(owner -> owner.equals(alice)
            ? "minecraft:overworld" : "pride:staging");
        ok(reaped.size() == 1 && reaped.getFirst().owner().equals(alice));
        ok(director.active(alice).isEmpty() && director.active(bob).isPresent());
        ok(director.skip(alice, live.session()).isEmpty());
        ok(director.reapInvalid(ignored -> null).size() == 1);
        ok(director.size() == 0);
        // Native continuation tickets must be single-use, world-bound and
        // expired promptly. Text preview does not invoke them.
        var token = director.begin(alice, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        var emitted = director.skip(alice, token.session());
        var handoff = emitted.stream().filter(e -> e.kind() ==
            StoryPrivateSceneDirector.Kind.OPEN_NATIVE_STARTER).findFirst().orElseThrow();
        ok(director.unclaimedContinuations() == 1);
        ok(!director.claimContinuation(handoff, "minecraft:overworld"));
        ok(director.claimContinuation(handoff, "pride:staging"));
        ok(!director.claimContinuation(handoff, "pride:staging"));
        ok(director.unclaimedContinuations() == 0);
        var another = director.begin(bob, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_CANYON_REUNION);
        var secondHandoff = director.skip(bob, another.session()).stream()
            .filter(e -> e.kind() == StoryPrivateSceneDirector.Kind.RESUME_STORY_DIALOGUE)
            .findFirst().orElseThrow();
        var spoof = new StoryPrivateSceneDirector.Event(alice, secondHandoff.session(),
            secondHandoff.scene(), secondHandoff.kind(),
            secondHandoff.speaker(), secondHandoff.text(), secondHandoff.anchor());
        ok(!director.claimContinuation(spoof, "pride:staging"));
        ok(!director.claimContinuation(handoff, "pride:staging"));
        ok(director.claimContinuation(secondHandoff, "pride:staging"));
        var exp = director.begin(alice, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        var expiring = director.skip(alice, exp.session()).stream()
            .filter(e -> e.kind() == StoryPrivateSceneDirector.Kind.OPEN_NATIVE_STARTER)
            .findFirst().orElseThrow();
        for (int i = 0; i < 122; i++) director.tick();
        ok(!director.claimContinuation(expiring, "pride:staging"));
        ok(director.unclaimedContinuations() == 0);
        var gone = director.begin(bob, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_CANYON_REUNION);
        var invalidated = director.skip(bob, gone.session()).stream()
            .filter(e -> e.kind() == StoryPrivateSceneDirector.Kind.RESUME_STORY_DIALOGUE)
            .findFirst().orElseThrow();
        director.reapInvalid(owner -> "minecraft:overworld");
        ok(!director.claimContinuation(invalidated, "pride:staging"));
        ok(director.unclaimedContinuations() == 0);
        var canceled = director.begin(alice, "pride:staging",
            StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO);
        var canceledHandoff = director.skip(alice, canceled.session()).stream()
            .filter(e -> e.kind() == StoryPrivateSceneDirector.Kind.OPEN_NATIVE_STARTER)
            .findFirst().orElseThrow();
        director.cancel(alice);
        ok(!director.claimContinuation(canceledHandoff, "pride:staging"));
        ok(director.unclaimedContinuations() == 0);
        System.out.println("PASS: " + assertions + " per-player Rin scene state assertions");
    }
}
