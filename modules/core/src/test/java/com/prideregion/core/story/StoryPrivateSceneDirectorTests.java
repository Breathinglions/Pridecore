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
        int opens = 0, entrances = 0, removals = 0;
        for (int i = 0; i < 205; i++) {
            for (var event : director.tick()) {
                ok(event.owner().equals(alice) && event.session().equals(next.session()));
                if (event.kind() == StoryPrivateSceneDirector.Kind.OPEN_NATIVE_STARTER) opens++;
                if (event.kind() == StoryPrivateSceneDirector.Kind.ACTOR_ENTER) entrances++;
                if (event.kind() == StoryPrivateSceneDirector.Kind.ACTOR_REMOVE) removals++;
            }
        }
        ok(opens == 1 && entrances == 1 && removals == 1);
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
        System.out.println("PASS: " + assertions + " per-player Rin scene state assertions");
    }
}
