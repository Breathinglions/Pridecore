package com.prideregion.core.story;

import com.prideregion.core.ProgressionService;
import java.io.IOException;
import java.util.*;

/**
 * Module 02 battle-proof boundary. A trusted, server-side battle callback must
 * arm the exact player's encounter and later report the matching victory.
 * No player command, NPC dialogue choice, or client packet may call victory().
 *
 * This is an adapter foundation, NOT a Cobblemon event subscription. It does
 * not award money, badges, Pokémon, items, or legacy League achievements.
 */
public final class StoryBattleProof {
    public record Encounter(String id, String requiredFact, String earnedFact) {
        public Encounter {
            if (!StoryDefinition.validId(id) || !StoryDefinition.owned(requiredFact)
                    || !StoryDefinition.owned(earnedFact))
                throw new IllegalArgumentException("Invalid story battle definition");
        }
    }
    public record Ticket(UUID player, UUID battle, String encounterId) {
        public Ticket {
            Objects.requireNonNull(player);
            Objects.requireNonNull(battle);
            if (!StoryDefinition.validId(encounterId))
                throw new IllegalArgumentException("Invalid encounter ID");
        }
    }
    private static final Map<String, Encounter> DEFINITIONS = Map.of(
        "rin_route1", new Encounter("rin_route1", "pride:met_rin", "pride:rin_route1_won"),
        "rin_canyon", new Encounter("rin_canyon", "pride:eclipse_contact", "pride:rin_canyon_won"),
        "rin_summit", new Encounter("rin_summit", "pride:frozen_warning", "pride:rin_summit_won"),
        "eclipse_lieutenant", new Encounter("eclipse_lieutenant", "pride:memory_truth", "pride:eclipse_lieutenant_won"),
        "sable", new Encounter("sable", "pride:eclipse_assault", "pride:sable_battle_won"),
        "director_cael", new Encounter("director_cael", "pride:sable_battle_won", "pride:cael_battle_won")
    );

    private final ProgressionService progression;
    private final Map<UUID, Ticket> active = new HashMap<>();

    public StoryBattleProof(ProgressionService progression) {
        this.progression = Objects.requireNonNull(progression);
    }

    public static Collection<Encounter> definitions() { return DEFINITIONS.values(); }

    /** Called only after the battle adapter verifies a real configured opponent. */
    public synchronized Ticket arm(UUID player, UUID battle, String encounterId)
            throws IOException {
        Objects.requireNonNull(player);
        Objects.requireNonNull(battle);
        Encounter encounter = DEFINITIONS.get(encounterId);
        if (encounter == null) throw new IllegalArgumentException("Unknown story battle");
        if (!progression.get(player).facts().contains(encounter.requiredFact()))
            throw new IllegalStateException("Story battle chapter locked");
        Ticket ticket = new Ticket(player, battle, encounterId);
        Ticket previous = active.putIfAbsent(player, ticket);
        if (previous != null && !previous.equals(ticket))
            throw new IllegalStateException("Player already has an active story battle");
        return ticket;
    }

    /**
     * A trusted Cobblemon victory callback must pass the true battle UUID,
     * true winner UUID and the ticket returned by arm(). Mismatches fail shut.
     * Persisting the earned fact is idempotent across retries and restarts.
     */
    public synchronized boolean victory(Ticket ticket, UUID actualBattle, UUID winner)
            throws IOException {
        if (ticket == null || !ticket.battle().equals(actualBattle)
                || !ticket.player().equals(winner)
                || !ticket.equals(active.get(ticket.player())))
            return false;
        Encounter encounter = DEFINITIONS.get(ticket.encounterId());
        if (encounter == null) return false;
        if (!progression.get(ticket.player()).facts().contains(encounter.requiredFact()))
            return false;
        // Only clear after persistent write succeeds; allow callback retry on IO failure.
        boolean changed = progression.grant(ticket.player(), encounter.earnedFact()).changed();
        active.remove(ticket.player());
        return changed;
    }

    /** Battle loss, cancellation, disconnect or callback timeout: never grant credit. */
    public synchronized void cancel(UUID player, UUID battle) {
        Ticket ticket = active.get(player);
        if (ticket != null && ticket.battle().equals(battle)) active.remove(player);
    }

    /** Clear pending runtime tickets on shutdown. Persistence is fact-based. */
    public synchronized void clear() { active.clear(); }
}
