package com.prideregion.core.story;

import java.util.*;

/**
 * Correlates verified native battle-start and battle-end events before forwarding
 * them to StoryBattleProof. This class intentionally does not register a Cobblemon
 * callback: the game's version-specific event implementation must supply the
 * authoritative identities. No player command can register a battle here.
 */
public final class StoryBattleSessionRouter {
    public record NativeStart(UUID battleId, UUID playerId, String encounterId,
                              String trainerIdentity) {
        public NativeStart {
            Objects.requireNonNull(battleId);
            Objects.requireNonNull(playerId);
            Objects.requireNonNull(encounterId);
            Objects.requireNonNull(trainerIdentity);
        }
    }
    private static final Map<String,String> TRUSTED_TRAINERS = Map.of(
        "rin_route1","pride_story:rin_route1",
        "rin_canyon","pride_story:rin_canyon",
        "rin_summit","pride_story:rin_summit",
        "eclipse_lieutenant","pride_story:eclipse_lieutenant",
        "sable","pride_story:sable",
        "director_cael","pride_story:director_cael"
    );
    private final StoryBattleProof proof;
    private final Map<UUID, StoryBattleProof.Ticket> byBattle = new HashMap<>();

    public StoryBattleSessionRouter(StoryBattleProof proof) {
        this.proof = Objects.requireNonNull(proof);
    }

    /** Only the trusted server event adapter should call this method. */
    public synchronized boolean started(NativeStart start) throws java.io.IOException {
        String expected = TRUSTED_TRAINERS.get(start.encounterId());
        if (!Objects.equals(expected, start.trainerIdentity())) return false;
        StoryBattleProof.Ticket old = byBattle.get(start.battleId());
        if (old != null) return old.player().equals(start.playerId())
            && old.encounterId().equals(start.encounterId());
        StoryBattleProof.Ticket ticket = proof.arm(
            start.playerId(), start.battleId(), start.encounterId());
        byBattle.put(start.battleId(), ticket);
        return true;
    }

    /** Winner ID must come from the resolved native battle, not a client. */
    public synchronized boolean ended(UUID battleId, UUID winnerId)
            throws java.io.IOException {
        StoryBattleProof.Ticket ticket = byBattle.get(battleId);
        if (ticket == null) return false;
        if (winnerId == null || !winnerId.equals(ticket.player())) {
            byBattle.remove(battleId);
            proof.cancel(ticket.player(), battleId);
            return false;
        }
        boolean credited = proof.victory(ticket, battleId, winnerId);
        byBattle.remove(battleId);
        return credited;
    }

    public synchronized void cancel(UUID battleId) {
        StoryBattleProof.Ticket ticket = byBattle.remove(battleId);
        if (ticket != null) proof.cancel(ticket.player(), battleId);
    }

    public synchronized void disconnect(UUID playerId) {
        byBattle.entrySet().removeIf(e -> e.getValue().player().equals(playerId));
        proof.cancelPlayer(playerId);
    }

    public synchronized void clear() {
        byBattle.clear();
        proof.clear();
    }

    public static Map<String,String> acceptedTrainers() { return TRUSTED_TRAINERS; }
}
