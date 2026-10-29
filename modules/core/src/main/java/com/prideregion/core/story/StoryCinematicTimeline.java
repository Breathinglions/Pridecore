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
        new Cue(0, "RIN  |  A FAMILIAR FACE", "battle.rin", 25),
        new Cue(25, "We have come a long way, haven't we?", "battle.rin", 16),
        new Cue(55, "There is more ahead of us than the League.", "battle.enchant", 12));
    private static final List<Cue> SABLE = List.of(
        new Cue(0, "SABLE  |  THE TRUTH BENEATH ECLIPSE", "battle.sable", 36),
        new Cue(22, "The shadows gather...", "battle.electric", 28),
        new Cue(50, "Meridian cannot bury the truth forever.", "battle.sable", 20));
    private static final List<Cue> CAEL = List.of(
        new Cue(0, "DIRECTOR CAEL  |  THE COST OF RESONANCE", "battle.cael", 38),
        new Cue(22, "Resonance levels are rising.", "battle.electric", 30),
        new Cue(50, "I thought I could protect everything.", "battle.cael", 22));
    private static final List<Cue> SHIELD = List.of(
        new Cue(0, "RESONANCE ENGINE  |  THE ANCIENT SEAL", "finale.shield", 34),
        new Cue(25, "An ancient light spills across the chamber.", "battle.electric", 24),
        new Cue(55, "The sealed heart beats beneath the stone.", "finale.shield", 16));
    private static final List<Cue> CONDUITS_COMPLETE = List.of(
        new Cue(0, "RESONANCE ENGINE  |  AN AWAKENING", "finale.conduit", 42),
        new Cue(24, "Three ancient seals emerge from the light.", "finale.shield", 32),
        new Cue(55, "Beyond the seals, something is calling.", "finale.end_rod", 24));
    private static final List<Cue> SHIELDS_COMPLETE = List.of(
        new Cue(0, "RESONANCE ENGINE  |  THE LAST SEAL FADES", "finale.shield", 40),
        new Cue(24, "The chamber has fallen strangely quiet.", "finale.end_rod", 30),
        new Cue(55, "A heartbeat of light remains at the center.", "finale.totem", 25));
    private static final List<Cue> ENGINE_STABILIZED = List.of(
        new Cue(0, "RESONANCE ENGINE  |  STABILIZED", "finale.totem", 45),
        new Cue(28, "The crisis has been contained.", "finale.end_rod", 28),
        new Cue(65, "The path forward is yours.", "finale.totem", 18));
    private static final List<Cue> LIEUTENANT = List.of(
        new Cue(0, "ECLIPSE  |  THE LAST WARNING", "battle.electric", 26),
        new Cue(35, "Everything we believed is coming apart.", "battle.enchant", 14));

    /**
     * Authored boss-specific presentation on an authenticated opponent KO.
     * Combat state stays entirely in Cobblemon/RCT.
     */
    public static String phaseScene(String encounter, int phase) {
        if (!StoryBattleSessionRouter.acceptedTrainers().containsKey(encounter)
                || (phase != 1 && phase != 2)) return null;
        return "bossphase_" + encounter + "_p" + phase;
    }

    private static List<Cue> phaseCues(String encounter, int phase) {
        String title;
        String warning;
        String effect;
        switch (encounter) {
            case "rin_route1", "rin_canyon", "rin_summit" -> {
                title = phase == 1 ? "RIN  |  AN OLD PROMISE" : "RIN  |  THE ROAD CONTINUES";
                warning = phase == 1 ? "You're stronger than I expected!"
                    : "I still believe in the journey we began!";
                effect = "battle.rin";
            }
            case "eclipse_lieutenant" -> {
                title = phase == 1 ? "ECLIPSE  |  DOUBT"
                    : "ECLIPSE  |  A CHOICE";
                warning = phase == 1 ? "What if they were right about Meridian?"
                    : "Sable deserves to know what we found.";
                effect = "battle.electric";
            }
            case "sable" -> {
                title = phase == 1 ? "SABLE  |  THE BURIED REPORTS"
                    : "SABLE  |  ANOTHER WAY";
                warning = phase == 1 ? "You have no idea what follows."
                    : "If there is another way, show me the truth.";
                effect = "battle.sable";
            }
            case "director_cael" -> {
                title = phase == 1 ? "CAEL  |  A FRACTURED VISION"
                    : "CAEL  |  THE LAST WARNING";
                warning = phase == 1 ? "The readings... they were wrong all along."
                    : "I cannot let the region pay for my mistake.";
                effect = "battle.cael";
            }
            case "shield_alpha", "shield_beta", "shield_gamma" -> {
                String shield = encounter.substring("shield_".length()).toUpperCase(Locale.ROOT);
                title = phase == 1 ? "SHIELD " + shield + "  |  AN ECHO STIRS"
                    : "SHIELD " + shield + "  |  A FADING LIGHT";
                warning = phase == 1 ? "The seal remembers those who came before."
                    : "The old light is beginning to fade.";
                effect = "finale.shield";
            }
            default -> { return List.of(); }
        }
        return List.of(new Cue(0, title, effect, phase == 1 ? 32 : 40),
            new Cue(24, warning, "battle.electric", phase == 1 ? 18 : 24));
    }

    public static List<Cue> cuesFor(String encounter) {
        if (encounter == null) return List.of();
        if (encounter.startsWith("bossphase_")) {
            String raw = encounter.substring("bossphase_".length());
            int cut = raw.lastIndexOf("_p");
            if (cut > 0) {
                String id = raw.substring(0, cut);
                String phase = raw.substring(cut + 2);
                if (phase.equals("1") || phase.equals("2")) {
                    String expected = phaseScene(id, Integer.parseInt(phase));
                    if (encounter.equals(expected)) return phaseCues(id, Integer.parseInt(phase));
                }
            }
            return List.of();
        }
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
        StoryPresentationConfig presentation = StoryPresentationConfig.current();
        if (!presentation.enabled()) return false;
        List<Cue> scaled = cues.stream()
            .map(c -> new Cue(presentation.tick(c.tick()), c.title(),
                c.effectKey(), c.particles())).toList();
        active.put(player, new Scene(encounter, 0, scaled));
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
