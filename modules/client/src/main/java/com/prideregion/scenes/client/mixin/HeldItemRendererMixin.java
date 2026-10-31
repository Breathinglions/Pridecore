package com.prideregion.scenes.client.mixin;

import com.prideregion.scenes.client.PrideScenesClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Safety net for shader/renderer paths that bypass GameRenderer.renderHand.
 * Suppresses only the first-person arms/items during this owner's cutscene. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"), cancellable = true)
    private void pride$hideFirstPersonItem(float tickDelta, MatrixStack matrices,
            VertexConsumerProvider.Immediate consumers, ClientPlayerEntity player,
            int light, CallbackInfo ci) {
        if (PrideScenesClientMod.cinematicActive(MinecraftClient.getInstance()))
            ci.cancel();
    }
}
