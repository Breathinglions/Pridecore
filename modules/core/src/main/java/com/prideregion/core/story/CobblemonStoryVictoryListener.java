package com.prideregion.core.story;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Cobblemon 1.8.1 victory-event listener. Never creates a battle ticket:
 * the trusted RCT launcher must register its UUID before a victory counts.
 */
public final class CobblemonStoryVictoryListener {
    public interface Sink {
        boolean isRegistered(UUID battleId);
        void victory(UUID battleId, UUID player);
        void cancelled(UUID battleId);
        default void fainted(UUID battleId, UUID pokemonId) {}
    }
    private final Object observable;
    private final Object subscription;
    private final Object faintObservable;
    private final Object faintSubscription;

    private CobblemonStoryVictoryListener(Object observable, Object subscription,
                                          Object faintObservable, Object faintSubscription) {
        this.observable = observable; this.subscription = subscription;
        this.faintObservable = faintObservable; this.faintSubscription = faintSubscription;
    }

    public static CobblemonStoryVictoryListener install(ClassLoader loader, Sink sink)
            throws ReflectiveOperationException {
        Objects.requireNonNull(sink);
        Class<?> events = Class.forName(
            "com.cobblemon.mod.common.api.events.CobblemonEvents", true, loader);
        Object observable = events.getField("BATTLE_VICTORY").get(null);
        Consumer<Object> consumer = event -> {
            try {
                Object battle = event.getClass().getMethod("getBattle").invoke(event);
                UUID battleId = (UUID) battle.getClass().getMethod("getBattleId").invoke(battle);
                if (!sink.isRegistered(battleId)) return;
                if (Boolean.TRUE.equals(event.getClass().getMethod("getWasWildCapture").invoke(event))) {
                    sink.cancelled(battleId); return;
                }
                Set<UUID> playerIds = new HashSet<>();
                Object players = battle.getClass().getMethod("getPlayerUUIDs").invoke(battle);
                for (Object id : (Iterable<?>) players)
                    if (id instanceof UUID uuid) playerIds.add(uuid);
                Object winners = event.getClass().getMethod("getWinners").invoke(event);
                Object losers = event.getClass().getMethod("getLosers").invoke(event);
                Set<UUID> playerWinners = new HashSet<>();
                for (Object actor : (List<?>) winners) {
                    UUID actorId = (UUID) actor.getClass().getMethod("getUuid").invoke(actor);
                    if (playerIds.contains(actorId)) playerWinners.add(actorId);
                }
                // Reject ambiguous outcomes with missing or shared winner/loser actors.
                if (!(losers instanceof List<?> losingActors) || losingActors.isEmpty()
                        || ((List<?>) winners).isEmpty()) {
                    sink.cancelled(battleId);
                    return;
                }
                // Story fights are always one player versus NPC. A multi-player
                // battle must never mint single-player campaign progression.
                if (soloWinner(playerIds, playerWinners).isPresent())
                    sink.victory(battleId, soloWinner(playerIds, playerWinners).orElseThrow());
                else sink.cancelled(battleId);
            } catch (ReflectiveOperationException | RuntimeException error) {
                // Event subscribers must never crash the Cobblemon event bus.
                // Fail closed and let disconnect/cleanup revoke the ticket.
                System.err.println("[PrideCore] Cobblemon victory inspection failed: " + error);
            }
        };
        Object subscription = observable.getClass().getMethod("subscribe", Consumer.class)
            .invoke(observable, consumer);
        // Use the exact 1.8.1 BATTLE_FAINTED event to drive presentation
        // beats. The current battle ticket still supplies all authorization.
        Consumer<Object> faintConsumer = event -> {
            try {
                Object battle = event.getClass().getMethod("getBattle").invoke(event);
                UUID battleId = (UUID) battle.getClass().getMethod("getBattleId").invoke(battle);
                if (!sink.isRegistered(battleId)) return;
                Object players = battle.getClass().getMethod("getPlayerUUIDs").invoke(battle);
                Set<UUID> playerIds = new HashSet<>();
                for (Object id : (Iterable<?>) players) {
                    if (!(id instanceof UUID uuid)) return;
                    playerIds.add(uuid);
                }
                if (playerIds.size() != 1) return;
                Object fainted = event.getClass().getMethod("getKilled").invoke(event);
                Object actor = fainted.getClass().getMethod("getActor").invoke(fainted);
                if (actor == null) return;
                // Only a Pokémon belonging to an NPC-side BattleActor may
                // trigger a boss phase. Never count the player's knocked-out Pokémon.
                Object actorPlayers = actor.getClass().getMethod("getPlayerUUIDs").invoke(actor);
                for (Object ignored : (Iterable<?>) actorPlayers) return;
                UUID actorId = (UUID) actor.getClass().getMethod("getUuid").invoke(actor);
                if (playerIds.contains(actorId)) return;
                UUID pokemon = (UUID) fainted.getClass().getMethod("getUuid").invoke(fainted);
                sink.fainted(battleId, pokemon);
            } catch (ReflectiveOperationException | RuntimeException error) {
                System.err.println("[PrideCore] Cobblemon faint event inspection failed: " + error);
            }
        };
        // Faint events are optional presentation. Never disable the mandatory
        // victory listener or entire story module if a mod build lacks them.
        Object faintObservable = null;
        Object faintSubscription = null;
        try {
            faintObservable = events.getField("BATTLE_FAINTED").get(null);
            faintSubscription = faintObservable.getClass()
                .getMethod("subscribe", Consumer.class).invoke(faintObservable, faintConsumer);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError optionalError) {
            System.err.println("[PrideCore] Optional knockout cinematics disabled: "
                + optionalError);
        }
        return new CobblemonStoryVictoryListener(
            observable, subscription, faintObservable, faintSubscription);
    }

    /** Deterministic eligibility rule, separately testable without Cobblemon. */
    public static Optional<UUID> soloWinner(Set<UUID> participants, Set<UUID> winners) {
        if (participants == null || winners == null ||
                participants.size() != 1 || winners.size() != 1)
            return Optional.empty();
        UUID winner = winners.iterator().next();
        return participants.contains(winner) ? Optional.of(winner) : Optional.empty();
    }

    public void uninstall() throws ReflectiveOperationException {
        try {
            if (faintObservable != null && faintSubscription != null)
                unsubscribe(faintObservable, faintSubscription);
        } finally {
            unsubscribe(observable, subscription);
        }
    }

    private static void unsubscribe(Object source, Object subscription)
            throws ReflectiveOperationException {
        // Resolve the actual subscription interface from the event source,
        // not from a separate Minecraft or system classloader.
        Method action = Arrays.stream(source.getClass().getMethods())
            .filter(m -> m.getName().equals("unsubscribe")
                && m.getParameterCount() == 1
                && m.getParameterTypes()[0].isInstance(subscription))
            .findFirst().orElseThrow(() -> new NoSuchMethodException(
                "Cobblemon subscription cannot be unregistered"));
        action.invoke(source, subscription);
    }
}
