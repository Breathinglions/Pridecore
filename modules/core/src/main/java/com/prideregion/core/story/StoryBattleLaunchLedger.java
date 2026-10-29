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
    private final Map<UUID, UUID> battles = new HashMap<>();

    public synchronized boolean reserve(Launch launch) {
        if (pending.containsKey(launch.player()) || battles.containsValue(launch.player()))
            return false;
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
        battles.put(nativeBattle, player);
        return Optional.of(new StoryBattleSessionRouter.NativeStart(
            nativeBattle, player, launch.encounter(),
            StoryBattleSessionRouter.acceptedTrainers().get(launch.encounter())));
    }

    public synchronized void cancelPlayer(UUID player) {
        pending.remove(player);
        battles.entrySet().removeIf(e -> e.getValue().equals(player));
    }

    /** Read-only authorization check for native battle event callbacks. */
    public synchronized boolean owns(UUID battle, UUID player) {
        return player != null && player.equals(battles.get(battle));
    }

    public synchronized void finished(UUID battle) { battles.remove(battle); }
    public synchronized void clear() { pending.clear(); battles.clear(); }
}
