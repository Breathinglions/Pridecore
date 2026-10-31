package com.prideregion.scenes.client;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import java.util.*;

/** Server-authored cinematic rendered privately. Never teleports the player. */
public final class ClientCameraDirector {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger("PrideCameraDirector");
    private record Shot(String id, Vec3d pos, float yaw, float pitch,
                        int duration, int transition, String mode, String target,
                        Vec3d offset) {}
    private record Frame(Vec3d pos, float yaw, float pitch) {}

    private final RinPreviewProjection projection;
    private UUID session;
    private ClientWorld world;
    private Vec3d originalPlayerPos;
    private Boolean originalHudHidden;
    private List<Shot> shots = List.of();
    private int index;
    private int shotTicks;
    private int elapsed;
    private Frame previousFrame;
    private Frame currentFrame;
    private Frame fromFrame;

    public ClientCameraDirector(RinPreviewProjection projection) {
        this.projection = projection;
    }

    public boolean active(MinecraftClient client) {
        return session != null && world == client.world
            && client.player != null && client.currentScreen == null;
    }

    public void accept(MinecraftClient client, ClientCameraDirectorPayload packet) {
        if (packet.stop()) {
            if (session != null && session.equals(packet.session())) clear();
            return;
        }
        clear();
        if (client.world == null || client.player == null) return;
        try {
            JsonObject root = JsonParser.parseString(packet.sceneJson()).getAsJsonObject();
            String requiredWorld = root.get("dimension").getAsString();
            if (!requiredWorld.equals("*")
                    && !requiredWorld.equals(client.world.getRegistryKey().getValue().toString()))
                return;
            JsonArray list = root.getAsJsonArray("shots");
            if (list == null || list.isEmpty() || list.size() > 32) return;
            ArrayList<Shot> loaded = new ArrayList<>();
            int total = 0;
            for (JsonElement element : list) {
                JsonObject s = element.getAsJsonObject();
                Vec3d point = new Vec3d(s.get("x").getAsDouble(),
                    s.get("y").getAsDouble(), s.get("z").getAsDouble());
                if (!valid(point)) throw new IllegalArgumentException("Invalid shot coordinates");
                int duration = s.get("durationTicks").getAsInt();
                int transition = s.get("transitionTicks").getAsInt();
                if (duration < 1 || duration > 1200 || transition < 0 || transition > duration)
                    throw new IllegalArgumentException("Invalid shot timing");
                total += duration;
                if (total > 2400) throw new IllegalArgumentException("Long camera sequence");
                String target = s.get("target").getAsString();
                if (target.length() > 48) throw new IllegalArgumentException("Invalid camera target");
                Shot shot = new Shot(s.get("id").getAsString(), point,
                    s.get("yaw").getAsFloat(), s.get("pitch").getAsFloat(),
                    duration, transition, s.get("mode").getAsString(), target,
                    new Vec3d(s.get("offsetX").getAsDouble(),
                        s.get("offsetY").getAsDouble(), s.get("offsetZ").getAsDouble()));
                loaded.add(shot);
            }
            Vec3d eye = client.player.getEyePos();
            if (loaded.getFirst().pos().squaredDistanceTo(eye) > 128 * 128) return;
            shots = List.copyOf(loaded);
            session = packet.session();
            world = client.world;
            originalPlayerPos = client.player.getPos();
            originalHudHidden = client.options.hudHidden;
            client.options.hudHidden = true;
            index = 0; shotTicks = 0; elapsed = 0;
            // For the first shot use its safe authored position rather than
            // orbiting through walls from the actual player camera.
            Shot first = shots.getFirst();
            Frame initial = frameFor(client, first, null);
            if (!spaceClear(client, initial.pos())) initial = viewerFrame(client);
            currentFrame = initial;
            previousFrame = initial;
            fromFrame = initial;
        } catch (RuntimeException ex) {
            LOG.warn("Rejected invalid camera scene payload: {}", ex.toString());
            clear();
        }
    }

    public void tick(MinecraftClient client) {
        if (session == null) return;
        if (world != client.world || client.player == null
                || !client.player.isAlive() || originalPlayerPos == null
                || client.player.getPos().squaredDistanceTo(originalPlayerPos) > 32 * 32) {
            clear(); return;
        }
        client.options.hudHidden = true;
        if (index >= shots.size()) { clear(); return; }
        Shot shot = shots.get(index);
        previousFrame = currentFrame;
        Frame desired = frameFor(client, shot, currentFrame);
        if (!spaceClear(client, desired.pos())) desired = currentFrame;
        // Smooth lens movement follows an NPC, while static shots blend from
        // the previous camera only through clear space.
        double alpha;
        if ("follow".equals(shot.mode())
                && ("rin".equalsIgnoreCase(shot.target())
                    && projection.actorPosition(session) != null
                    || target(client, shot.target()) != null))
            alpha = 0.22;
        else if (shot.transition() > 0)
            alpha = MathHelper.clamp((double) shotTicks / shot.transition(), 0, 1);
        else alpha = 1.0;
        if (!"follow".equals(shot.mode())) {
            alpha = alpha * alpha * (3 - 2 * alpha);
            Frame first = fromFrame == null ? desired : fromFrame;
            Vec3d point = first.pos().lerp(desired.pos(), alpha);
            if (!segmentClear(client, currentFrame.pos(), point)) point = desired.pos();
            currentFrame = new Frame(point,
                MathHelper.lerpAngleDegrees((float) alpha, first.yaw(), desired.yaw()),
                MathHelper.lerp((float) alpha, first.pitch(), desired.pitch()));
        } else {
            Vec3d point = currentFrame.pos().lerp(desired.pos(), alpha);
            if (!segmentClear(client, currentFrame.pos(), point)) point = desired.pos();
            currentFrame = new Frame(point,
                MathHelper.lerpAngleDegrees((float) alpha, currentFrame.yaw(), desired.yaw()),
                MathHelper.lerp((float) alpha, currentFrame.pitch(), desired.pitch()));
        }
        shotTicks++;
        elapsed++;
        if (shotTicks >= shot.duration()) {
            index++;
            shotTicks = 0;
            fromFrame = currentFrame;
        }
    }

    private Frame frameFor(MinecraftClient client, Shot shot, Frame fallback) {
        Entity actor = target(client, shot.target());
        Vec3d rin = "rin".equalsIgnoreCase(shot.target())
            ? projection.actorPosition(session) : null;
        Vec3d focused = rin != null ? rin.add(0, 1.45, 0)
            : actor != null ? actor.getPos().add(0, actor.getStandingEyeHeight(), 0) : null;
        if ("follow".equals(shot.mode()) && focused != null) {
            Vec3d base = rin == null ? actor.getPos() : rin;
            Vec3d pos = base.add(shot.offset());
            if (spaceClear(client, pos)) {
                Vec3d diff = focused.subtract(pos);
                if (diff.lengthSquared() > 0.1) {
                    float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90);
                    float pitch = (float) -Math.toDegrees(Math.atan2(diff.y,
                        Math.hypot(diff.x, diff.z)));
                    return new Frame(pos, yaw, MathHelper.clamp(pitch, -75, 75));
                }
            }
        }
        return new Frame(shot.pos(), shot.yaw(), shot.pitch());
    }

    private Entity target(MinecraftClient client, String name) {
        if (client.world == null || client.player == null || name == null) return null;
        if (name.equalsIgnoreCase("rin")) return null; // private projection not an NPC
        if (name.equals("none") || name.isBlank()) return null;
        String display = name.replace('_', ' ').toLowerCase(Locale.ROOT);
        return client.world.getOtherEntities(client.player,
                client.player.getBoundingBox().expand(48),
                e -> name.startsWith("tag:")
                    ? e.getCommandTags().contains(name.substring(4))
                    : e.getName().getString().toLowerCase(Locale.ROOT).contains(display))
            .stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(client.player)))
            .orElse(null);
    }

    private static boolean valid(Vec3d pos) {
        return Double.isFinite(pos.x) && Double.isFinite(pos.y)
            && Double.isFinite(pos.z) && Math.abs(pos.x) <= 29_999_984
            && Math.abs(pos.z) <= 29_999_984 && pos.y >= -64 && pos.y <= 1024;
    }

    private static boolean spaceClear(MinecraftClient client, Vec3d pos) {
        if (client.world == null || !valid(pos)) return false;
        for (double dx : new double[]{-0.2, 0.2})
            for (double dy : new double[]{-0.2, 0.2})
                for (double dz : new double[]{-0.2, 0.2}) {
                    BlockPos at = BlockPos.ofFloored(pos.add(dx, dy, dz));
                    if (!client.world.getBlockState(at)
                        .getCollisionShape(client.world, at).isEmpty()) return false;
                }
        return true;
    }

    private static boolean segmentClear(MinecraftClient client, Vec3d from, Vec3d to) {
        if (client.world == null || client.player == null) return false;
        if (!spaceClear(client, to)) return false;
        var result = client.world.raycast(new RaycastContext(from, to,
            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
        return result.getType() == HitResult.Type.MISS;
    }

    private static Frame viewerFrame(MinecraftClient client) {
        return new Frame(client.player.getEyePos(), client.player.getYaw(),
            client.player.getPitch());
    }

    public RinCinematicCamera.Pose renderPose(MinecraftClient client, float tickDelta) {
        if (!active(client) || currentFrame == null || previousFrame == null) return null;
        float partial = MathHelper.clamp(tickDelta, 0, 1);
        Vec3d pos = previousFrame.pos().lerp(currentFrame.pos(), partial);
        float yaw = MathHelper.lerpAngleDegrees(partial, previousFrame.yaw(), currentFrame.yaw());
        float pitch = MathHelper.lerp(partial, previousFrame.pitch(), currentFrame.pitch());
        return new RinCinematicCamera.Pose(pos, yaw, pitch);
    }

    public void clear() {
        if (originalHudHidden != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) client.options.hudHidden = originalHudHidden;
        }
        session = null;
        world = null;
        originalPlayerPos = null;
        originalHudHidden = null;
        shots = List.of();
        index = 0; shotTicks = 0; elapsed = 0;
        currentFrame = null; previousFrame = null; fromFrame = null;
    }
}
