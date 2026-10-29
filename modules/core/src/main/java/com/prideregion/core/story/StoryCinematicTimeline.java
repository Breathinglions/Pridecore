package com.prideregion.core.story;

import java.util.*;

/**
 * Bounded, server-tick presentation timeline for authored story battles.
 * No timers, client packets, damage, teleportation or battle-result mutations.
 * The Fabric adapter decides how to display each authenticated cue.
 */
public final class StoryCinematicTimeline {
    public record Cue(int tick, String title, String effectKey, int particles) {
        public Cue {
            if (tick < 0 || title == null || title.isBlank()
                || effectKey == null || particles < 0 || particles > 48)
                throw new IllegalArgumentException("Invalid cinematic cue");
        }
    }
    public record Event(UUID player, String encounter, Cue cue) {}
    private record Scene(String encounter, int elapsed, List<Cue> cues) {}
    private static final int MAX_ACTIVE_SCENES = 64;
    private final Map<UUID, Scene> active = new HashMap<>();

    private static final List<Cue> RIN = List.of(
        new Cue(0, "RIN  |  YOUR RIVAL", "battle.rin", 25),
        new Cue(25, "A rival's challenge!", "battle.rin", 16),
        new Cue(55, "Make every move count.", "battle.enchant", 12));
    private static final List<Cue> SABLE = List.of(
        new Cue(0, "SABLE  |  ECLIPSE COMMANDER", "battle.sable", 36),
        new Cue(22, "The shadows gather...", "battle.electric", 28),
        new Cue(50, "The Eclipse is watching.", "battle.sable", 20));
    private static final List<Cue> CAEL = List.of(
        new Cue(0, "DIRECTOR CAEL  |  MERIDIAN", "battle.cael", 38),
        new Cue(22, "Resonance levels are rising.", "battle.electric", 30),
        new Cue(50, "The engine cannot contain this.", "battle.cael", 22));
    private static final List<Cue> SHIELD = List.of(
        new Cue(0, "RESONANCE ENGINE  |  SHIELD DEFENDER", "finale.shield", 34),
        new Cue(25, "A defensive field activates.", "battle.electric", 24),
        new Cue(55, "Break through the shield!", "finale.shield", 16));
    private static final List<Cue> CONDUITS_COMPLETE = List.of(
        new Cue(0, "RESONANCE ENGINE  |  CONDUITS ONLINE", "finale.conduit", 42),
        new Cue(24, "Three defensive shields materialize.", "finale.shield", 32),
        new Cue(55, "Defeat the defenders to reach the core.", "finale.end_rod", 24));
    private static final List<Cue> SHIELDS_COMPLETE = List.of(
        new Cue(0, "RESONANCE ENGINE  |  SHIELDS OFFLINE", "finale.shield", 40),
        new Cue(24, "The core is exposed.", "finale.end_rod", 30),
        new Cue(55, "Stabilize the engine before it overloads!", "finale.totem", 25));
    private static final List<Cue> ENGINE_STABILIZED = List.of(
        new Cue(0, "RESONANCE ENGINE  |  STABILIZED", "finale.totem", 45),
        new Cue(28, "The crisis has been contained.", "finale.end_rod", 28),
        new Cue(65, "The path forward is yours.", "finale.totem", 18));
    private static final List<Cue> LIEUTENANT = List.of(
        new Cue(0, "ECLIPSE  |  LIEUTENANT", "battle.electric", 26),
        new Cue(35, "The operation is compromised!", "battle.enchant", 14));

    public static List<Cue> cuesFor(String encounter) {
        if (encounter == null) return List.of();
        if (encounter.startsWith("rin_") &&
            StoryBattleSessionRouter.acceptedTrainers().containsKey(encounter)) return RIN;
        if (encounter.startsWith("shield_") &&
            StoryBattleSessionRouter.acceptedTrainers().containsKey(encounter)) return SHIELD;
        return switch (encounter) {
            case "sable" -> SABLE;
            case "director_cael" -> CAEL;
            case "eclipse_lieutenant" -> LIEUTENANT;
            case "finale_conduits_complete" -> CONDUITS_COMPLETE;
            case "finale_shields_complete" -> SHIELDS_COMPLETE;
            case "finale_stabilized" -> ENGINE_STABILIZED;
            default -> List.of();
        };
    }

    /** Called only AFTER the RCT launch has been verified by the ledger. */
    public synchronized boolean begin(UUID player, String encounter) {
        Objects.requireNonNull(player);
        List<Cue> cues = cuesFor(encounter);
        if (cues.isEmpty() || active.containsKey(player)
            || active.size() >= MAX_ACTIVE_SCENES) return false;
        active.put(player, new Scene(encounter, 0, cues));
        return true;
    }

    /** Run exactly once per server tick. No scene survives past its final cue. */
    public synchronized List<Event> tick() {
        List<Event> emitted = new ArrayList<>();
        Iterator<Map.Entry<UUID, Scene>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Scene scene = entry.getValue();
            for (Cue cue : scene.cues()) {
                if (cue.tick() == scene.elapsed())
                    emitted.add(new Event(entry.getKey(), scene.encounter(), cue));
            }
            if (scene.elapsed() >= scene.cues().getLast().tick()) iterator.remove();
            else entry.setValue(new Scene(scene.encounter(), scene.elapsed() + 1, scene.cues()));
        }
        return List.copyOf(emitted);
    }

    public synchronized void cancel(UUID player) { active.remove(player); }
    public synchronized void clear() { active.clear(); }
    public synchronized int activeCount() { return active.size(); }
}
