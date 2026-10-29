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
    }
    private final Object observable;
    private final Object subscription;

    private CobblemonStoryVictoryListener(Object observable, Object subscription) {
        this.observable = observable; this.subscription = subscription;
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
                Set<UUID> playerWinners = new HashSet<>();
                for (Object actor : (List<?>) winners) {
                    UUID actorId = (UUID) actor.getClass().getMethod("getUuid").invoke(actor);
                    if (playerIds.contains(actorId)) playerWinners.add(actorId);
                }
                // Story fights are always one player versus NPC. A multi-player
                // battle must never mint single-player campaign progression.
                if (playerIds.size() == 1 && playerWinners.size() == 1)
                    sink.victory(battleId, playerWinners.iterator().next());
                else sink.cancelled(battleId);
            } catch (ReflectiveOperationException | RuntimeException error) {
                // Event subscribers must never crash the Cobblemon event bus.
                // Fail closed and let disconnect/cleanup revoke the ticket.
                System.err.println("[PrideCore] Cobblemon victory inspection failed: " + error);
            }
        };
        Object subscription = observable.getClass().getMethod("subscribe", Consumer.class)
            .invoke(observable, consumer);
        return new CobblemonStoryVictoryListener(observable, subscription);
    }

    public void uninstall() throws ReflectiveOperationException {
        Class<?> subscriptionType = Class.forName(
            "com.cobblemon.mod.common.api.reactive.ObservableSubscription");
        observable.getClass().getMethod("unsubscribe", subscriptionType)
            .invoke(observable, subscription);
    }
}
