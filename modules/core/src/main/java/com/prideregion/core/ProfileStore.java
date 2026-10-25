package com.prideregion.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.CRC32;

import static java.nio.file.StandardOpenOption.*;

/**
 * One UUID per file, atomic revision-checked writes. Corrupt/unknown records fail closed.
 * Use on Minecraft's main server thread; not an async transactional database.
 */
public final class ProfileStore {
    private static final int MAGIC = 0x50524331; // PRC1
    private static final int FORMAT = 1;
    private static final int LIMIT = 65536;
    private final Path directory;

    public ProfileStore(Path directory) { this.directory = directory; }
    public Path path(UUID uuid) { return directory.resolve(uuid + ".bin"); }

    public ProfileState load(UUID uuid) throws IOException {
        Path file = path(uuid);
        if (!Files.exists(file)) return ProfileState.empty(uuid);
        long length = Files.size(file);
        if (length < 12 || length > LIMIT + 12) throw new IOException("Profile size invalid");
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            int payloadLength = in.readInt();
            if (payloadLength < 1 || payloadLength > LIMIT || payloadLength + 12L != length)
                throw new IOException("Profile length invalid");
            byte[] payload = in.readNBytes(payloadLength);
            if (payload.length != payloadLength) throw new EOFException("Profile truncated");
            long checksum = in.readLong();
            CRC32 crc = new CRC32(); crc.update(payload);
            if (crc.getValue() != checksum) throw new IOException("Profile checksum mismatch");
            try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(payload))) {
                if (data.readInt() != MAGIC || data.readInt() != FORMAT)
                    throw new IOException("Unknown profile format");
                UUID stored = new UUID(data.readLong(), data.readLong());
                if (!stored.equals(uuid)) throw new IOException("UUID mismatch");
                long revision = data.readLong();
                String stage = data.readUTF();
                int count = data.readInt();
                if (count < 0 || count > ProfileState.MAX_FACTS) throw new IOException("Invalid fact count");
                Set<String> facts = new TreeSet<>();
                for (int i = 0; i < count; i++) {
                    if (!facts.add(data.readUTF())) throw new IOException("Duplicate fact");
                }
                if (data.available() != 0) throw new IOException("Unexpected trailing data");
                try {
                    return new ProfileState(uuid, revision, stage, facts);
                } catch (IllegalArgumentException ex) {
                    throw new IOException("Invalid stored profile", ex);
                }
            }
        }
    }

    public boolean save(ProfileState next) throws IOException {
        ProfileState old = load(next.playerId());
        if (next.equals(old)) return false;
        if (next.revision() != old.revision() + 1)
            throw new IOException("Profile revision conflict; refusing overwrite");
        Files.createDirectories(directory);
        Path tmp = directory.resolve(next.playerId() + "." + UUID.randomUUID() + ".tmp");
        try {
            byte[] payload = encode(next);
            CRC32 crc = new CRC32(); crc.update(payload);
            ByteBuffer serialized = ByteBuffer.allocate(4 + payload.length + 8);
            serialized.putInt(payload.length).put(payload).putLong(crc.getValue()).flip();
            try (FileChannel channel = FileChannel.open(tmp, CREATE_NEW, WRITE)) {
                while (serialized.hasRemaining()) channel.write(serialized);
                channel.force(true);
            }
            // No non-atomic fallback: fail safely if the filesystem cannot honor this guarantee.
            Files.move(tmp, path(next.playerId()), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            return true;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static byte[] encode(ProfileState p) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC); out.writeInt(FORMAT);
            out.writeLong(p.playerId().getMostSignificantBits());
            out.writeLong(p.playerId().getLeastSignificantBits());
            out.writeLong(p.revision()); out.writeUTF(p.storyStage());
            out.writeInt(p.facts().size());
            for (String fact : p.facts()) out.writeUTF(fact);
        }
        if (bytes.size() > LIMIT) throw new IOException("Profile exceeds safe limit");
        return bytes.toByteArray();
    }
}
