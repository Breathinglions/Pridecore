package com.prideregion.core.story;

import java.util.*;

/**
 * Conservative prerequisite policy for a future trusted *native* story scene
 * adapter. This checks campaign facts, NOT Cobblemon starter ownership or the
 * security of a clicked NPC. The live adapter must additionally recheck the
 * actual Sequoia native service, player, world, distance and GUI entitlement.
 *
 * OP4 rehearsal deliberately does not use this gate; it is non-authoritative.
 */
public final class StoryPrivateSceneEligibility {
    public enum Availability { READY, LOCKED, ALREADY_PASSED }

    private StoryPrivateSceneEligibility() {}

    public static Availability evaluate(StoryPrivateSceneDirector.SceneId scene,
                                        Set<String> facts, Set<String> verifiedLegacy) {
        Objects.requireNonNull(scene);
        Objects.requireNonNull(facts);
        Objects.requireNonNull(verifiedLegacy);
        return switch (scene) {
            case RIN_LAB_INTRO -> {
                // Mom's storyline establishes legitimate context; a native
                // Sequoia interaction and real starter-selector eligibility
                // must be checked by the service, not by Pride Core.
                if (verifiedLegacy.contains("starter_chosen"))
                    yield Availability.ALREADY_PASSED;
                yield facts.contains("pride:met_mom")
                    ? Availability.READY : Availability.LOCKED;
            }
            case RIN_CANYON_REUNION -> {
                if (facts.contains("pride:rival_theory"))
                    yield Availability.ALREADY_PASSED;
                yield facts.contains("pride:eclipse_contact")
                    ? Availability.READY : Availability.LOCKED;
            }
            case RIN_SUMMIT_REUNION -> {
                if (facts.contains("pride:rival_ally"))
                    yield Availability.ALREADY_PASSED;
                yield facts.contains("pride:frozen_warning")
                    && facts.contains("pride:clue_ice")
                    ? Availability.READY : Availability.LOCKED;
            }
        };
    }

    /**
     * Only the Lab scene may request an original starter UI handoff. Calling
     * this does not perform a native action or grant starter eligibility.
     */
    public static boolean nativeStarterContinuationAllowed(StoryPrivateSceneDirector.Event event,
                                                            Set<String> facts, Set<String> verifiedLegacy) {
        return event != null
            && event.scene() == StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO
            && event.kind() == StoryPrivateSceneDirector.Kind.OPEN_NATIVE_STARTER
            && evaluate(event.scene(), facts, verifiedLegacy) == Availability.READY;
    }
}
