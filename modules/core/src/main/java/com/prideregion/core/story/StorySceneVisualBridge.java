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

    /**
     * Operator-visible snapshot of the exact visual packet prerequisites.
     * A scripted chat cue does NOT imply the client visual channel is ready.
     * No packets or player progression are affected by this diagnostic.
     */
    public static String diagnostic(ServerPlayerEntity viewer,
                                    StoryPrivateSceneDirector.SceneId scene) {
        Objects.requireNonNull(viewer);
        Objects.requireNonNull(scene);
        String dimension = viewer.getWorld().getRegistryKey().getValue().toString();
        var config = anchors;
        if (config.count() == 0)
            return "ANCHORS_DISABLED (server did not load an enabled, complete scene layout)";
        var match = config.forDimension(scene, dimension);
        if (match.isEmpty())
            return "SCENE_DIMENSION_MISMATCH (player dimension=" + dimension
                + ", configured layouts=" + config.count() + ")";
        var entry = match.get().entry();
        var viewerPoint = new StorySceneAnchorConfig.Point(
            viewer.getX(), viewer.getY(), viewer.getZ());
        double distance = Math.sqrt(viewerPoint.distanceSquared(entry));
        if (distance > 96.0)
            return String.format(java.util.Locale.ROOT,
                "TOO_FAR (entry=%.2f,%.2f,%.2f; player distance=%.1f blocks; max=96)",
                entry.x(), entry.y(), entry.z(), distance);
        if (!ServerPlayNetworking.canSend(viewer, StorySceneVisualPayload.ID))
            return "CLIENT_CHANNEL_MISSING (client does not advertise pridecore:scene_visual_v1)";
        var stop = match.get().move();
        var facing = match.get().face();
        boolean missingFacingTarget = Math.pow(stop.x() - facing.x(), 2.0D)
            + Math.pow(stop.z() - facing.z(), 2.0D) < 0.01D;
        return String.format(java.util.Locale.ROOT,
            "READY (client channel advertised; dimension=%s; entry=%.2f,%.2f,%.2f; distance=%.1f; exit=%.2f,%.2f,%.2f)%s",
            dimension, entry.x(), entry.y(), entry.z(), distance,
            match.get().exit().x(), match.get().exit().y(), match.get().exit().z(),
            missingFacingTarget
                ? " WARNING: FACE_EQUALS_MOVE (set scene.<scene>.face to the person Rin should look at, not his destination)"
                : "");
    }

    /**
     * Strict readiness for a real onboarding scene: never start a silent
     * eight-second cinematic that cannot render on this one client's screen.
     * Returning false permits an immediate native starter GUI fallback.
     */
    public static boolean canRender(ServerPlayerEntity viewer,
                                    StoryPrivateSceneDirector.SceneId scene) {
        if (!ServerPlayNetworking.canSend(viewer, StorySceneVisualPayload.ID))
            return false;
        String world = viewer.getWorld().getRegistryKey().getValue().toString();
        return anchors.forScene(scene, world,
            new StorySceneAnchorConfig.Point(
                viewer.getX(), viewer.getY(), viewer.getZ())).isPresent();
    }

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
        var layout = action == StorySceneVisualPayload.Action.REMOVE
            ? config.forDimension(cue.scene(), world)
            : config.forScene(cue.scene(), world,
                new StorySceneAnchorConfig.Point(viewer.getX(), viewer.getY(), viewer.getZ()));
        if (layout.isEmpty()) return false;
        var point = switch (action) {
            case ENTER -> layout.get().entry();
            // The Lab uses the same MOVE packet for entry and exit. The
            // trusted scene director supplies the lab_exit cue; clients
            // never choose their own exit coordinates.
            case MOVE -> "lab_exit".equals(cue.anchor())
                && cue.scene() == StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO
                    ? layout.get().exit() : layout.get().move();
            case FACE -> layout.get().face();
            case REMOVE -> layout.get().entry();
        };
        ServerPlayNetworking.send(viewer, new StorySceneVisualPayload(cue.session(), action,
            point.x(), point.y(), point.z()));
        return true;
    }
}
