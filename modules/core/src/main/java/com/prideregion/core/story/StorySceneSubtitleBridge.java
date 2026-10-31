package com.prideregion.core.story;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/** Never broadcast private story dialogue; the owner is the sole recipient. */
public final class StorySceneSubtitleBridge {
    private StorySceneSubtitleBridge() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(
            StorySceneSubtitlePayload.ID, StorySceneSubtitlePayload.CODEC);
    }

    public static boolean emit(ServerPlayerEntity viewer,
                               StoryPrivateSceneDirector.Event cue) {
        if (cue.kind() != StoryPrivateSceneDirector.Kind.DIALOGUE
                || !viewer.getUuid().equals(cue.owner())
                || !ServerPlayNetworking.canSend(viewer, StorySceneSubtitlePayload.ID))
            return false;
        ServerPlayNetworking.send(viewer,
            new StorySceneSubtitlePayload(cue.session(), cue.speaker(), cue.text()));
        return true;
    }
}
