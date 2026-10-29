package com.prideregion.core.story;

import java.util.*;

public final class StoryCombatPolicyTests {
    private static int checks;
    private static void check(boolean expected) {
        checks++;
        if (!expected) throw new AssertionError("Pokémon-only story combat invariant " + checks);
    }
    private static void rejects(String encounter, String mode) {
        boolean rejected = false;
        try { StoryCombatPolicy.requirePokemonOnly(encounter, mode); }
        catch (IllegalArgumentException correct) { rejected = true; }
        check(rejected);
    }

    public static void main(String[] args) {
        check(StoryCombatPolicy.requiredMode(StoryCombatPolicy.System.STORY)
            == StoryCombatPolicy.Mode.POKEMON_ONLY);
        for (var system : List.of(StoryCombatPolicy.System.FLASHPOINT,
                StoryCombatPolicy.System.OPERATION, StoryCombatPolicy.System.RAID,
                StoryCombatPolicy.System.WORLD_BOSS)) {
            check(StoryCombatPolicy.requiredMode(system) == StoryCombatPolicy.Mode.HYBRID);
        }

        for (String encounter : StoryBattleSessionRouter.acceptedTrainers().keySet()) {
            StoryCombatPolicy.requirePokemonOnly(encounter, "POKEMON_ONLY");
            check(StoryCombatPolicy.protectTrainer(Set.of("pride_battle_" + encounter),
                true, true, false));
            check(StoryCombatPolicy.protectTrainer(Set.of("pride_battle_" + encounter,
                "third_party_custom"), true, true, false));
            check(!StoryCombatPolicy.protectTrainer(Set.of("pride_battle_" + encounter),
                false, true, false));
            check(!StoryCombatPolicy.protectTrainer(Set.of("pride_battle_" + encounter),
                true, false, false));
            check(!StoryCombatPolicy.protectTrainer(Set.of("pride_battle_" + encounter),
                true, true, true));
            check(!StoryCombatPolicy.protectTrainer(Set.of("pride_battle_" + encounter,
                "pride_clue_forest"), true, true, false));
            rejects(encounter, "HYBRID");
            rejects(encounter, "MINECRAFT_COMBAT");
            rejects(encounter, null);
        }
        check(!StoryCombatPolicy.protectTrainer(Set.of(), true, true, false));
        check(!StoryCombatPolicy.protectTrainer(Set.of("pride_battle_forged"),
            true, true, false));
        check(!StoryCombatPolicy.protectTrainer(Set.of("pride_npc_sable"),
            true, true, false));
        check(!StoryCombatPolicy.protectTrainer(Set.of("flashpoint_boss"),
            true, true, false));
        check(!StoryCombatPolicy.protectTrainer(Set.of("operation_boss"),
            true, true, false));
        check(!StoryCombatPolicy.protectTrainer(Set.of("worldboss_boss"),
            true, true, false));
        check(!StoryCombatPolicy.protectTrainer(null, true, true, false));
        rejects("unknown", "POKEMON_ONLY");
        System.out.println("PASS: " + checks + " strict story-vs-hybrid combat policy checks");
    }
}
