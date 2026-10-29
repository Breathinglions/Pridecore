package com.prideregion.core.story;

import java.util.*;

/**
 * Server-owned, per-viewer story scene state. This is deliberately independent
 * of Minecraft entities and transport: adapters MUST send each event solely
 * to its owner, and MUST NOT spawn a shared persistent Rin NPC.
 *
 * It cannot award a starter, fact, badge, battle result, item, or currency.
 * Integrations must validate real native service eligibility before begin().
 * The OPEN_NATIVE_STARTER cue only requests the existing native GUI action.
 */
public final class StoryPrivateSceneDirector {
    public static final int MAX_ACTIVE = 64;
    public static final int MAX_TICKS = 200;

    public enum SceneId { RIN_LAB_INTRO }
    public enum Kind {
        DIALOGUE, ACTOR_ENTER, ACTOR_MOVE, ACTOR_FACE,
        ACTOR_REMOVE, OPEN_NATIVE_STARTER
    }
    public record Event(UUID owner, UUID session, SceneId scene, Kind kind,
                        String speaker, String text, String anchor) {
        public Event {
            Objects.requireNonNull(owner);
            Objects.requireNonNull(session);
            Objects.requireNonNull(scene);
            Objects.requireNonNull(kind);
            Objects.requireNonNull(speaker);
            Objects.requireNonNull(text);
            Objects.requireNonNull(anchor);
        }
    }
    public record Receipt(UUID owner, UUID session, SceneId scene) {}
    public record Start(boolean started, UUID session) {}

    private record Cue(int atTick, Kind kind, String speaker, String text, String anchor) {}
    private record Running(UUID session, SceneId scene, String dimension, int elapsed) {}
    private final Map<UUID, Running> active = new LinkedHashMap<>();

    private static final List<Cue> LAB = List.of(
        new Cue(0, Kind.DIALOGUE, "Professor Sequoia",
            "Before we begin, there's someone I'd like you to meet.", ""),
        new Cue(18, Kind.ACTOR_ENTER, "Rin", "", "lab_entry"),
        new Cue(38, Kind.ACTOR_MOVE, "Rin", "", "sequoia_desk"),
        new Cue(52, Kind.ACTOR_FACE, "Rin", "", "player"),
        new Cue(53, Kind.DIALOGUE, "Rin",
            "Oh! You're choosing your first Pokémon today too? I'm Rin!", ""),
        new Cue(88, Kind.DIALOGUE, "Rin",
            "I'm going to be Champion someday. Guess we'll see who gets there first!", ""),
        new Cue(125, Kind.DIALOGUE, "Professor Sequoia",
            "Competition is healthy when you look after your partners. Now, both of you—choose wisely.", ""),
        new Cue(160, Kind.ACTOR_REMOVE, "Rin", "", ""),
        new Cue(160, Kind.OPEN_NATIVE_STARTER, "Professor Sequoia", "", "")
    );

    private static List<Cue> cues(SceneId scene) {
        return switch (scene) { case RIN_LAB_INTRO -> LAB; };
    }

    /** Duplicate clicks cannot restart a scene or allocate a second actor. */
    public synchronized Start begin(UUID player, String dimension, SceneId scene) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(scene);
        if (dimension == null || dimension.isBlank())
            throw new IllegalArgumentException("Scene dimension required");
        Running old = active.get(player);
        if (old != null) return new Start(false, old.session());
        if (active.size() >= MAX_ACTIVE) return new Start(false, null);
        UUID session = UUID.randomUUID();
        active.put(player, new Running(session, scene, dimension, 0));
        return new Start(true, session);
    }

    /**
     * Exactly one tick per server tick. Events contain an explicit owner and
     * session token; the renderer must never broadcast them to nearby players.
     */
    public synchronized List<Event> tick() {
        List<Event> result = new ArrayList<>();
        var iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            UUID owner = entry.getKey();
            Running run = entry.getValue();
            List<Cue> sequence = cues(run.scene());
            for (Cue cue : sequence) {
                if (cue.atTick() == run.elapsed())
                    result.add(new Event(owner, run.session(), run.scene(),
                        cue.kind(), cue.speaker(), cue.text(), cue.anchor()));
            }
            if (run.elapsed() >= MAX_TICKS ||
                    run.elapsed() >= sequence.getLast().atTick()) {
                iterator.remove();
            } else {
                entry.setValue(new Running(run.session(), run.scene(),
                    run.dimension(), run.elapsed() + 1));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Accessible/non-animated fallback. Dialogue remains intact, then exactly
     * one native UI continuation is requested; no story progress is granted.
     */
    public synchronized List<Event> skip(UUID player, UUID session) {
        Running run = active.get(player);
        if (run == null || !run.session().equals(session)) return List.of();
        active.remove(player);
        List<Event> result = new ArrayList<>();
        for (Cue cue : cues(run.scene())) {
            if (cue.kind() == Kind.DIALOGUE || cue.kind() == Kind.OPEN_NATIVE_STARTER
                    || cue.kind() == Kind.ACTOR_REMOVE)
                result.add(new Event(player, run.session(), run.scene(),
                    cue.kind(), cue.speaker(), cue.text(), cue.anchor()));
        }
        return List.copyOf(result);
    }

    /** Cancellations remove private projections WITHOUT reopening the GUI. */
    public synchronized Optional<Event> cancel(UUID player) {
        Running run = active.remove(player);
        return run == null ? Optional.empty() : Optional.of(
            new Event(player, run.session(), run.scene(),
                Kind.ACTOR_REMOVE, "Rin", "", ""));
    }

    /** Dimension changes invalidate the scene; no cross-world actor leak. */
    public synchronized Optional<Event> checkDimension(UUID player, String dimension) {
        Running run = active.get(player);
        if (run == null || Objects.equals(run.dimension(), dimension))
            return Optional.empty();
        return cancel(player);
    }

    /** Shutdown must inform any connected private-actor renderer of cleanup. */
    public synchronized List<Event> clear() {
        List<Event> cleanup = new ArrayList<>();
        for (UUID owner : List.copyOf(active.keySet())) cancel(owner).ifPresent(cleanup::add);
        return List.copyOf(cleanup);
    }

    public synchronized Optional<Receipt> active(UUID player) {
        Running run = active.get(player);
        return run == null ? Optional.empty()
            : Optional.of(new Receipt(player, run.session(), run.scene()));
    }
    public synchronized int size() { return active.size(); }
}
