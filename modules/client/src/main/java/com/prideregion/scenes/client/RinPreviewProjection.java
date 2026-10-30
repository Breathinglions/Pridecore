package com.prideregion.scenes.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only staging placeholder for the eventual Cobblemon Rin model.
 * This is a temporary villager silhouette with owner-local, smoothed movement.
 * It is NEVER a server entity, trader, interaction target, or battle NPC.
 */
public final class RinPreviewProjection {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrideRinProjection");
    private ClientWorld world;
    private VillagerEntity actor;
    private UUID session;
    private Vec3d destination;
    private int steps;
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
            VillagerEntity created = new VillagerEntity(EntityType.VILLAGER, current);
            int id = nextVirtualId--;
            while (current.getEntityById(id) != null) id = nextVirtualId--;
            created.setId(id);
            created.setUuid(packet.session());
            created.setPosition(packet.x(), packet.y(), packet.z());
            created.setNoGravity(true);
            created.setSilent(true);
            created.setCustomName(Text.literal("Rin [Staging Preview]"));
            created.setCustomNameVisible(true);
            current.addEntity(created);
            boolean registered = current.getEntityById(id) == created;
            LOGGER.info("Rin projection ENTER: virtual entity id={} registered={} alive={} "
                + "world={} position={},{},{}",
                id, registered, created.isAlive(),
                current.getRegistryKey().getValue(), packet.x(), packet.y(), packet.z());
            if (!registered) {
                LOGGER.error("Rin placeholder was not registered in ClientWorld; "
                    + "no animation can render");
                return;
            }
            actor = created;
            world = current;
            session = packet.session();
            destination = null;
            steps = 0;
            silenceTicks = 0;
            return;
        }
        if (actor == null || world != client.world
                || !packet.session().equals(session)) return;
        silenceTicks = 0;
        switch (packet.action()) {
            case MOVE -> {
                LOGGER.info("Rin projection MOVE received");
                destination = new Vec3d(packet.x(), packet.y(), packet.z());
                steps = 22;
            }
            case FACE -> {
                LOGGER.info("Rin projection FACE received");
                double dx = packet.x() - actor.getX();
                double dz = packet.z() - actor.getZ();
                if (dx * dx + dz * dz > 0.0001) {
                    float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                    actor.setYaw(yaw);
                    actor.setHeadYaw(yaw);
                }
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
            double factor = 1.0D / steps;
            Vec3d next = at.add(destination.subtract(at).multiply(factor));
            actor.setPosition(next);
            steps--;
            if (steps == 0) destination = null;
        }
    }

    public void clear() {
        if (actor != null && world != null)
            world.removeEntity(actor.getId(), Entity.RemovalReason.DISCARDED);
        actor = null;
        session = null;
        world = null;
        destination = null;
        steps = 0;
        silenceTicks = 0;
    }
}
