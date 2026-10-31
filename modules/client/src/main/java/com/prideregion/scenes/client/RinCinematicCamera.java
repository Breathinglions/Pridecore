package com.prideregion.scenes.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;
import java.util.UUID;

/**
 * Owner-local cinematic camera. Adjusts only the RENDER CAMERA; never moves,
 * rotates, or changes the camera entity of the real Minecraft player.
 * A vanilla update restores normal view automatically when this is inactive.
 */
public final class RinCinematicCamera {
    public enum Shot { ENTRY, WALK, RIN, SEQUOIA, EXIT }
    public record Pose(Vec3d position, float yaw, float pitch) {}

    private final RinPreviewProjection projection;
    private final boolean enabled;
    private final boolean reducedMotion;
    private ClientWorld world;
    private UUID session;
    private Shot shot = Shot.ENTRY;
    private int moves;
    private int activeTicks;
    private int releaseTicks = -1;
    private Vec3d entryPos;
    private Vec3d previousPos;
    private Vec3d currentPos;
    private Vec3d previousLook;
    private Vec3d currentLook;
    private Vec3d lastTarget;
    private boolean labFixedShot;
    private Boolean originalHudHidden;

    // Authored Hearthleaf laboratory view. Minecraft yaw +90 faces WEST.
    private static final Vec3d LAB_CAMERA = new Vec3d(-54.0, 102.0, 3195.0);
    private static final float LAB_YAW_WEST = 90.0F;
    private static final float LAB_PITCH = 0.0F;

    private static final int FADE_IN = 12;
    private static final int FADE_OUT = 12;
    private static final int MAX_SCENE_TICKS = 400;
    private static final double MAX_VIEWER_DISTANCE = 9.0;

    public RinCinematicCamera(RinPreviewProjection projection) {
        this.projection = projection;
        Path config = FabricLoader.getInstance().getConfigDir()
            .resolve("pride_scenes_client").resolve("camera.properties");
        Properties properties = new Properties();
        properties.setProperty("camera_enabled", "true");
        properties.setProperty("reduced_motion", "false");
        try {
            Files.createDirectories(config.getParent());
            if (Files.exists(config)) {
                try (Reader reader = Files.newBufferedReader(config)) {
                    properties.load(reader);
                }
            } else {
                Files.writeString(config,
                    "# Pride Scenes optional camera, client only. Restart after editing.\n"
                    + "camera_enabled=true\n"
                    + "reduced_motion=false\n");
            }
        } catch (IOException ex) {
            // Missing/unwritable config never interrupts the story scene.
            org.slf4j.LoggerFactory.getLogger("PrideScenesCamera")
                .warn("Could not read camera config; using default settings", ex);
        }
        this.enabled = Boolean.parseBoolean(properties.getProperty("camera_enabled", "true"));
        this.reducedMotion = Boolean.parseBoolean(properties.getProperty("reduced_motion", "false"));
    }

    public void acceptVisual(MinecraftClient client, ClientSceneVisualPayload packet) {
        if (!enabled || reducedMotion) return;
        if (client.world == null || client.player == null) { clear(); return; }
        if (world != null && world != client.world) clear();

        if (packet.action() == ClientSceneVisualPayload.Action.ENTER) {
            if (!projection.hasActor(packet.session())) return;
            clear();
            if (client.player.squaredDistanceTo(packet.x(), packet.y(), packet.z()) > 12 * 12)
                return; // never drag a distant viewer's camera into another area
            world = client.world;
            session = packet.session();
            shot = Shot.ENTRY;
            entryPos = client.player.getPos();
            Vec3d actorEntry = new Vec3d(packet.x(), packet.y(), packet.z());
            labFixedShot = entryPos.squaredDistanceTo(LAB_CAMERA) < 28.0 * 28.0
                && actorEntry.squaredDistanceTo(LAB_CAMERA) < 28.0 * 28.0;
            originalHudHidden = client.options.hudHidden;
            client.options.hudHidden = true;
            Vec3d eye = client.player.getEyePos();
            previousPos = eye;
            currentPos = eye;
            previousLook = eye.add(client.player.getRotationVec(1.0F).multiply(4.0));
            currentLook = previousLook;
            lastTarget = new Vec3d(packet.x(), packet.y() + 1.3, packet.z());
            activeTicks = 0;
            releaseTicks = -1;
            moves = 0;
            return;
        }
        if (session == null || !session.equals(packet.session())) return;
        switch (packet.action()) {
            case MOVE -> {
                moves++;
                shot = moves > 1 ? Shot.EXIT : Shot.WALK;
            }
            case FACE -> shot = Shot.RIN;
            case REMOVE -> release();
            case ENTER -> {}
        }
    }

    public void acceptDialogue(ClientSceneSubtitlePayload subtitle) {
        if (session == null || !session.equals(subtitle.session()) || releaseTicks >= 0)
            return;
        if (shot == Shot.EXIT) return;
        if (subtitle.speaker().equalsIgnoreCase("Rin"))
            shot = Shot.RIN;
        else if (subtitle.speaker().toLowerCase(java.util.Locale.ROOT).contains("sequoia"))
            shot = Shot.SEQUOIA;
    }

    /** Hide vanilla UI/arms only for our private scene; preserve real F1 option. */
    public boolean suppressHud(MinecraftClient client) {
        return enabled && !reducedMotion && world != null && world == client.world
            && session != null && client.player != null && client.currentScreen == null;
    }

    public boolean showingShot() {
        return world != null && session != null && releaseTicks < 0
            && enabled && !reducedMotion;
    }

    public void tick(MinecraftClient client) {
        if (world == null || session == null) return;
        // Some HUD mods update their visibility on a different tick: reapply
        // the temporary F1 state until the private scene has fully faded out.
        if (!client.options.hudHidden) client.options.hudHidden = true;
        if (client.world != world || client.player == null
            || !client.player.isAlive() || !projection.hasActor(session)
               && releaseTicks < 0 || entryPos == null
            || client.player.getPos().squaredDistanceTo(entryPos) > 8 * 8) {
            clear();
            return;
        }
        if (releaseTicks >= 0) {
            if (--releaseTicks <= 0) clear();
            return;
        }
        if (++activeTicks > MAX_SCENE_TICKS) { clear(); return; }

        Vec3d rin = projection.actorPosition(session);
        if (rin == null) { clear(); return; }
        Vec3d rinFocus = rin.add(0, 1.45, 0);
        Entity professor = sequoiaEntity(client);
        Vec3d sequoia = professor == null ? null : professor.getPos().add(0, 1.48, 0);
        Vec3d focus;
        if (shot == Shot.RIN || shot == Shot.ENTRY || shot == Shot.EXIT)
            focus = rinFocus;
        else if (shot == Shot.SEQUOIA && sequoia != null)
            focus = sequoia;
        else if (sequoia != null)
            focus = rinFocus.lerp(sequoia, 0.48);
        else focus = rinFocus;
        if (!Double.isFinite(focus.x) || !Double.isFinite(focus.y)
            || !Double.isFinite(focus.z)) { clear(); return; }

        Vec3d playerEye = client.player.getEyePos();
        Vec3d front = new Vec3d(playerEye.x - focus.x, 0.0,
            playerEye.z - focus.z);
        if (front.lengthSquared() < 0.09)
            front = new Vec3d(0, 0, 1);
        else front = front.normalize();
        Vec3d side = new Vec3d(-front.z, 0, front.x);
        double distance = switch (shot) {
            case ENTRY -> 3.6;
            case WALK -> 3.1;
            case RIN, SEQUOIA -> 2.55;
            case EXIT -> 3.75;
        };
        double orbit = switch (shot) {
            case ENTRY -> -0.9;
            case WALK -> -1.35;
            case RIN -> -1.1;
            case SEQUOIA -> 1.2;
            case EXIT -> 1.45;
        };
        Vec3d desired = focus.add(front.multiply(distance))
            .add(side.multiply(orbit)).add(0, 0.48, 0);
        if (shot == Shot.SEQUOIA && professor != null) {
            // Frame Sequoia from the direction he faces, NOT from behind him.
            Vec3d faceView = professorFrontShot(client, professor, focus, playerEye);
            desired = faceView == null ? playerEye : faceView;
        }
        // Camera is restricted to the viewer's currently loaded lab area.
        Vec3d fromViewer = desired.subtract(playerEye);
        if (fromViewer.lengthSquared() > MAX_VIEWER_DISTANCE * MAX_VIEWER_DISTANCE)
            desired = playerEye.add(fromViewer.normalize().multiply(MAX_VIEWER_DISTANCE));

        // Never cut through the laboratory's walls, ceiling or furnishings.
        // Shorten the shot along the ray to the subject. If it would be too
        // close to frame clearly, stay at the player's normal view.
        var hit = client.world.raycast(new RaycastContext(
            focus, desired, RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE, client.player));
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3d direction = desired.subtract(focus).normalize();
            desired = hit.getPos().subtract(direction.multiply(0.3));
            if (desired.squaredDistanceTo(focus) < 1.2 * 1.2)
                desired = playerEye;
        }
        if (currentPos == null || currentLook == null) { clear(); return; }
        previousPos = currentPos;
        previousLook = currentLook;
        currentPos = currentPos.lerp(desired, 0.24);
        currentLook = currentLook.lerp(focus, 0.36);
        lastTarget = focus;
    }

    private Entity sequoiaEntity(MinecraftClient client) {
        if (client.world == null || client.player == null) return null;
        return client.world.getOtherEntities(client.player,
                client.player.getBoundingBox().expand(10),
                e -> "cobblemon:npc".equals(Registries.ENTITY_TYPE.getId(e.getType()).toString())
                    && (e.getCommandTags().contains("pride_npc_professor_sequoia")
                        || e.getName().getString().toLowerCase(java.util.Locale.ROOT)
                            .contains("sequoia")))
            .stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(client.player)))
            .orElse(null);
    }

    /** Choose a collision-free view of the professor's FRONT side. */
    private Vec3d professorFrontShot(MinecraftClient client, Entity professor,
                                    Vec3d focus, Vec3d playerEye) {
        Vec3d facing = professor.getRotationVec(1.0F);
        Vec3d forward = new Vec3d(facing.x, 0, facing.z);
        if (forward.lengthSquared() < 0.01) return null;
        forward = forward.normalize();
        Vec3d right = new Vec3d(-forward.z, 0, forward.x);
        for (double side : new double[]{0.65, -0.65, 0.0, 1.2, -1.2}) {
            for (double distance : new double[]{2.65, 2.1, 1.6}) {
                Vec3d candidate = focus.add(forward.multiply(distance))
                    .add(right.multiply(side)).add(0, 0.2, 0);
                if (candidate.squaredDistanceTo(playerEye)
                        > MAX_VIEWER_DISTANCE * MAX_VIEWER_DISTANCE) continue;
                var hit = client.world.raycast(new RaycastContext(
                    focus, candidate, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, client.player));
                if (hit.getType() == HitResult.Type.MISS) return candidate;
            }
        }
        // Room geometry has no unobstructed frontal angle: use viewer eye.
        return null;
    }

    /**
     * Called after vanilla Camera.update each render frame; vanilla is the
     * unmodified baseline so view always returns to the player after a scene.
     */
    public Pose renderPose(MinecraftClient client, Camera vanilla, float tickDelta) {
        if (session == null || world != client.world || client.player == null
            || currentPos == null || currentLook == null || previousPos == null
            || previousLook == null || client.currentScreen != null) return null;
        float partial = MathHelper.clamp(tickDelta, 0.0F, 1.0F);
        double weight = releaseTicks >= 0
            ? MathHelper.clamp((releaseTicks + 1 - partial) / FADE_OUT, 0, 1)
            : MathHelper.clamp((activeTicks - 1 + partial) / FADE_IN, 0, 1);
        weight = weight * weight * (3 - 2 * weight);
        if (weight <= 0.001) return null;
        if (labFixedShot) {
            // Fixed camera is render-only: NEVER teleport or rotate the real player.
            // After REMOVE, weight fades to zero and vanilla resumes the original
            // player location and orientation automatically.
            Vec3d blended = vanilla.getPos().lerp(LAB_CAMERA, weight);
            float blendedYaw = MathHelper.lerpAngleDegrees((float) weight,
                vanilla.getYaw(), LAB_YAW_WEST);
            float blendedPitch = MathHelper.lerp((float) weight,
                vanilla.getPitch(), LAB_PITCH);
            return new Pose(blended, blendedYaw, blendedPitch);
        }
        Vec3d position = previousPos.lerp(currentPos, partial);
        Vec3d focus = previousLook.lerp(currentLook, partial);
        Vec3d aim = focus.subtract(position);
        if (aim.lengthSquared() < 0.04) return null;
        float yaw = (float)(Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90.0);
        float pitch = (float)-Math.toDegrees(Math.atan2(aim.y,
            Math.sqrt(aim.x * aim.x + aim.z * aim.z)));
        pitch = MathHelper.clamp(pitch, -70.0F, 70.0F);
        Vec3d blended = vanilla.getPos().lerp(position, weight);
        float blendedYaw = MathHelper.lerpAngleDegrees((float) weight, vanilla.getYaw(), yaw);
        float blendedPitch = MathHelper.lerp((float) weight, vanilla.getPitch(), pitch);
        return new Pose(blended, blendedYaw, blendedPitch);
    }

    public void release() {
        if (world != null && releaseTicks < 0)
            releaseTicks = FADE_OUT;
    }

    public void clear() {
        // The cinematic must never leave F1 forced on after a disconnect,
        // dimension swap, camera timeout or native GUI handoff.
        if (originalHudHidden != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) client.options.hudHidden = originalHudHidden;
            originalHudHidden = null;
        }
        labFixedShot = false;
        world = null;
        session = null;
        entryPos = null;
        previousPos = null;
        currentPos = null;
        previousLook = null;
        currentLook = null;
        lastTarget = null;
        releaseTicks = -1;
        activeTicks = 0;
        moves = 0;
        shot = Shot.ENTRY;
    }
}
