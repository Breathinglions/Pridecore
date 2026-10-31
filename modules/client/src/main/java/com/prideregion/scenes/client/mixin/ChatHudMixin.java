package com.prideregion.scenes.client.mixin;

import com.prideregion.scenes.client.PrideScenesClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide chat only while the scripted camera runs, keeping message history. */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pride$hideChatDuringScene(DrawContext context, int tick,
            int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (PrideScenesClientMod.camera().suppressHud(MinecraftClient.getInstance()))
            ci.cancel();
    }
}
