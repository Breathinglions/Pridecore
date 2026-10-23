package com.prideregion.flashpoints.encounter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;
import java.util.zip.CRC32;

/** Checksummed, versioned and atomically replaced one-run staging ledger. */
public final class BossProgressJournal {
    private static final int MAGIC=0x50464253; // PFBS
    private static final int VERSION=1;
    private static final int MAX_BYTES=1024;

    private BossProgressJournal() {}

    public static void save(Path path,BossProgress.Snapshot snapshot) throws IOException {
        byte[] body;
        try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            var key=snapshot.key();
            out.writeUTF(key.dimension());
            out.writeLong(key.seed());
            out.writeInt(key.x());
            out.writeInt(key.y());
            out.writeInt(key.z());
            out.writeInt(snapshot.stage().ordinal());
            out.writeInt(snapshot.attempts());
            out.writeLong(snapshot.operator().getMostSignificantBits());
            out.writeLong(snapshot.operator().getLeastSignificantBits());
            out.writeLong(snapshot.updatedAtMillis());
            out.flush();
            body=bytes.toByteArray();
        }
        CRC32 checksum=new CRC32();
        checksum.update(body);
        byte[] payload;
        try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)) {
            out.write(body);
            out.writeLong(checksum.getValue());
            out.flush();
            payload=bytes.toByteArray();
        }
        if(payload.length>MAX_BYTES)throw new IOException("Boss journal payload too large");
        Path parent=path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path pending=Files.createTempFile(parent,"pridefp-boss-",".pending");
        try {
            try(FileChannel channel=FileChannel.open(pending,StandardOpenOption.WRITE)) {
                ByteBuffer buffer=ByteBuffer.wrap(payload);
                while(buffer.hasRemaining())channel.write(buffer);
                channel.force(true);
            }
            Files.move(pending,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    public static BossProgress.Snapshot load(Path path) throws IOException {
        long length=Files.size(path);
        if(length<67 || length>MAX_BYTES)
            throw new IOException("Boss journal size invalid");
        byte[] bytes=Files.readAllBytes(path);
        CRC32 checksum=new CRC32();
        checksum.update(bytes,0,bytes.length-8);
        try(var in=new DataInputStream(new ByteArrayInputStream(bytes))) {
            int magic=in.readInt(),version=in.readInt();
            if(magic!=MAGIC || version!=VERSION)
                throw new IOException("Invalid boss journal signature/version");
            String dim=in.readUTF();
            long seed=in.readLong();
            int x=in.readInt(),y=in.readInt(),z=in.readInt();
            int ordinal=in.readInt();
            if(ordinal<0 || ordinal>=BossProgress.Stage.values().length)
                throw new IOException("Unknown boss stage");
            int attempts=in.readInt();
            UUID operator=new UUID(in.readLong(),in.readLong());
            long timestamp=in.readLong();
            long crc=in.readLong();
            if(in.available()!=0 || checksum.getValue()!=crc)
                throw new IOException("Boss journal checksum or trailing-data error");
            try {
                return new BossProgress.Snapshot(
                    new EncounterLedger.Key(dim,seed,x,y,z),
                    BossProgress.Stage.values()[ordinal],attempts,operator,timestamp);
            } catch(IllegalArgumentException ex) {
                throw new IOException("Invalid boss journal fields",ex);
            }
        } catch(EOFException ex) {
            throw new IOException("Truncated boss journal",ex);
        }
    }
}
