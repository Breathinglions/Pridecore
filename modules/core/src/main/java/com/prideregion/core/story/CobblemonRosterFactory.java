package com.prideregion.core.story;

import java.lang.reflect.*;
import java.util.*;

/**
 * Exact Cobblemon 1.8.1 binary boundary.
 * No compile-time Cobblemon dependency is required by the foundation module.
 * Called only from server-side RCT adapter code, never from a client packet.
 */
public final class CobblemonRosterFactory {
    private CobblemonRosterFactory() {}

    public static Object[] create(StoryBattleRosters.Roster roster, ClassLoader loader)
            throws ReflectiveOperationException {
        Objects.requireNonNull(roster);
        Objects.requireNonNull(loader);
        Class<?> propertiesType = Class.forName(
            "com.cobblemon.mod.common.api.pokemon.PokemonProperties", true, loader);
        Class<?> pokemonType = Class.forName(
            "com.cobblemon.mod.common.pokemon.Pokemon", true, loader);
        Object companion = propertiesType.getField("Companion").get(null);
        Method parse = companion.getClass().getMethod("parse", String.class);
        Method create = propertiesType.getMethod("create");
        Object array = Array.newInstance(pokemonType, roster.team().size());
        for (int i = 0; i < roster.team().size(); i++) {
            StoryBattleRosters.Member member = roster.team().get(i);
            if (!member.species().matches("[a-z0-9_-]+")
                    || member.level() < 1 || member.level() > 100)
                throw new IllegalArgumentException("Invalid authored roster member");
            // Cobblemon's Pokémon property language supports species and level.
            Object properties = parse.invoke(companion,
                member.species() + " level=" + member.level());
            Object pokemon = create.invoke(properties);
            if (!pokemonType.isInstance(pokemon))
                throw new IllegalStateException("Cobblemon returned invalid Pokémon instance");
            Array.set(array, i, pokemon);
        }
        // Object[] is covariant: runtime component type remains Pokemon.
        return (Object[]) array;
    }
}
