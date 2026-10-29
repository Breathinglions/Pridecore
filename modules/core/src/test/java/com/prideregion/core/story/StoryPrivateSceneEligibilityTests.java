package com.prideregion.core.story;

import java.util.*;

public final class StoryPrivateSceneEligibilityTests {
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("Scene eligibility check #" + checks);
    }

    private static StoryPrivateSceneEligibility.Availability evaluate(
            StoryPrivateSceneDirector.SceneId scene, String... facts) {
        return StoryPrivateSceneEligibility.evaluate(scene, Set.of(facts), Set.of());
    }

    public static void main(String[] args) {
        var lab = StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO;
        var canyon = StoryPrivateSceneDirector.SceneId.RIN_CANYON_REUNION;
        var summit = StoryPrivateSceneDirector.SceneId.RIN_SUMMIT_REUNION;
        var ready = StoryPrivateSceneEligibility.Availability.READY;
        var locked = StoryPrivateSceneEligibility.Availability.LOCKED;
        var passed = StoryPrivateSceneEligibility.Availability.ALREADY_PASSED;

        check(evaluate(lab) == locked);
        check(evaluate(lab, "pride:met_mom") == ready);
        check(StoryPrivateSceneEligibility.evaluate(lab, Set.of("pride:met_mom"),
            Set.of("starter_chosen")) == passed);
        check(evaluate(canyon) == locked);
        check(evaluate(canyon, "pride:eclipse_contact") == ready);
        check(evaluate(canyon, "pride:eclipse_contact", "pride:rival_theory") == passed);
        check(evaluate(summit, "pride:frozen_warning") == locked);
        check(evaluate(summit, "pride:clue_ice") == locked);
        check(evaluate(summit, "pride:frozen_warning", "pride:clue_ice") == ready);
        check(evaluate(summit, "pride:rival_ally") == passed);
        var owner = UUID.randomUUID();
        var session = UUID.randomUUID();
        var continuation = new StoryPrivateSceneDirector.Event(owner, session, lab,
            StoryPrivateSceneDirector.Kind.OPEN_NATIVE_STARTER, "Sequoia", "", "");
        check(StoryPrivateSceneEligibility.nativeStarterContinuationAllowed(continuation,
            Set.of("pride:met_mom"), Set.of()));
        check(!StoryPrivateSceneEligibility.nativeStarterContinuationAllowed(continuation,
            Set.of(), Set.of()));
        check(!StoryPrivateSceneEligibility.nativeStarterContinuationAllowed(continuation,
            Set.of("pride:met_mom"), Set.of("starter_chosen")));
        check(!StoryPrivateSceneEligibility.nativeStarterContinuationAllowed(null,
            Set.of("pride:met_mom"), Set.of()));
        var canyonContinuation = new StoryPrivateSceneDirector.Event(owner, session,
            canyon, StoryPrivateSceneDirector.Kind.RESUME_STORY_DIALOGUE, "Rin", "", "");
        check(!StoryPrivateSceneEligibility.nativeStarterContinuationAllowed(canyonContinuation,
            Set.of("pride:met_mom"), Set.of()));
        var spoofedType = new StoryPrivateSceneDirector.Event(owner, session, lab,
            StoryPrivateSceneDirector.Kind.ACTOR_MOVE, "Rin", "", "lab_entry");
        check(!StoryPrivateSceneEligibility.nativeStarterContinuationAllowed(spoofedType,
            Set.of("pride:met_mom"), Set.of()));
        // Atomic native integration begins only from server-supplied facts.
        var director = new StoryPrivateSceneDirector();
        check(!director.beginVerified(owner, "pride:staging", lab, Set.of(), Set.of()).started());
        check(director.active(owner).isEmpty());
        var authorized = director.beginVerified(owner, "pride:staging", lab,
            Set.of("pride:met_mom"), Set.of());
        check(authorized.started());
        check(!director.beginVerified(owner, "pride:staging", lab,
            Set.of("pride:met_mom"), Set.of()).started());
        check(director.active(owner).orElseThrow().session().equals(authorized.session()));
        check(director.cancel(owner).isPresent());
        check(!director.beginVerified(owner, "pride:staging", lab,
            Set.of("pride:met_mom"), Set.of("starter_chosen")).started());
        check(director.size() == 0);
        System.out.println("PASS: " + checks + " Rin scene prerequisite checks");
    }
}
