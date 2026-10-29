package com.prideregion.core.story;

import java.util.*;

/** RCT trainer-vs-trainer singleton party contract for Module 02 stories. */
public final class StoryPokemonOnlyParties {
    private StoryPokemonOnlyParties() {}

    public static boolean valid(List<?> playerSide, List<?> npcSide,
                                Object expectedPlayer, Object expectedNpc) {
        return playerSide != null && npcSide != null
            && expectedPlayer != null && expectedNpc != null
            && !Objects.equals(expectedPlayer, expectedNpc)
            && playerSide.size() == 1 && npcSide.size() == 1
            && Objects.equals(playerSide.getFirst(), expectedPlayer)
            && Objects.equals(npcSide.getFirst(), expectedNpc);
    }
}
