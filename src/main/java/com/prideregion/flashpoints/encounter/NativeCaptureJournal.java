package com.prideregion.flashpoints.encounter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * One exact Cobblemon Pokemon UUID armed for one specific dungeon boss capture.
 * Never edits Pokemon or awards items. Separate from operator-only simulation.
 */
public final class NativeCaptureJournal {
    private static final int MAGIC=0x50464341; // PFCA
    private static final int VERSION=1;
    private static final int MAX_BYTES=1024;

    public enum Stage { ARMED, CAPTURED }

    public record Snapshot(EncounterLedger.Key dungeon,UUID pokemon,UUID operator,
                           Stage stage,UUID capturedBy,long capturedAt) {
        public Snapshot {
            Objects.requireNonNull(dungeon);
            Objects.requireNonNull(pokemon);
            Objects.requireNonNull(operator);
            Objects.requireNonNull(stage);
            if(stage==Stage.ARMED && (capturedBy!=null || capturedAt!=0L)
                || stage==Stage.CAPTURED && (capturedBy==null || capturedAt<=0))
                throw new IllegalArgumentException("Invalid capture progression");
        }
        public Snapshot capture(UUID pokemonId,UUID playerId,long timestamp) {
            if(stage!=Stage.ARMED || !pokemon.equals(pokemonId) ||
                    !operator.equals(playerId) || timestamp<=0)
                throw new IllegalStateException("Capture event does not match armed boss/player");
            return new Snapshot(dungeon,pokemon,operator,Stage.CAPTURED,playerId,timestamp);
        }
    }

    private NativeCaptureJournal(){}

    public static void save(Path path,Snapshot value) throws IOException {
        byte[] body;
        try(var data=new ByteArrayOutputStream();var out=new DataOutputStream(data)) {
            out.writeInt(MAGIC);out.writeInt(VERSION);
            out.writeUTF(value.dungeon().dimension());
            out.writeLong(value.dungeon().seed());
            out.writeInt(value.dungeon().x());out.writeInt(value.dungeon().y());out.writeInt(value.dungeon().z());
            writeUuid(out,value.pokemon());writeUuid(out,value.operator());
            out.writeInt(value.stage().ordinal());
            writeUuid(out,value.capturedBy()==null ? new UUID(0,0) : value.capturedBy());
            out.writeLong(value.capturedAt());out.flush();
            body=data.toByteArray();
        }
        CRC32 crc=new CRC32();crc.update(body);
        byte[] payload;
        try(var data=new ByteArrayOutputStream();var out=new DataOutputStream(data)){
            out.write(body);out.writeLong(crc.getValue());out.flush();payload=data.toByteArray();
        }
        if(payload.length>MAX_BYTES)throw new IOException("Capture record too large");
        Path parent=path.toAbsolutePath().getParent();Files.createDirectories(parent);
        Path temp=Files.createTempFile(parent,"pridefp-capture-",".pending");
        try {
            try(FileChannel channel=FileChannel.open(temp,StandardOpenOption.WRITE)){
                ByteBuffer buffer=ByteBuffer.wrap(payload);
                while(buffer.hasRemaining())channel.write(buffer);
                channel.force(true);
            }
            Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(temp);}
    }

    public static Snapshot load(Path path) throws IOException {
        long length=Files.size(path);
        if(length<90 || length>MAX_BYTES)throw new IOException("Capture ledger size invalid");
        byte[] data=Files.readAllBytes(path);
        CRC32 crc=new CRC32();crc.update(data,0,data.length-8);
        try(var in=new DataInputStream(new ByteArrayInputStream(data))) {
            if(in.readInt()!=MAGIC || in.readInt()!=VERSION)
                throw new IOException("Capture ledger magic/version mismatch");
            String dim=in.readUTF();long seed=in.readLong();
            int x=in.readInt(),y=in.readInt(),z=in.readInt();
            UUID pokemon=readUuid(in),operator=readUuid(in);
            int stageCode=in.readInt();
            if(stageCode<0 || stageCode>=Stage.values().length)
                throw new IOException("Unrecognized capture state");
            UUID capturedBy=readUuid(in);
            if(capturedBy.equals(new UUID(0,0)))capturedBy=null;
            long capturedAt=in.readLong();
            long recordedCrc=in.readLong();
            if(in.available()!=0 || recordedCrc!=crc.getValue())
                throw new IOException("Capture ledger CRC/trailing data error");
            try {
                return new Snapshot(new EncounterLedger.Key(dim,seed,x,y,z),pokemon,operator,
                    Stage.values()[stageCode],capturedBy,capturedAt);
            } catch(IllegalArgumentException ex){
                throw new IOException("Malformed capture state",ex);
            }
        } catch(EOFException ex){throw new IOException("Truncated capture state",ex);}
    }
    private static void writeUuid(DataOutputStream out,UUID u) throws IOException {
        out.writeLong(u.getMostSignificantBits());out.writeLong(u.getLeastSignificantBits());
    }
    private static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(),in.readLong());
    }
}
