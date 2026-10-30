package com.prideregion.scenes.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * A short-lived, strictly client-local Rin actor for private story scenes.
 *
 * Uses Minecraft's actual humanoid player model and a bundled player skin,
 * not a VillagerEntity texture or an online player account. The entity is
 * never registered on the server and cannot award starter/battle/story state.
 */
public final class RinPreviewPlayerEntity extends OtherClientPlayerEntity {
    private static final SkinTextures RIN_SKIN = new SkinTextures(
        Identifier.of("pride_scenes_client", "textures/entity/rin.png"),
        null, null, null, SkinTextures.Model.WIDE, true);

    public RinPreviewPlayerEntity(ClientWorld world, UUID sceneId) {
        super(world, new GameProfile(sceneId, "Rin"));
        setNoGravity(true);
        setSilent(true);
    }

    @Override
    public SkinTextures getSkinTextures() {
        return RIN_SKIN;
    }
}
