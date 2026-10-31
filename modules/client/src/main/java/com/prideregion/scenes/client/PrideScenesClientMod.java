package com.prideregion.scenes.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Optional visual receiver. Never sends client->server progression or NPC
 * packets; it can be removed without altering the storyline.
 */
public final class PrideScenesClientMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrideScenesClient");
    private static final RinPreviewProjection PROJECTION = new RinPreviewProjection();
    private static final RinCinematicDialogueOverlay DIALOGUE = new RinCinematicDialogueOverlay();
    private static final RinCinematicCamera CAMERA = new RinCinematicCamera(PROJECTION);

    public static RinCinematicCamera camera() { return CAMERA; }

    @Override public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(
            ClientSceneVisualPayload.ID, ClientSceneVisualPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
            ClientSceneSubtitlePayload.ID, ClientSceneSubtitlePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ClientSceneSubtitlePayload.ID,
            (payload, context) -> context.client().execute(
                () -> {
                    DIALOGUE.accept(context.client(), payload);
                    CAMERA.acceptDialogue(payload);
                }));
        boolean receiverRegistered = ClientPlayNetworking.registerGlobalReceiver(
            ClientSceneVisualPayload.ID, (payload, context) -> {
                LOGGER.info("Received Rin scene visual packet: {} at {},{},{}",
                    payload.action(), payload.x(), payload.y(), payload.z());
                context.client().execute(() -> {
                    if (payload.action() == ClientSceneVisualPayload.Action.REMOVE)
                        DIALOGUE.clearScene(payload.session());
                    PROJECTION.accept(context.client(), payload);
                    CAMERA.acceptVisual(context.client(), payload);
                });
            });
        LOGGER.info("Pride Scenes client initialized: visual channel={} receiverRegistered={}",
            ClientSceneVisualPayload.ID, receiverRegistered);
        ClientTickEvents.END_CLIENT_TICK.register(PROJECTION::tick);
        ClientTickEvents.END_CLIENT_TICK.register(CAMERA::tick);
        ClientTickEvents.END_CLIENT_TICK.register(DIALOGUE::tick);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
            DIALOGUE.draw(net.minecraft.client.MinecraftClient.getInstance(), drawContext));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> {
                PROJECTION.clear();
                DIALOGUE.clear();
                CAMERA.clear();
            }));
    }
}
