package com.prideregion.scenes.client;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Wire-compatible with the server's StorySceneVisualPayload v1. The client
 * never replies with a scene result or a progression authorization.
 */
public record ClientSceneVisualPayload(UUID session, Action action,
                                       double x, double y, double z)
        implements CustomPayload {
    public enum Action { ENTER, MOVE, FACE, REMOVE }
    public static final Id<ClientSceneVisualPayload> ID =
        new Id<>(Identifier.of("pridecore", "scene_visual_v1"));
    public static final PacketCodec<RegistryByteBuf, ClientSceneVisualPayload> CODEC =
        PacketCodec.of(ClientSceneVisualPayload::write, ClientSceneVisualPayload::new);

    public ClientSceneVisualPayload {
        Objects.requireNonNull(session);
        Objects.requireNonNull(action);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 29_999_984 || Math.abs(z) > 29_999_984
                || y < -64 || y > 1024)
            throw new IllegalArgumentException("Bad cinematic location");
    }
    private ClientSceneVisualPayload(RegistryByteBuf buf) {
        this(buf.readUuid(), decodeAction(buf.readUnsignedByte()),
            buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    private static Action decodeAction(int index) {
        if (index < 0 || index >= Action.values().length)
            throw new IllegalArgumentException("Bad scene action");
        return Action.values()[index];
    }
    private void write(RegistryByteBuf buf) {
        buf.writeUuid(session);
        buf.writeByte(action.ordinal());
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
