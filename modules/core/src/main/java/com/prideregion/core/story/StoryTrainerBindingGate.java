package com.prideregion.core.story;

import java.util.*;

/**
 * Exact physical-actor authorization at the trusted RCT launch boundary.
 * The click listener is not sufficient: future adapters must obey this too.
 */
public final class StoryTrainerBindingGate {
    public static final double MAX_DISTANCE_SQUARED = 36.0D;
    private StoryTrainerBindingGate() {}

    public static boolean permits(String encounter, Collection<String> actorTags,
                                  boolean sameDimension, boolean playerActor,
                                  double squaredDistance) {
        if (encounter == null || actorTags == null || !sameDimension || playerActor
                || !Double.isFinite(squaredDistance) || squaredDistance < 0
                || squaredDistance > MAX_DISTANCE_SQUARED
                || !StoryBattleSessionRouter.acceptedTrainers().containsKey(encounter)
                || !StoryWorldMarkerCatalog.exclusiveStoryMarker(actorTags)) return false;
        return actorTags.contains("pride_battle_" + encounter);
    }
}
