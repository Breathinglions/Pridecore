package com.prideregion.core.story;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

/** Clientbound-only, presentation-only dialogue; never accepts player actions. */
public record StorySceneSubtitlePayload(UUID session, String speaker, String line)
        implements CustomPayload {
    public static final Id<StorySceneSubtitlePayload> ID =
        new Id<>(Identifier.of("pridecore", "scene_subtitle_v1"));
    public static final PacketCodec<RegistryByteBuf, StorySceneSubtitlePayload> CODEC =
        PacketCodec.of(StorySceneSubtitlePayload::write, StorySceneSubtitlePayload::new);

    public StorySceneSubtitlePayload {
        Objects.requireNonNull(session);
        Objects.requireNonNull(speaker);
        Objects.requireNonNull(line);
        if (speaker.isBlank() || speaker.length() > 64
                || line.isBlank() || line.length() > 512)
            throw new IllegalArgumentException("Invalid cinematic subtitle");
    }

    private StorySceneSubtitlePayload(RegistryByteBuf buf) {
        this(buf.readUuid(), buf.readString(64), buf.readString(512));
    }
    private void write(RegistryByteBuf buf) {
        buf.writeUuid(session);
        buf.writeString(speaker, 64);
        buf.writeString(line, 512);
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
