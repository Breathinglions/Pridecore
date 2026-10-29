package com.prideregion.core.story;

import java.util.*;

/**
 * Per-player, in-memory Resonance Engine encounter phases.
 * This controller cannot grant story facts or League rewards. It requires
 * independent, trusted event validation for every phase advancement.
 * Incomplete sessions reset safely after restart/disconnect.
 */
public final class ResonanceFinaleController {
    public enum Phase { LOCKED, CONDUITS, SHIELDS, STABILIZE, COMPLETE }
    public record Snapshot(UUID player, Phase phase, int conduits, int shields) {}
    private record State(Phase phase, Set<String> conduits, Set<String> shields) {}
    private final Map<UUID, State> sessions = new HashMap<>();
    private static final Set<String> CONDUITS = Set.of("north", "south", "east", "west");
    private static final Set<String> SHIELDS = Set.of("alpha", "beta", "gamma");

    public synchronized Snapshot start(UUID player, boolean verifiedCaelVictory,
                                        boolean verifiedEngineEvidence) {
        Objects.requireNonNull(player);
        if (!verifiedCaelVictory || !verifiedEngineEvidence)
            return snapshot(player);
        sessions.putIfAbsent(player, new State(Phase.CONDUITS, Set.of(), Set.of()));
        return snapshot(player);
    }

    /** World interaction adapter must validate matching marker and nearby player. */
    public synchronized Snapshot conduit(UUID player, String name) {
        State state = require(player, Phase.CONDUITS);
        if (!CONDUITS.contains(name)) throw new IllegalArgumentException("Unknown conduit");
        Set<String> updated = new HashSet<>(state.conduits());
        updated.add(name);
        Phase next = updated.equals(CONDUITS) ? Phase.SHIELDS : Phase.CONDUITS;
        sessions.put(player, new State(next, Set.copyOf(updated), Set.of()));
        return snapshot(player);
    }

    /** Only a verified shield-defender defeat may call this transition. */
    public synchronized Snapshot shieldVictory(UUID player, String shield) {
        State state = require(player, Phase.SHIELDS);
        if (!SHIELDS.contains(shield)) throw new IllegalArgumentException("Unknown shield");
        Set<String> updated = new HashSet<>(state.shields());
        updated.add(shield);
        Phase next = updated.equals(SHIELDS) ? Phase.STABILIZE : Phase.SHIELDS;
        sessions.put(player, new State(next, state.conduits(), Set.copyOf(updated)));
        return snapshot(player);
    }

    /** The engine core interaction must separately verify its world marker. */
    public synchronized Snapshot stabilize(UUID player) {
        State state = require(player, Phase.STABILIZE);
        sessions.put(player, new State(Phase.COMPLETE, state.conduits(), state.shields()));
        return snapshot(player);
    }

    public synchronized Snapshot snapshot(UUID player) {
        State s = sessions.get(player);
        return new Snapshot(player, s == null ? Phase.LOCKED : s.phase(),
            s == null ? 0 : s.conduits().size(), s == null ? 0 : s.shields().size());
    }

    public synchronized void disconnect(UUID player) { sessions.remove(player); }
    public synchronized void clear() { sessions.clear(); }

    private State require(UUID player, Phase phase) {
        State state = sessions.get(player);
        if (state == null || state.phase() != phase)
            throw new IllegalStateException("Resonance finale phase is not active");
        return state;
    }
}
