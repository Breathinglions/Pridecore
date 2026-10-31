package com.prideregion.scenes.client;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

/** Wire-compatible with server scene_subtitle_v1; strictly clientbound. */
public record ClientSceneSubtitlePayload(UUID session, String speaker, String line)
        implements CustomPayload {
    public static final Id<ClientSceneSubtitlePayload> ID =
        new Id<>(Identifier.of("pridecore", "scene_subtitle_v1"));
    public static final PacketCodec<RegistryByteBuf, ClientSceneSubtitlePayload> CODEC =
        PacketCodec.of(ClientSceneSubtitlePayload::write, ClientSceneSubtitlePayload::new);
    public ClientSceneSubtitlePayload {
        Objects.requireNonNull(session);
        Objects.requireNonNull(speaker);
        Objects.requireNonNull(line);
        if (speaker.isBlank() || speaker.length() > 64
                || line.isBlank() || line.length() > 512)
            throw new IllegalArgumentException("Invalid cinematic subtitle");
    }
    private ClientSceneSubtitlePayload(RegistryByteBuf buf) {
        this(buf.readUuid(), buf.readString(64), buf.readString(512));
    }
    private void write(RegistryByteBuf buf) {
        buf.writeUuid(session);
        buf.writeString(speaker, 64);
        buf.writeString(line, 512);
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
