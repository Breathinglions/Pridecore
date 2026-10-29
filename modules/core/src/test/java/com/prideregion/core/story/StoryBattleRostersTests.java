package com.prideregion.core.story;

import java.io.StringReader;

public final class StoryBattleRostersTests {
    private static void check(boolean valid) {
        if (!valid) throw new AssertionError("Story battle roster check failed");
    }
    private static void rejected(String data) {
        try { StoryBattleRosters.parse(new StringReader(data)); }
        catch (RuntimeException expected) { return; }
        throw new AssertionError("Invalid roster was accepted");
    }
    public static void main(String[] args) throws Exception {
        var all = StoryBattleRosters.bundled();
        check(all.all().size() == 9);
        check(all.find("rin_route1").orElseThrow().team().size() == 2);
        check(all.find("director_cael").orElseThrow().team().size() == 6);
        check(all.find("shield_gamma").orElseThrow().team().get(1).level() == 64);
        check(all.find("unknown").isEmpty());
        String invalid = "{\"schema\":2,\"mode\":\"STAGING_ONLY\",\"encounters\":[]}";
        rejected(invalid);
        // The authored team file is the only combat authority for Module 02.
        // Hybrid damage fields and alternate formats must fail closed.
        String authored;
        try (var in = StoryBattleRosters.class.getResourceAsStream(
                "/story/story_battles.json")) {
            if (in == null) throw new AssertionError("Missing bundled story roster");
            authored = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        check(authored.contains("\"combatMode\": \"POKEMON_ONLY\""));
        check(authored.contains("\"battleFormat\": \"GEN_9_SINGLES\""));
        rejected(authored.replace("\"POKEMON_ONLY\"", "\"HYBRID\""));
        rejected(authored.replace("\"GEN_9_SINGLES\"", "\"MINECRAFT_HYBRID\""));
        rejected(authored.replace("\"combatMode\"", "\"minecraftDamage\": 5, \"combatMode\""));
        rejected(authored.replace("\"chapter\"", "\"mobWave\": true, \"chapter\""));
        rejected(authored.replace("\"species\"", "\"meleeAttack\": 10, \"species\""));
        rejected(authored.replace("\"team\"", "\"minecraftBossPhase\": true, \"team\""));
        System.out.println("PASS: story battle roster loading and schema checks");
    }
}
