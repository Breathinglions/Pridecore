package com.prideregion.scenes.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only staging version of Rin using his uploaded Minecraft player skin.
 * This temporary humanoid is scoped to one viewer with smoothed gait/yaw.
 * It is NEVER a server entity, trainer, progression trigger, or battle NPC.
 */
public final class RinPreviewProjection {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrideRinProjection");
    private ClientWorld world;
    private RinPreviewPlayerEntity actor;
    private UUID session;
    private Vec3d destination;
    private Vec3d faceTarget;
    private int steps;
    private static final int WALK_STEPS = 22;
    private static final float MAX_TURN_PER_TICK = 15.0F;
    private int silenceTicks;
    private int nextVirtualId = -2_000_000;
    private static final int MAX_SILENCE_TICKS = 220;

    public void accept(MinecraftClient client, ClientSceneVisualPayload packet) {
        if (client.world == null || client.player == null) { clear(); return; }
        if (world != null && world != client.world) clear();
        if (packet.action() == ClientSceneVisualPayload.Action.ENTER) {
            // Require an entry close to the viewer; server checks world/owner
            // already, but the optional client must reject absurd coordinates.
            if (client.player.squaredDistanceTo(packet.x(), packet.y(), packet.z()) > 96 * 96) {
                LOGGER.warn("Ignored Rin ENTER: client viewer farther than 96 blocks from {},{},{}",
                    packet.x(), packet.y(), packet.z());
                return;
            }
            clear();
            ClientWorld current = client.world;
            RinPreviewPlayerEntity created = new RinPreviewPlayerEntity(current, packet.session());
            int id = nextVirtualId--;
            while (current.getEntityById(id) != null) id = nextVirtualId--;
            created.setId(id);
            created.setUuid(packet.session());
            created.setPosition(packet.x(), packet.y(), packet.z());
            created.setNoGravity(true);
            created.setSilent(true);
            created.setCustomName(Text.literal("Rin"));
            created.setCustomNameVisible(true);
            current.addEntity(created);
            boolean registered = current.getEntityById(id) == created;
            LOGGER.info("Rin projection ENTER: virtual entity id={} registered={} alive={} "
                + "world={} position={},{},{}",
                id, registered, created.isAlive(),
                current.getRegistryKey().getValue(), packet.x(), packet.y(), packet.z());
            if (!registered) {
                LOGGER.error("Rin player-skin preview was not registered in ClientWorld; "
                    + "no animation can render");
                return;
            }
            actor = created;
            world = current;
            session = packet.session();
            destination = null;
            faceTarget = null;
            steps = 0;
            silenceTicks = 0;
            return;
        }
        if (actor == null || world != client.world
                || !packet.session().equals(session)) return;
        silenceTicks = 0;
        switch (packet.action()) {
            case MOVE -> {
                destination = new Vec3d(packet.x(), packet.y(), packet.z());
                faceTarget = null; // The next FACE packet names the final look target.
                steps = WALK_STEPS;
                LOGGER.info("Rin projection walking for {} ticks to {}", steps, destination);
            }
            case FACE -> {
                faceTarget = new Vec3d(packet.x(), packet.y(), packet.z());
                // FACE is a cardinal or world facing point from the trusted
                // server anchors. Turn after the walk so Rin does not
                // snap back toward the walking direction.
                LOGGER.info("Rin projection will face {} after walking", faceTarget);
                if (destination != null
                        && faceTarget.squaredDistanceTo(destination) < 0.01)
                    LOGGER.warn("Rin FACE target equals MOVE destination: "
                        + "set scene.lab.face=north (or another valid heading), "
                        + "not Rin's stopping position");
            }
            case REMOVE -> clear();
            case ENTER -> { /* handled above */ }
        }
    }

    public void tick(MinecraftClient client) {
        if (actor == null) return;
        if (client.world != world || client.player == null
                || !actor.isAlive() || ++silenceTicks > MAX_SILENCE_TICKS) {
            LOGGER.warn("Rin client projection cleared early: worldChanged={} playerMissing={} "
                + "alive={} silentTicks={}",
                client.world != world, client.player == null,
                actor.isAlive(), silenceTicks);
            clear();
            return;
        }
        if (destination != null && steps > 0) {
            Vec3d at = actor.getPos();
            // Tick-rate stable linear interpolation, instead of sliding a
            // no-gravity virtual actor while its vanilla walking limbs idle.
            Vec3d next = at.add(destination.subtract(at).multiply(1.0D / steps));
            Vec3d walked = next.subtract(at);
            actor.setPosition(next);
            double horizontal = Math.hypot(walked.x, walked.z);
            if (horizontal > 0.00001D) {
                rotateToward(walked.x, walked.z);
                // LivingEntityRenderer reads limbAnimator, not just position.
                // Feed real horizontal stride distance on the local preview.
                actor.limbAnimator.updateLimbs(
                    (float) Math.min(1.0D, horizontal * 4.0D), 1.0F);
            } else {
                actor.limbAnimator.updateLimbs(0.0F, 1.0F);
            }
            steps--;
            if (steps == 0) {
                destination = null;
                LOGGER.info("Rin projection finished walking; awaiting final facing");
            }
        } else {
            // Decelerate legs to a resting stance after the last step.
            actor.limbAnimator.updateLimbs(0.0F, 1.0F);
            if (faceTarget != null) {
                double dx = faceTarget.x - actor.getX();
                double dz = faceTarget.z - actor.getZ();
                if (dx * dx + dz * dz > 0.01D)
                    rotateToward(dx, dz);
            }
        }
    }

    private void rotateToward(double dx, double dz) {
        if (dx * dx + dz * dz < 0.0001D) return;
        float desired = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float current = actor.getYaw();
        float change = MathHelper.wrapDegrees(desired - current);
        float smooth = current + MathHelper.clamp(change,
            -MAX_TURN_PER_TICK, MAX_TURN_PER_TICK);
        // Body, head and root yaw must agree for a humanoid player render.
        actor.setYaw(smooth);
        actor.setBodyYaw(smooth);
        actor.setHeadYaw(smooth);
    }

    /** Read-only camera anchor, scoped to the current private scene token. */
    public boolean hasActor(UUID expectedSession) {
        return expectedSession != null && expectedSession.equals(session)
            && actor != null && actor.isAlive() && world != null;
    }

    public Vec3d actorPosition(UUID expectedSession) {
        return hasActor(expectedSession) ? actor.getPos() : null;
    }

    public void clear() {
        if (actor != null && world != null)
            world.removeEntity(actor.getId(), Entity.RemovalReason.DISCARDED);
        actor = null;
        session = null;
        world = null;
        destination = null;
        faceTarget = null;
        steps = 0;
        silenceTicks = 0;
    }
}
