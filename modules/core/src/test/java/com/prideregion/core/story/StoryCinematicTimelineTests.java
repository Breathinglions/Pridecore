package com.prideregion.core.story;

import java.util.*;

public final class StoryCinematicTimelineTests {
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("Cinematic timeline invariant " + checks);
    }
    public static void main(String[] args) {
        StoryCinematicTimeline timeline = new StoryCinematicTimeline();
        for (String encounter : StoryBattleSessionRouter.acceptedTrainers().keySet()) {
            var cues = StoryCinematicTimeline.cuesFor(encounter);
            check(!cues.isEmpty());
            check(cues.getFirst().tick() == 0);
            for (int i = 1; i < cues.size(); i++)
                check(cues.get(i).tick() > cues.get(i-1).tick());
            UUID player = UUID.randomUUID();
            check(timeline.begin(player, encounter));
            check(!timeline.begin(player, encounter));
            var events = new ArrayList<StoryCinematicTimeline.Event>();
            for (int i = 0; i < 120; i++) events.addAll(timeline.tick());
            check(events.size() == cues.size());
            for (int i = 0; i < cues.size(); i++) {
                check(events.get(i).player().equals(player));
                check(events.get(i).encounter().equals(encounter));
                check(events.get(i).cue().equals(cues.get(i)));
            }
            check(timeline.activeCount() == 0);
        }
        for (String phase : List.of("finale_conduits_complete",
                                   "finale_shields_complete", "finale_stabilized")) {
            var cues = StoryCinematicTimeline.cuesFor(phase);
            check(cues.size() == 3);
            UUID player = UUID.randomUUID();
            check(timeline.begin(player, phase));
            List<StoryCinematicTimeline.Event> events = new ArrayList<>();
            for (int i = 0; i < 90; i++) events.addAll(timeline.tick());
            check(events.size() == 3);
            check(events.stream().allMatch(e -> e.player().equals(player)
                && e.encounter().equals(phase)));
            check(timeline.activeCount() == 0);
        }
        check(StoryCinematicTimeline.cuesFor("battle_phase_one").isEmpty());
        check(StoryCinematicTimeline.cuesFor("battle_phase_two").isEmpty());
        // Every authenticated encounter has its own two narrative beats per
        // phase, with no untrusted ID accepted as a scene name.
        for (String encounter : StoryBattleSessionRouter.acceptedTrainers().keySet()) {
            for (int phase : List.of(1, 2)) {
                String name = StoryCinematicTimeline.phaseScene(encounter, phase);
                check(name != null);
                var authored = StoryCinematicTimeline.cuesFor(name);
                check(authored.size() == 2);
                check(authored.getFirst().title().contains(
                    encounter.startsWith("rin_") ? "RIN"
                    : encounter.startsWith("shield_") ? "SHIELD"
                    : encounter.equals("director_cael") ? "CAEL"
                    : encounter.equals("eclipse_lieutenant") ? "ECLIPSE" : "SABLE"));
                UUID actor = UUID.randomUUID();
                check(timeline.begin(actor, name));
                var first = timeline.tick();
                check(first.size() == 1 && first.getFirst().cue().tick() == 0);
                timeline.cancel(actor);
            }
        }
        check(StoryCinematicTimeline.phaseScene("forged", 1) == null);
        check(StoryCinematicTimeline.phaseScene("sable", 3) == null);
        check(StoryCinematicTimeline.cuesFor("bossphase_forged_p1").isEmpty());
        check(StoryCinematicTimeline.cuesFor("bossphase_sable_p99").isEmpty());
        check(StoryCinematicTimeline.cuesFor("invalid").isEmpty());
        check(!timeline.begin(UUID.randomUUID(), "invalid"));
        UUID leaving = UUID.randomUUID();
        check(timeline.begin(leaving, "sable"));
        timeline.cancel(leaving);
        check(timeline.tick().isEmpty());
        check(timeline.activeCount() == 0);
        check(timeline.begin(leaving, "director_cael"));
        timeline.clear();
        check(timeline.tick().isEmpty());
        // Hard cap on concurrent visual timelines protects crowded servers.
        for (int i = 0; i < 64; i++)
            check(timeline.begin(UUID.randomUUID(), "sable"));
        check(!timeline.begin(UUID.randomUUID(), "sable"));
        timeline.clear();
        System.out.println("PASS: " + checks + " cinematic timeline assertions");
    }
}
