package com.prideregion.scenes.client.mixin;

import com.prideregion.scenes.client.PrideScenesClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * During an active cinematic shot, avoid floating first-person arms/items.
 * Does not modify inventory, held item, player pose or any world state.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void pride$noFirstPersonHands(Camera camera, float tickDelta,
                                         Matrix4f matrix, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen == null
            && PrideScenesClientMod.camera().showingShot()) ci.cancel();
    }
}
