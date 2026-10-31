package com.prideregion.scenes.client;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.Objects;
import java.util.UUID;

/** Wire compatible with server camera director; no C2S camera commands. */
public record ClientCameraDirectorPayload(UUID session, boolean stop, String sceneJson)
        implements CustomPayload {
    public static final Id<ClientCameraDirectorPayload> ID =
        new Id<>(Identifier.of("pridecore", "camera_director_v1"));
    public static final PacketCodec<RegistryByteBuf, ClientCameraDirectorPayload> CODEC =
        PacketCodec.of(ClientCameraDirectorPayload::write, ClientCameraDirectorPayload::new);
    public ClientCameraDirectorPayload {
        Objects.requireNonNull(session);
        Objects.requireNonNull(sceneJson);
        if (sceneJson.length() > 24_000)
            throw new IllegalArgumentException("Cinematic exceeds safe wire limit");
    }
    private ClientCameraDirectorPayload(RegistryByteBuf buf) {
        this(buf.readUuid(), buf.readBoolean(), buf.readString(24_000));
    }
    private void write(RegistryByteBuf buf) {
        buf.writeUuid(session);
        buf.writeBoolean(stop);
        buf.writeString(sceneJson, 24_000);
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
