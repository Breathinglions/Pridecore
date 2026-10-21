package com.prideregion.flashpoints;

import com.prideregion.flashpoints.layout.DungeonLayout;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * Write-ahead record for ONE disposable staging dungeon.
 *
 * Persist the complete block layout before the first world write. The record
 * intentionally stays on disk until every expected block is scanned during
 * cleanup, including across crashes/restarts.
 */
public final class StagingJournal {
    private static final int MAGIC = 0x50524650; // PRFP
    private static final int VERSION = 2;
    private static final int MAX_FILE = 4_000_000;
    private static final int MAX_VOLUME = 2_000_000;

    private StagingJournal() {}

    public record Snapshot(String dimension, UUID owner, int x, int y, int z, DungeonLayout layout) {
        public Snapshot {
            if (dimension == null || dimension.isBlank() || dimension.length() > 256)
                throw new IllegalArgumentException("invalid dimension");
            if (owner == null || layout == null) throw new IllegalArgumentException("missing owner/layout");
        }
    }

    public static void save(Path path, Snapshot snapshot) throws IOException {
        if (Files.exists(path)) throw new FileAlreadyExistsException("A recovery journal already exists: " + path);
        byte[] body;
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {
            DungeonLayout layout = snapshot.layout();
            byte[] blocks = layout.blocks();
            if (blocks.length > MAX_VOLUME) throw new IOException("Dungeon volume too large");
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeUTF(snapshot.dimension());
            out.writeUTF(snapshot.owner().toString());
            out.writeInt(snapshot.x());
            out.writeInt(snapshot.y());
            out.writeInt(snapshot.z());
            out.writeLong(layout.seed());
            out.writeInt(layout.width());
            out.writeInt(layout.height());
            out.writeInt(layout.length());
            out.writeInt(layout.entranceX());
            out.writeInt(layout.entranceZ());
            out.writeInt(layout.bossX());
            out.writeInt(layout.bossZ());
            out.writeInt(blocks.length);
            out.write(blocks);
            out.flush();
            body = b.toByteArray();
        }
        CRC32 crc = new CRC32();
        crc.update(body);
        byte[] bytes;
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {
            out.write(body);
            out.writeLong(crc.getValue());
            bytes = b.toByteArray();
        }
        if (bytes.length > MAX_FILE) throw new IOException("Journal size limit exceeded");
        Path dir = path.getParent();
        if (dir != null) Files.createDirectories(dir);
        Path tmp = path.resolveSibling(path.getFileName() + ".pending");
        try {
            try (FileChannel channel = FileChannel.open(tmp,
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public static Snapshot load(Path path) throws IOException {
        long size = Files.size(path);
        if (size < 80 || size > MAX_FILE) throw new IOException("Unexpected recovery journal size");
        byte[] bytes = Files.readAllBytes(path);
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, bytes.length - 8);
        long actualCrc;
        try (DataInputStream tail = new DataInputStream(
                new ByteArrayInputStream(bytes, bytes.length - 8, 8))) {
            actualCrc = tail.readLong();
        }
        if (crc.getValue() != actualCrc) throw new IOException("Recovery journal CRC mismatch");

        try (DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(bytes, 0, bytes.length - 8))) {
            if (in.readInt() != MAGIC) throw new IOException("Wrong journal signature");
            if (in.readInt() != VERSION) throw new IOException("Unsupported journal version");
            String dimension = in.readUTF();
            UUID owner;
            try { owner = UUID.fromString(in.readUTF()); }
            catch (IllegalArgumentException e) { throw new IOException("Invalid owner UUID", e); }
            int x=in.readInt(), y=in.readInt(), z=in.readInt();
            long seed=in.readLong();
            int width=in.readInt(), height=in.readInt(), length=in.readInt();
            int entryX=in.readInt(), entryZ=in.readInt(), bossX=in.readInt(), bossZ=in.readInt();
            int total=in.readInt();
            if (width <= 0 || height <= 0 || length <= 0 || width > 512 || length > 512
                || height > 128 || (long)width * length * height != total
                || total > MAX_VOLUME || total < 1)
                throw new IOException("Invalid layout dimensions");
            if (entryX < 0 || entryX >= width || bossX < 0 || bossX >= width
                || entryZ < 0 || entryZ >= length || bossZ < 0 || bossZ >= length)
                throw new IOException("Invalid room coordinates");
            byte[] blocks = new byte[total];
            in.readFully(blocks);
            if (in.available() != 0) throw new IOException("Trailing journal bytes");
            for (byte block : blocks)
                if (Byte.toUnsignedInt(block) > 10) throw new IOException("Invalid block palette value");
            DungeonLayout layout = new DungeonLayout(seed,width,height,length,
                    entryX,entryZ,bossX,bossZ,blocks);
            return new Snapshot(dimension,owner,x,y,z,layout);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid recovery journal",e);
        }
    }

    public static void remove(Path path) throws IOException {
        Files.delete(path);
    }
}
