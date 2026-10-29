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
        System.out.println("PASS: story battle roster loading and schema checks");
    }
}
