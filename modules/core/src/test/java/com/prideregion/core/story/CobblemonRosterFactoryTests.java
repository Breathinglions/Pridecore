package com.prideregion.core.story;

public final class CobblemonRosterFactoryTests {
    public static void main(String[] args) throws Exception {
        var roster = StoryBattleRosters.bundled().find("rin_route1").orElseThrow();
        try {
            CobblemonRosterFactory.create(roster, CobblemonRosterFactoryTests.class.getClassLoader());
            throw new AssertionError("Cobblemon is absent in standalone tests; fail closed");
        } catch (ClassNotFoundException expected) {
            System.out.println("PASS: native Pokémon factory fails closed without Cobblemon dependency");
        }
    }
}
