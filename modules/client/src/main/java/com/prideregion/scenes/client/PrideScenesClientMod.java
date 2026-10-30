package com.prideregion.scenes.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

    @Override public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(
            ClientSceneVisualPayload.ID, ClientSceneVisualPayload.CODEC);
        boolean receiverRegistered = ClientPlayNetworking.registerGlobalReceiver(
            ClientSceneVisualPayload.ID, (payload, context) -> {
                LOGGER.info("Received Rin scene visual packet: {} at {},{},{}",
                    payload.action(), payload.x(), payload.y(), payload.z());
                context.client().execute(() -> PROJECTION.accept(context.client(), payload));
            });
        LOGGER.info("Pride Scenes client initialized: visual channel={} receiverRegistered={}",
            ClientSceneVisualPayload.ID, receiverRegistered);
        ClientTickEvents.END_CLIENT_TICK.register(PROJECTION::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> PROJECTION.clear()));
    }
}
