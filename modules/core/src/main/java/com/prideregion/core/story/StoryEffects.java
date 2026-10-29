package com.prideregion.core.story;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional, allowlisted particle overrides for Module 02 presentation.
 * Override IDs are resolved only if the particle type exists on the server.
 * Missing mods or invalid IDs always fall back to the bundled vanilla effect.
 * Overrides never participate in battle authentication or story progression.
 */
public final class StoryEffects {
    private static final Set<String> KEYS = Set.of(
        "finale.end_rod", "finale.totem", "battle.electric", "battle.enchant");
    private static final Map<String, String> OVERRIDES = new ConcurrentHashMap<>();

    private StoryEffects() {}

    public static void reset() { OVERRIDES.clear(); }

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
        return true;
    }

    public static void emit(ServerWorld world, String key, ParticleEffect fallback,
                            double x, double y, double z, int count,
                            double spreadX, double spreadY, double spreadZ, double speed) {
        ParticleEffect effect = fallback;
        String override = OVERRIDES.get(key);
        if (override != null) {
            Identifier id = Identifier.tryParse(override);
            if (id != null) {
                ParticleType<?> type = Registries.PARTICLE_TYPE.get(id);
                if (type instanceof SimpleParticleType candidate) effect = candidate;
            }
        }
        world.spawnParticles(effect, x, y, z, count, spreadX, spreadY, spreadZ, speed);
    }
}
