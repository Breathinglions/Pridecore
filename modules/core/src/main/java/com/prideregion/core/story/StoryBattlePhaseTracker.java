package com.prideregion.core.story;

import java.util.*;

/**
 * Optional mid-battle presentation triggers driven by verified Cobblemon
 * opponent knockouts. This does not change the RCT battle or award progress.
 */
public final class StoryBattlePhaseTracker {
    public record Beat(UUID player, String encounter, int defeated, int total, int phase) {}
    private static final class State {
        final UUID player;
        final String encounter;
        final int total;
        final Set<UUID> knockedOut = new HashSet<>();
        State(UUID player, String encounter, int total) {
            this.player = player; this.encounter = encounter; this.total = total;
        }
    }
    private static final int MAX_BATTLES = 96;
    private final Map<UUID, State> battles = new HashMap<>();

    public synchronized boolean register(UUID battle, UUID player, String encounter, int total) {
        Objects.requireNonNull(battle);
        Objects.requireNonNull(player);
        if (!StoryBattleSessionRouter.acceptedTrainers().containsKey(encounter)
                || total < 2 || total > 6 || battles.size() >= MAX_BATTLES
                || battles.containsKey(battle)) return false;
        battles.put(battle, new State(player, encounter, total));
        return true;
    }

    /**
     * Deduplicate faint IDs. Never announce the last Pokémon fainting as
     * a new combat phase because the battle victory callback is imminent.
     */
    public synchronized Optional<Beat> opponentFainted(UUID battle, UUID pokemon) {
        if (pokemon == null) return Optional.empty();
        State state = battles.get(battle);
        if (state == null || !state.knockedOut.add(pokemon)) return Optional.empty();
        int count = state.knockedOut.size();
        if (count >= state.total) return Optional.empty();
        int secondAt = Math.max(2, state.total - 2);
        int phase = count == 1 ? 1 : count == secondAt && secondAt > 1 ? 2 : 0;
        return phase == 0 ? Optional.empty()
            : Optional.of(new Beat(state.player, state.encounter, count, state.total, phase));
    }

    public synchronized void cancelPlayer(UUID player) {
        if (player != null)
            battles.entrySet().removeIf(entry -> entry.getValue().player.equals(player));
    }

    public synchronized void finish(UUID battle) { battles.remove(battle); }
    public synchronized void clear() { battles.clear(); }
    public synchronized int activeCount() { return battles.size(); }
}
