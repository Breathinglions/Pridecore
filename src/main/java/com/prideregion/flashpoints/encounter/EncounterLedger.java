package com.prideregion.flashpoints.encounter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Durable, strictly bounded staging-only entity UUID associations.
 * This class never spawns, captures, edits or removes an entity.
 */
public final class EncounterLedger {
    private static final int MAGIC=0x5046454E; // PFEN
    private static final int VERSION=1;
    private static final int MAX_BYTES=4096;
    private EncounterLedger() {}

    public record Key(String dimension,long seed,int x,int y,int z) {
        public Key {
            if(dimension==null || dimension.isBlank() || dimension.length()>256 ||
                    !dimension.contains(":"))throw new IllegalArgumentException("Invalid dimension ID");
        }
    }

    public record Snapshot(Key key,Map<Integer,UUID> bindings) {
        public Snapshot {
            Objects.requireNonNull(key);
            Objects.requireNonNull(bindings);
            TreeMap<Integer,UUID> copy=new TreeMap<>();
            HashSet<UUID> used=new HashSet<>();
            if(bindings.size()>16)throw new IllegalArgumentException("Too many room bindings");
            bindings.forEach((room,uuid)->{
                if(room==null || room<0 || room>=16 || uuid==null || !used.add(uuid))
                    throw new IllegalArgumentException("Invalid or duplicate room/entity binding");
                copy.put(room,uuid);
            });
            bindings=Collections.unmodifiableMap(copy);
        }
        public Snapshot bind(int room,UUID uuid) {
            if(bindings.containsKey(room))throw new IllegalArgumentException("Room already tracked");
            HashMap<Integer,UUID> changed=new HashMap<>(bindings);
            changed.put(room,uuid);
            return new Snapshot(key,changed);
        }
        public Snapshot forget(int room) {
            if(!bindings.containsKey(room))throw new IllegalArgumentException("Room not tracked");
            HashMap<Integer,UUID> changed=new HashMap<>(bindings);
            changed.remove(room);
            return new Snapshot(key,changed);
        }
    }

    public static void save(Path path,Snapshot snapshot) throws IOException {
        Objects.requireNonNull(path);Objects.requireNonNull(snapshot);
        byte[] body;
        try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);out.writeInt(VERSION);
            out.writeUTF(snapshot.key().dimension());
            out.writeLong(snapshot.key().seed());
            out.writeInt(snapshot.key().x());
            out.writeInt(snapshot.key().y());
            out.writeInt(snapshot.key().z());
            out.writeInt(snapshot.bindings().size());
            for(var entry:snapshot.bindings().entrySet()) {
                out.writeInt(entry.getKey());
                out.writeLong(entry.getValue().getMostSignificantBits());
                out.writeLong(entry.getValue().getLeastSignificantBits());
            }
            out.flush();
            body=bytes.toByteArray();
        }
        CRC32 check=new CRC32();check.update(body);
        byte[] payload;
        try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)) {
            out.write(body);out.writeLong(check.getValue());out.flush();
            payload=bytes.toByteArray();
        }
        if(payload.length>MAX_BYTES)throw new IOException("Entity ledger limit exceeded");
        Path dir=path.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp=Files.createTempFile(dir,"pridefp-encounters-",".pending");
        try {
            try(FileChannel channel=FileChannel.open(tmp,StandardOpenOption.WRITE)) {
                ByteBuffer buffer=ByteBuffer.wrap(payload);
                while(buffer.hasRemaining())channel.write(buffer);
                channel.force(true);
            }
            Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(tmp);}
    }

    public static Snapshot load(Path path) throws IOException {
        byte[] bytes=Files.readAllBytes(path);
        if(bytes.length<50 || bytes.length>MAX_BYTES)
            throw new IOException("Entity ledger has invalid size");
        CRC32 check=new CRC32();check.update(bytes,0,bytes.length-8);
        try(var reader=new DataInputStream(new ByteArrayInputStream(bytes))) {
            if(reader.readInt()!=MAGIC || reader.readInt()!=VERSION)
                throw new IOException("Invalid entity ledger signature or version");
            String dimension=reader.readUTF();
            long seed=reader.readLong();
            int x=reader.readInt(),y=reader.readInt(),z=reader.readInt();
            int count=reader.readInt();
            if(count<0 || count>16)throw new IOException("Invalid binding count");
            Map<Integer,UUID> map=new HashMap<>();
            for(int i=0;i<count;i++) {
                int room=reader.readInt();
                UUID id=new UUID(reader.readLong(),reader.readLong());
                if(map.putIfAbsent(room,id)!=null)
                    throw new IOException("Duplicate room in entity ledger");
            }
            long crc=reader.readLong();
            if(reader.available()!=0 || check.getValue()!=crc)
                throw new IOException("Entity ledger has trailing data or invalid checksum");
            try {return new Snapshot(new Key(dimension,seed,x,y,z),map);}
            catch(IllegalArgumentException ex) {throw new IOException("Invalid ledger content",ex);}
        } catch(EOFException ex) {throw new IOException("Truncated entity ledger",ex);}
    }
}
