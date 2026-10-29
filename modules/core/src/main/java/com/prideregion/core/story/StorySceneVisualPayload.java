package com.prideregion.core.story;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

/** Clientbound, presentation-only packet. No player commands or battle facts. */
public record StorySceneVisualPayload(UUID session, Action action,
                                      double x, double y, double z)
        implements CustomPayload {
    public enum Action { ENTER, MOVE, FACE, REMOVE }
    public static final Id<StorySceneVisualPayload> ID =
        new Id<>(Identifier.of("pridecore", "scene_visual_v1"));
    public static final PacketCodec<RegistryByteBuf, StorySceneVisualPayload> CODEC =
        PacketCodec.of(StorySceneVisualPayload::write, StorySceneVisualPayload::new);

    public StorySceneVisualPayload {
        Objects.requireNonNull(session);
        Objects.requireNonNull(action);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 29_999_984 || Math.abs(z) > 29_999_984
                || y < -64 || y > 1024)
            throw new IllegalArgumentException("Invalid visual scene coordinates");
    }

    private StorySceneVisualPayload(RegistryByteBuf buf) {
        this(buf.readUuid(), decodeAction(buf.readUnsignedByte()),
             buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    private static Action decodeAction(int value) {
        if (value < 0 || value >= Action.values().length)
            throw new IllegalArgumentException("Invalid scene action");
        return Action.values()[value];
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
