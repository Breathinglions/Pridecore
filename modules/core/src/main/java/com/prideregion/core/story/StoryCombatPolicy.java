package com.prideregion.core.story;

import java.util.*;

/**
 * Explicit feature boundary: Module 02 story trainers use Pokémon battles
 * exclusively. Minecraft melee/damage phases belong to independent endgame
 * modules (Flashpoints, Operations, Raids, World Bosses), not this module.
 */
public final class StoryCombatPolicy {
    public enum Mode { POKEMON_ONLY, HYBRID }
    public enum System { STORY, FLASHPOINT, OPERATION, RAID, WORLD_BOSS }

    private StoryCombatPolicy() {}

    public static Mode requiredMode(System system) {
        Objects.requireNonNull(system);
        return system == System.STORY ? Mode.POKEMON_ONLY : Mode.HYBRID;
    }

    /** Strictly reject cross-module combat flags on authored story encounters. */
    public static void requirePokemonOnly(String encounterId, String mode) {
        if (encounterId == null
                || !StoryBattleSessionRouter.acceptedTrainers().containsKey(encounterId)
                || !Mode.POKEMON_ONLY.name().equals(mode))
            throw new IllegalArgumentException("Storyline encounters require Pokémon-only RCT battles");
    }

    /**
     * Refuse vanilla and other-mod Minecraft damage on a marked *story NPC*
     * actor only. Cobblemon battle Pokémon are not story trainer entities.
     * Do not protect unrelated players, mobs, League actors or hybrid bosses.
     */
    public static boolean protectTrainer(Collection<String> tags, boolean storyEnabled,
                                         boolean inAllowedWorld, boolean isPlayer) {
        if (!storyEnabled || !inAllowedWorld || isPlayer || tags == null
                || !StoryWorldMarkerCatalog.exclusiveStoryMarker(tags)) return false;
        for (String tag : tags) {
            if (tag.startsWith("pride_battle_")
                    && StoryBattleSessionRouter.acceptedTrainers().containsKey(
                        tag.substring("pride_battle_".length()))) return true;
        }
        return false;
    }
}
