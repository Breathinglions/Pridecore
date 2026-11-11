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
    /**
     * Rin is one physical Cobblemon NPC across all chapters. His exact native
     * marker is a trusted alternative only for the three fixed Rin encounters.
     * It does not authorize other scripted bosses, Gym NPCs or player actors.
     */
    public static boolean permitsCanonicalRin(String encounter, Collection<String> tags,
            boolean sameDimension, boolean playerActor, double distanceSquared) {
        if (!Set.of("rin_route1","rin_canyon","rin_summit").contains(encounter)
                || tags == null || !sameDimension || playerActor
                || !Double.isFinite(distanceSquared) || distanceSquared < 0
                || distanceSquared > MAX_DISTANCE_SQUARED
                || !StoryWorldMarkerCatalog.exclusiveStoryMarker(tags))
            return false;
        return tags.contains("pride_npc_rin")
            && tags.stream().noneMatch(t -> t.startsWith("pride_battle_"));
    }

    /**
     * Eclipse Lieutenant, Sable and Director Cael use ONE class-verified
     * Cobblemon NPC for both their dialogue and authenticated RCT battle.
     * Their native NPC tag is accepted only for its exact registered battle,
     * never another boss, a Gym trainer, player actor or mixed-tag entity.
     * The calling server must ALSO verify NPCClass and chapter facts.
     */
    public static boolean permitsCanonicalBoss(String encounter, Collection<String> tags,
            boolean sameDimension, boolean playerActor, double distanceSquared) {
        String expectedTag = switch (encounter == null ? "" : encounter) {
            case "eclipse_lieutenant" -> "pride_npc_eclipse_operative";
            case "sable" -> "pride_npc_sable";
            case "director_cael" -> "pride_npc_director_cael";
            default -> null;
        };
        return expectedTag != null && tags != null && sameDimension && !playerActor
            && Double.isFinite(distanceSquared) && distanceSquared >= 0
            && distanceSquared <= MAX_DISTANCE_SQUARED
            && StoryWorldMarkerCatalog.exclusiveStoryMarker(tags)
            && tags.contains(expectedTag)
            && tags.stream().noneMatch(t -> t.startsWith("pride_battle_"));
    }

}
