package com.prideregion.scenes.client.mixin;

import com.prideregion.scenes.client.PrideScenesClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide hotbar, crosshair, chat and third-party HUD for an owner-local cutscene,
 * without changing the player's F1 option or suppressing native GUI screens. */
@Mixin(InGameHud.class)
public abstract class CinematicHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pride$cinematicHud(DrawContext context, RenderTickCounter ticks,
                                    CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!PrideScenesClientMod.cinematicActive(client)) return;
        PrideScenesClientMod.renderCinematicDialogue(client, context);
        ci.cancel();
    }
}
