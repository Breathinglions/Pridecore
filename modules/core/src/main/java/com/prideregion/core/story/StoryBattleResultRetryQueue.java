package com.prideregion.core.story;

import java.util.*;

/**
 * Bounded server-tick retries for a verified RCT victory whose persistent
 * profile write failed. This only remembers results already authenticated by
 * the existing launch ledger and never creates or awards a victory by itself.
 *
 * Volatile state: server restarts still require a verified recovery procedure.
 */
public final class StoryBattleResultRetryQueue {
    public static final int RETRY_TICKS = 20;
    public static final int MAX_RETRIES = 5;
    public static final int MAX_PENDING = 64;

    public record Retry(UUID battle, UUID winner, int attempt) {}
    public record Batch(List<Retry> due, List<Retry> expired) {}

    private static final class Pending {
        final UUID winner;
        int ticks = RETRY_TICKS;
        int attempts;
        Pending(UUID winner) { this.winner = winner; }
    }
    private final Map<UUID, Pending> pending = new LinkedHashMap<>();

    /** Never substitute a different winner on duplicate queued callbacks. */
    public synchronized boolean schedule(UUID battle, UUID verifiedWinner) {
        if (battle == null || verifiedWinner == null) return false;
        Pending existing = pending.get(battle);
        if (existing != null) return existing.winner.equals(verifiedWinner);
        if (pending.size() >= MAX_PENDING) return false;
        pending.put(battle, new Pending(verifiedWinner));
        return true;
    }

    /** Must run once per server tick, on the same thread as the native router. */
    public synchronized Batch tick() {
        List<Retry> due = new ArrayList<>();
        List<Retry> expired = new ArrayList<>();
        Iterator<Map.Entry<UUID,Pending>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Pending state = entry.getValue();
            if (--state.ticks > 0) continue;
            state.ticks = RETRY_TICKS;
            if (state.attempts >= MAX_RETRIES) {
                expired.add(new Retry(entry.getKey(), state.winner, state.attempts));
                iterator.remove();
            } else {
                state.attempts++;
                due.add(new Retry(entry.getKey(), state.winner, state.attempts));
            }
        }
        return new Batch(List.copyOf(due), List.copyOf(expired));
    }

    public synchronized void forget(UUID battle) { pending.remove(battle); }
    public synchronized void cancelPlayer(UUID player) {
        pending.entrySet().removeIf(e -> e.getValue().winner.equals(player));
    }
    public synchronized int size() { return pending.size(); }
    public synchronized void clear() { pending.clear(); }
}
