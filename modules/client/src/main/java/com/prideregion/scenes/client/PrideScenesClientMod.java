package com.prideregion.scenes.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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
    private static final CinematicHudPresentation HUD = new CinematicHudPresentation();
    private static final RinCinematicCamera CAMERA = new RinCinematicCamera(PROJECTION);
    private static final ClientCameraDirector DIRECTOR = new ClientCameraDirector(PROJECTION);

    public static RinCinematicCamera camera() { return CAMERA; }
    public static CinematicHudPresentation hudPresentation() { return HUD; }
    public static boolean cinematicActive(MinecraftClient client) {
        return DIRECTOR.active(client) || CAMERA.suppressHud(client);
    }
    public static RinCinematicCamera.Pose directorPose(MinecraftClient client, float partial) {
        return DIRECTOR.renderPose(client, partial);
    }

    /** Draw Pride dialogue even with vanilla HUD suppressed for cinema. */
    public static void renderCinematicDialogue(MinecraftClient client, DrawContext context) {
        HUD.drawBars(context);
        DIALOGUE.draw(client, context, true);
    }

    @Override public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(
            ClientCameraDirectorPayload.ID, ClientCameraDirectorPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ClientCameraDirectorPayload.ID,
            (payload, context) -> context.client().execute(() -> {
                if (!payload.stop()) CAMERA.clear();
                DIRECTOR.accept(context.client(), payload);
            }));
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
        ClientTickEvents.END_CLIENT_TICK.register(DIRECTOR::tick);
        ClientTickEvents.END_CLIENT_TICK.register(DIALOGUE::tick);
        ClientTickEvents.END_CLIENT_TICK.register(HUD::tick);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
            DIALOGUE.draw(MinecraftClient.getInstance(), drawContext, false));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> {
                PROJECTION.clear();
                DIALOGUE.clear();
                CAMERA.clear();
                DIRECTOR.clear();
            }));
    }
}
