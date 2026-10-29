package com.prideregion.core.story;

import java.util.*;

public final class StoryArenaReadinessTests {
    private static int checks;
    private static void check(boolean good) {
        checks++;
        if (!good) throw new AssertionError("Finale arena readiness check " + checks);
    }
    public static void main(String[] args) {
        check(StoryArenaReadiness.REQUIRED.size() == 8);
        var empty = StoryArenaReadiness.check(List.of());
        check(!empty.ready());
        check(empty.missing().size() == 8);
        check(empty.present().isEmpty());
        var complete = new ArrayList<Set<String>>();
        for (String tag : StoryArenaReadiness.REQUIRED)
            complete.add(Set.of(tag, "unrelated_other_mod_tag"));
        var ready = StoryArenaReadiness.check(complete);
        check(ready.ready());
        check(ready.present().size() == 8);
        check(ready.missing().isEmpty());
        check(ready.duplicate().isEmpty());
        check(ready.examined() == 8);
        var duplicate = new ArrayList<>(complete);
        duplicate.add(Set.of("pride_finale_core"));
        var bad = StoryArenaReadiness.check(duplicate);
        check(!bad.ready());
        check(bad.duplicate().equals(Set.of("pride_finale_core")));
        check(bad.missing().isEmpty());
        duplicate.removeIf(x -> x.contains("pride_battle_shield_beta"));
        var incomplete = StoryArenaReadiness.check(duplicate);
        check(incomplete.missing().contains("pride_battle_shield_beta"));
        check(incomplete.duplicate().contains("pride_finale_core"));
        check(!incomplete.ready());
        check(StoryArenaReadiness.check(List.of(Set.of("pride_finale_fake")))
            .present().isEmpty());
        System.out.println("PASS: " + checks + " local finale arena placement checks");
    }
}
