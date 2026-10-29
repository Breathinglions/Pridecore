package com.prideregion.core.story;

import java.util.*;

/**
 * Prevents remote or stale chat choices after opening a physical story NPC.
 * Only server-bound entity identity and distance can authorize the choice.
 * Admin test conversations may run without a physical anchor.
 */
public final class StoryDialogueAnchorLedger {
    public static final double MAX_DISTANCE_SQUARED = 36.0D;
    private final Map<UUID, Anchor> anchors = new HashMap<>();

    public record Anchor(UUID entity, String marker, String dimension) {
        public Anchor {
            Objects.requireNonNull(entity);
            Objects.requireNonNull(marker);
            Objects.requireNonNull(dimension);
            if (!marker.startsWith("pride_npc_"))
                throw new IllegalArgumentException("Not a story NPC marker");
        }
    }

    public synchronized void bind(UUID player, UUID entity, String marker, String dimension) {
        Objects.requireNonNull(player);
        anchors.put(player, new Anchor(entity, marker, dimension));
    }

    public synchronized Optional<Anchor> get(UUID player) {
        return Optional.ofNullable(anchors.get(player));
    }

    public static boolean permits(Anchor bound, UUID entity, String marker,
                                  String dimension, double squaredDistance) {
        return bound != null && bound.entity().equals(entity)
            && bound.marker().equals(marker) && bound.dimension().equals(dimension)
            && Double.isFinite(squaredDistance) && squaredDistance >= 0
            && squaredDistance <= MAX_DISTANCE_SQUARED;
    }

    public synchronized void clear(UUID player) { anchors.remove(player); }
    public synchronized void clearAll() { anchors.clear(); }
}
