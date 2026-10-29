package com.prideregion.core.story;

import java.util.*;

/**
 * Server-controlled reservation for an RCT battle launch. The trusted launcher
 * reserves before invoking RCT, then supplies the returned native battle UUID.
 * Nothing provided by a client may reserve or complete this handshake.
 */
public final class StoryBattleLaunchLedger {
    public record Launch(UUID player, String encounter, UUID trainerEntity) {
        public Launch {
            Objects.requireNonNull(player);
            Objects.requireNonNull(trainerEntity);
            if (!StoryBattleSessionRouter.acceptedTrainers().containsKey(encounter))
                throw new IllegalArgumentException("Unknown authored encounter");
        }
    }
    private final Map<UUID, Launch> pending = new HashMap<>();
    private final Map<UUID, StoryBattleSessionRouter.NativeStart> battles = new HashMap<>();
    private final Map<UUID, UUID> trainerByBattle = new HashMap<>();

    public synchronized boolean reserve(Launch launch) {
        if (pending.containsKey(launch.player()) || active(launch.player())
                || trainerBusy(launch.trainerEntity())) return false;
        pending.put(launch.player(), launch);
        return true;
    }

    /** Only the RCT startBattle return value may enter here. */
    public synchronized Optional<StoryBattleSessionRouter.NativeStart> launched(
            UUID player, UUID trainerEntity, UUID nativeBattle) {
        if (nativeBattle == null || battles.containsKey(nativeBattle)) return Optional.empty();
        Launch launch = pending.get(player);
        if (launch == null || !launch.trainerEntity().equals(trainerEntity)) return Optional.empty();
        pending.remove(player);
        var start = new StoryBattleSessionRouter.NativeStart(
            nativeBattle, player, launch.encounter(),
            StoryBattleSessionRouter.acceptedTrainers().get(launch.encounter()));
        battles.put(nativeBattle, start);
        trainerByBattle.put(nativeBattle, trainerEntity);
        return Optional.of(start);
    }

    public synchronized boolean pending(UUID player) { return pending.containsKey(player); }

    public synchronized boolean active(UUID player) {
        return battles.values().stream().anyMatch(start -> start.playerId().equals(player));
    }

    /** Exact live native battle UUIDs belonging to one authorized player. */
    public synchronized Set<UUID> activeBattles(UUID player) {
        if (player == null) return Set.of();
        Set<UUID> result = new HashSet<>();
        for (var entry : battles.entrySet())
            if (player.equals(entry.getValue().playerId())) result.add(entry.getKey());
        return Set.copyOf(result);
    }

    public synchronized void cancelPlayer(UUID player) {
        pending.remove(player);
        Set<UUID> retired = new HashSet<>();
        battles.entrySet().removeIf(e -> {
            boolean owned = e.getValue().playerId().equals(player);
            if (owned) retired.add(e.getKey());
            return owned;
        });
        for (UUID id : retired) trainerByBattle.remove(id);
    }

    /** Read-only authorization check for native battle event callbacks. */
    public synchronized boolean owns(UUID battle, UUID player) {
        var start = battles.get(battle);
        return start != null && player != null && player.equals(start.playerId());
    }

    /** Exact identity, not just UUID ownership, is required at native arm. */
    public synchronized boolean matches(StoryBattleSessionRouter.NativeStart start) {
        return start != null && start.equals(battles.get(start.battleId()));
    }

    /** Return the authenticated owner before retiring a battle result. */
    public synchronized Optional<UUID> owner(UUID battle) {
        return Optional.ofNullable(battles.get(battle))
            .map(StoryBattleSessionRouter.NativeStart::playerId);
    }

    /** A physical RCT trainer may service only one pending or live story fight. */
    public synchronized boolean trainerBusy(UUID trainerEntity) {
        if (trainerEntity == null) return false;
        return pending.values().stream()
            .anyMatch(x -> x.trainerEntity().equals(trainerEntity))
            || trainerByBattle.containsValue(trainerEntity);
    }

    public synchronized void finished(UUID battle) {
        battles.remove(battle);
        trainerByBattle.remove(battle);
    }
    public synchronized void clear() {
        pending.clear();
        battles.clear();
        trainerByBattle.clear();
    }
}
