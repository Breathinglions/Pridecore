package com.prideregion.scenes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Optional visual receiver. Never sends client->server progression or NPC
 * packets; it can be removed without altering the storyline.
 */
public final class PrideScenesClientMod implements ClientModInitializer {
    private static final RinPreviewProjection PROJECTION = new RinPreviewProjection();

    @Override public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(
            ClientSceneVisualPayload.ID, ClientSceneVisualPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(
            ClientSceneVisualPayload.ID, (payload, context) ->
                context.client().execute(() -> PROJECTION.accept(context.client(), payload)));
        ClientTickEvents.END_CLIENT_TICK.register(PROJECTION::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> PROJECTION.clear()));
    }
}
