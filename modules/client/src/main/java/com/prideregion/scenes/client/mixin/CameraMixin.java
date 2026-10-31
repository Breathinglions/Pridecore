package com.prideregion.scenes.client.mixin;

import com.prideregion.scenes.client.PrideScenesClientMod;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Render-only camera position; vanilla updates before every applied shot.
 * Server/player coordinates and player yaw/pitch are never modified.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void pride$camera(BlockView area, Entity focusedEntity,
                              boolean thirdPerson, boolean inverseView,
                              float tickDelta, CallbackInfo ci) {
        Camera original = (Camera)(Object)this;
        var pose = PrideScenesClientMod.camera().renderPose(
            MinecraftClient.getInstance(), original, tickDelta);
        if (pose == null) return;
        setPos(pose.position().x, pose.position().y, pose.position().z);
        setRotation(pose.yaw(), pose.pitch());
    }
}
