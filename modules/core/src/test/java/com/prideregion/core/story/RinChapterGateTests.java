package com.prideregion.core.story;

import java.util.*;

public final class RinChapterGateTests {
    private static void check(boolean b) {
        if (!b) throw new AssertionError("Invalid Rin chapter visibility");
    }
    private static Set<String> facts(String... values) { return Set.of(values); }
    private static Set<String> badges(int n) {
        var out = new HashSet<String>();
        int i = 0;
        for (String tag : List.of("league_grass","league_rock","league_fire",
                "league_water","league_electric","league_dragon","league_ice",
                "league_ghost")) {
            if (i++ >= n) break;
            out.add(tag);
        }
        return out;
    }
    private static void scene(RinChapterGate.Scene expected, int badges, String... facts) {
        check(RinChapterGate.choose(facts(facts), badges(badges)) == expected);
    }
    public static void main(String[] args) {
        scene(RinChapterGate.Scene.RETURN, 0);
        scene(RinChapterGate.Scene.RETURN, 8, "pride:met_rin");
        scene(RinChapterGate.Scene.ROUTE1, 0, "pride:met_rin", "pride:met_avery");
        // Veteran badges do not bypass the still-incomplete Route 1 chapter.
        scene(RinChapterGate.Scene.ROUTE1, 8, "pride:met_rin", "pride:met_avery",
            "pride:eclipse_contact", "pride:frozen_warning");
        scene(RinChapterGate.Scene.RETURN, 8, "pride:met_rin", "pride:met_avery",
            "pride:rin_route1_won");
        // Story progress with one badge must never reveal Canyon.
        scene(RinChapterGate.Scene.RETURN, 1,
            "pride:rin_route1_won", "pride:eclipse_contact");
        scene(RinChapterGate.Scene.CANYON, 2,
            "pride:rin_route1_won", "pride:eclipse_contact");
        scene(RinChapterGate.Scene.CANYON, 8,
            "pride:rin_route1_won", "pride:eclipse_contact",
            "pride:frozen_warning");
        // Summit needs both a genuine earlier Canyon victory and 7 badges.
        scene(RinChapterGate.Scene.RETURN, 7,
            "pride:rin_route1_won", "pride:rin_canyon_won",
            "pride:eclipse_contact");
        scene(RinChapterGate.Scene.RETURN, 6,
            "pride:rin_route1_won", "pride:rin_canyon_won",
            "pride:frozen_warning");
        scene(RinChapterGate.Scene.SUMMIT, 7,
            "pride:rin_route1_won", "pride:rin_canyon_won",
            "pride:frozen_warning");
        scene(RinChapterGate.Scene.RETURN, 8,
            "pride:rin_route1_won", "pride:rin_canyon_won",
            "pride:frozen_warning", "pride:rin_summit_won");
        // An irrelevant tag does not count as an official Gym badge.
        check(RinChapterGate.choose(facts("pride:rin_route1_won",
            "pride:eclipse_contact"), Set.of("league_started","league_champion"))
            == RinChapterGate.Scene.RETURN);
        check(StoryTrainerBindingGate.permitsCanonicalRin("rin_route1",
            Set.of("pride_npc_rin"),true,false,9));
        check(!StoryTrainerBindingGate.permitsCanonicalRin("sable",
            Set.of("pride_npc_rin"),true,false,9));
        check(!StoryTrainerBindingGate.permitsCanonicalRin("rin_route1",
            Set.of("pride_npc_rin","pride_battle_rin_route1"),true,false,9));
        check(!StoryTrainerBindingGate.permitsCanonicalRin("rin_route1",
            Set.of("pride_npc_rin"),true,false,49));
        System.out.println("PASS: per-player narrative+badge Rin routing and single NPC battle authority");
    }
}
