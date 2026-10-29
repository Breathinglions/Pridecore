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
