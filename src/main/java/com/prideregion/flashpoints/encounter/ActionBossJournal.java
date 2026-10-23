package com.prideregion.flashpoints.encounter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;
import java.util.zip.CRC32;

/** One bounded, CRC-checked, atomically replaced staging combat journal. */
public final class ActionBossJournal {
    private static final int MAGIC=0x50464143; // PFAC
    private static final int VERSION=1;
    private static final int MAX_BYTES=1024;
    private ActionBossJournal(){}

    public static void save(Path path,ActionBossState.Snapshot value)throws IOException {
        byte[] data;
        try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);out.writeInt(VERSION);
            var key=value.dungeon();out.writeUTF(key.dimension());out.writeLong(key.seed());
            out.writeInt(key.x());out.writeInt(key.y());out.writeInt(key.z());
            uuid(out,value.pokemon());uuid(out,value.operator());
            out.writeInt(value.maxHp());out.writeInt(value.hp());out.writeInt(value.hits());
            out.writeInt(value.phase().ordinal());
            uuid(out,value.capturedBy()==null?new UUID(0,0):value.capturedBy());
            out.writeLong(value.changedAt());out.flush();
            data=bytes.toByteArray();
        }
        CRC32 crc=new CRC32();crc.update(data);
        byte[] full;
        try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)) {
            out.write(data);out.writeLong(crc.getValue());out.flush();full=bytes.toByteArray();
        }
        if(full.length>MAX_BYTES)throw new IOException("Action combat journal too large");
        Path parent=path.toAbsolutePath().getParent();Files.createDirectories(parent);
        Path tmp=Files.createTempFile(parent,"pridefp-action-",".pending");
        try {
            try(FileChannel channel=FileChannel.open(tmp,StandardOpenOption.WRITE)) {
                ByteBuffer buf=ByteBuffer.wrap(full);
                while(buf.hasRemaining())channel.write(buf);
                channel.force(true);
            }
            Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(tmp);}
    }

    public static ActionBossState.Snapshot load(Path path)throws IOException {
        long length=Files.size(path);
        if(length<88 || length>MAX_BYTES)throw new IOException("Invalid action journal size");
        byte[] data=Files.readAllBytes(path);
        CRC32 crc=new CRC32();crc.update(data,0,data.length-8);
        try(var in=new DataInputStream(new ByteArrayInputStream(data))) {
            if(in.readInt()!=MAGIC || in.readInt()!=VERSION)throw new IOException("Invalid action journal version");
            String dim=in.readUTF();long seed=in.readLong();
            int x=in.readInt(),y=in.readInt(),z=in.readInt();
            UUID pokemon=readUuid(in),player=readUuid(in);
            int max=in.readInt(),hp=in.readInt(),hits=in.readInt(),phase=in.readInt();
            if(phase<0 || phase>=ActionBossState.Phase.values().length)
                throw new IOException("Invalid combat phase code");
            UUID capturedBy=readUuid(in);
            if(capturedBy.equals(new UUID(0,0)))capturedBy=null;
            long updated=in.readLong(),checksum=in.readLong();
            if(in.available()!=0 || crc.getValue()!=checksum)
                throw new IOException("Action combat journal damaged or has trailing data");
            try {
                return new ActionBossState.Snapshot(
                    new EncounterLedger.Key(dim,seed,x,y,z),pokemon,player,
                    max,hp,hits,ActionBossState.Phase.values()[phase],capturedBy,updated);
            }catch(IllegalArgumentException ex){throw new IOException("Invalid action combat data",ex);}
        }catch(EOFException ex){throw new IOException("Truncated action combat journal",ex);}
    }
    private static void uuid(DataOutputStream out,UUID uuid)throws IOException {
        out.writeLong(uuid.getMostSignificantBits());out.writeLong(uuid.getLeastSignificantBits());
    }
    private static UUID readUuid(DataInputStream in)throws IOException {
        return new UUID(in.readLong(),in.readLong());
    }
}
