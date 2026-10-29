package com.prideregion.core.story;

import java.util.*;

public final class StoryPokemonOnlyPartiesTests {
    private static int checks;
    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("RCT Pokémon-only party check " + checks);
    }
    public static void main(String[] args) {
        Object player = new Object(), npc = new Object(), second = new Object();
        check(StoryPokemonOnlyParties.valid(List.of(player), List.of(npc), player, npc));
        check(!StoryPokemonOnlyParties.valid(List.of(), List.of(npc), player, npc));
        check(!StoryPokemonOnlyParties.valid(List.of(player), List.of(), player, npc));
        check(!StoryPokemonOnlyParties.valid(
            List.of(player, second), List.of(npc), player, npc));
        check(!StoryPokemonOnlyParties.valid(
            List.of(player), List.of(npc, second), player, npc));
        check(!StoryPokemonOnlyParties.valid(List.of(npc), List.of(player), player, npc));
        check(!StoryPokemonOnlyParties.valid(List.of(player), List.of(player), player, npc));
        check(!StoryPokemonOnlyParties.valid(List.of(npc), List.of(npc), player, npc));
        check(!StoryPokemonOnlyParties.valid(List.of(player), List.of(npc), player, player));
        check(!StoryPokemonOnlyParties.valid(null, List.of(npc), player, npc));
        check(!StoryPokemonOnlyParties.valid(List.of(player), null, player, npc));
        check(!StoryPokemonOnlyParties.valid(List.of(player), List.of(npc), null, npc));
        check(!StoryPokemonOnlyParties.valid(List.of(player), List.of(npc), player, null));
        System.out.println("PASS: " + checks + " Pokémon-only RCT 1v1 party checks");
    }
}
