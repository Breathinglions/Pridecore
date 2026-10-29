package com.prideregion.core.story;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional, allowlisted particle overrides for Module 02 presentation.
 * Override IDs are resolved only if the particle type exists on the server.
 * Missing mods or invalid IDs always fall back to the bundled vanilla effect.
 * Overrides never participate in battle authentication or story progression.
 */
public final class StoryEffects {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrideStoryEffects");
    private static final Set<String> FAILED_OVERRIDES = ConcurrentHashMap.newKeySet();
    private static final Set<String> KEYS = Set.of(
        "finale.end_rod", "finale.totem", "finale.conduit",
        "finale.shield", "battle.electric", "battle.enchant",
        "battle.rin", "battle.sable", "battle.cael");
    private static final Map<String, String> OVERRIDES = new ConcurrentHashMap<>();

    private StoryEffects() {}

    public static void reset() { OVERRIDES.clear(); FAILED_OVERRIDES.clear(); }

    public static Set<String> keys() { return KEYS; }

    public static boolean configure(String key, String particleId) {
        if (!KEYS.contains(key)) return false;
        if (particleId == null || particleId.isBlank()) {
            OVERRIDES.remove(key);
            return true;
        }
        Identifier id = Identifier.tryParse(particleId);
        if (id == null || !Registries.PARTICLE_TYPE.containsId(id)
                || !(Registries.PARTICLE_TYPE.get(id) instanceof SimpleParticleType))
            return false;
        OVERRIDES.put(key, particleId);
        FAILED_OVERRIDES.remove(key);
        return true;
    }

    public static void emit(ServerWorld world, String key, ParticleEffect fallback,
                            double x, double y, double z, int count,
                            double spreadX, double spreadY, double spreadZ, double speed) {
        count = StoryPresentationConfig.current().particles(count);
        if (count <= 0) return; // Cosmetics never interfere with progression.
        ParticleEffect effect = fallback;
        String override = OVERRIDES.get(key);
        if (override != null && !FAILED_OVERRIDES.contains(key)) {
            Identifier id = Identifier.tryParse(override);
            if (id != null && Registries.PARTICLE_TYPE.containsId(id)) {
                ParticleType<?> type = Registries.PARTICLE_TYPE.get(id);
                if (type instanceof SimpleParticleType candidate) effect = candidate;
            }
        }
        try {
            world.spawnParticles(effect, x, y, z, count, spreadX, spreadY, spreadZ, speed);
        } catch (RuntimeException presentationFailure) {
            // Particles must never roll back or interrupt a verified story action.
            if (effect != fallback) {
                if (FAILED_OVERRIDES.add(key))
                    LOGGER.warn("Modded particle override {} ({}) failed; disabling until next reload",
                        key, override, presentationFailure);
                try {
                    world.spawnParticles(fallback, x, y, z, count,
                        spreadX, spreadY, spreadZ, speed);
                } catch (RuntimeException ignored) {
                    // The story transition has already been committed.
                }
            }
        }
    }
}
