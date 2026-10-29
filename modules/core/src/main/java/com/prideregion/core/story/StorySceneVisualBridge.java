package com.prideregion.core.story;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Opt-in staging-only visual bridge. Registers a clientbound packet type but
 * sends nothing unless exact world anchors are enabled and the one recipient
 * explicitly advertises support for the companion client module.
 */
public final class StorySceneVisualBridge {
    private static volatile StorySceneAnchorConfig anchors = StorySceneAnchorConfig.disabled();
    private StorySceneVisualBridge() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(
            StorySceneVisualPayload.ID, StorySceneVisualPayload.CODEC);
    }
    public static void configure(Path path) throws IOException {
        anchors = StorySceneAnchorConfig.load(path);
    }
    public static void disable() { anchors = StorySceneAnchorConfig.disabled(); }
    public static int enabledLayouts() { return anchors.count(); }

    /** Only real server-owned scene events may reach the selected player. */
    public static boolean emit(ServerPlayerEntity viewer,
                               StoryPrivateSceneDirector.Event cue) {
        Objects.requireNonNull(viewer);
        Objects.requireNonNull(cue);
        if (!viewer.getUuid().equals(cue.owner())
                || !ServerPlayNetworking.canSend(viewer, StorySceneVisualPayload.ID))
            return false;
        var action = switch (cue.kind()) {
            case ACTOR_ENTER -> StorySceneVisualPayload.Action.ENTER;
            case ACTOR_MOVE -> StorySceneVisualPayload.Action.MOVE;
            case ACTOR_FACE -> StorySceneVisualPayload.Action.FACE;
            case ACTOR_REMOVE -> StorySceneVisualPayload.Action.REMOVE;
            default -> null;
        };
        if (action == null) return false;
        String world = viewer.getWorld().getRegistryKey().getValue().toString();
        var config = anchors;
        var layout = config.forScene(cue.scene(), world,
            new StorySceneAnchorConfig.Point(viewer.getX(), viewer.getY(), viewer.getZ()));
        if (layout.isEmpty()) return false;
        var point = switch (action) {
            case ENTER -> layout.get().entry();
            case MOVE -> layout.get().move();
            case FACE -> layout.get().face();
            case REMOVE -> layout.get().entry();
        };
        ServerPlayNetworking.send(viewer, new StorySceneVisualPayload(cue.session(), action,
            point.x(), point.y(), point.z()));
        return true;
    }
}
