package com.prideregion.flashpoints.encounter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;
import java.util.zip.CRC32;

/** Small, checksummed, atomic staging journal for real BATTLE_VICTORY outcomes. */
public final class NativeBattleJournal {
    private static final int MAGIC=0x50464256; // PFBV
    private static final int VERSION=1;
    private static final int MAX_BYTES=1024;
    private NativeBattleJournal(){}

    public static void save(Path path,NativeBattleProgress.Snapshot value) throws IOException {
        byte[] body;
        try(var memory=new ByteArrayOutputStream();var out=new DataOutputStream(memory)){
            out.writeInt(MAGIC);out.writeInt(VERSION);
            var k=value.dungeon();out.writeUTF(k.dimension());out.writeLong(k.seed());
            out.writeInt(k.x());out.writeInt(k.y());out.writeInt(k.z());
            uuid(out,value.bossPokemon());uuid(out,value.player());
            out.writeInt(value.stage().ordinal());
            uuid(out,value.battleId()==null?new UUID(0,0):value.battleId());
            out.writeLong(value.resolvedAt());
            out.flush();body=memory.toByteArray();
        }
        CRC32 crc=new CRC32();crc.update(body);
        byte[] full;
        try(var memory=new ByteArrayOutputStream();var out=new DataOutputStream(memory)){
            out.write(body);out.writeLong(crc.getValue());out.flush();full=memory.toByteArray();
        }
        if(full.length>MAX_BYTES)throw new IOException("Battle record size exceeded");
        Path parent=path.toAbsolutePath().getParent();Files.createDirectories(parent);
        Path pending=Files.createTempFile(parent,"pridefp-battle-",".pending");
        try {
            try(FileChannel channel=FileChannel.open(pending,StandardOpenOption.WRITE)) {
                ByteBuffer buffer=ByteBuffer.wrap(full);
                while(buffer.hasRemaining())channel.write(buffer);
                channel.force(true);
            }
            Files.move(pending,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(pending);}
    }
    public static NativeBattleProgress.Snapshot load(Path path) throws IOException {
        long size=Files.size(path);
        if(size<80 || size>MAX_BYTES)throw new IOException("Invalid native battle journal size");
        byte[] data=Files.readAllBytes(path);
        CRC32 crc=new CRC32();crc.update(data,0,data.length-8);
        try(var in=new DataInputStream(new ByteArrayInputStream(data))){
            if(in.readInt()!=MAGIC || in.readInt()!=VERSION)
                throw new IOException("Invalid native battle journal version");
            String dim=in.readUTF();long seed=in.readLong();
            int x=in.readInt(),y=in.readInt(),z=in.readInt();
            UUID boss=readUuid(in),player=readUuid(in);
            int ordinal=in.readInt();
            if(ordinal<0 || ordinal>=NativeBattleProgress.Stage.values().length)
                throw new IOException("Invalid battle stage");
            UUID battle=readUuid(in);
            if(battle.equals(new UUID(0,0)))battle=null;
            long timestamp=in.readLong(),checksum=in.readLong();
            if(in.available()!=0 || checksum!=crc.getValue())
                throw new IOException("Native battle journal CRC/trailing data mismatch");
            try {
                return new NativeBattleProgress.Snapshot(new EncounterLedger.Key(dim,seed,x,y,z),
                    boss,player,NativeBattleProgress.Stage.values()[ordinal],battle,timestamp);
            } catch(IllegalArgumentException ex){throw new IOException("Invalid journal contents",ex);}
        }catch(EOFException ex){throw new IOException("Truncated battle journal",ex);}
    }
    private static void uuid(DataOutputStream out,UUID id) throws IOException {
        out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits());
    }
    private static UUID readUuid(DataInputStream in)throws IOException {
        return new UUID(in.readLong(),in.readLong());
    }
}
