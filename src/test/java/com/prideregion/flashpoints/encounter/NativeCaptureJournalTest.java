package com.prideregion.flashpoints.encounter;

import java.nio.file.*;
import java.util.UUID;

/** Pure Java staging tests for capture identity and atomic event-state storage. */
public final class NativeCaptureJournalTest {
    public static void main(String[] args) throws Exception {
        var key=new EncounterLedger.Key("multiworld:fptest",1047,-39,-19,-5);
        UUID boss=UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID player=UUID.fromString("99999999-8888-7777-6666-555555555555");
        UUID other=UUID.fromString("aaaaaaaa-1111-2222-3333-444444444444");
        var armed=new NativeCaptureJournal.Snapshot(key,boss,player,
            NativeCaptureJournal.Stage.ARMED,null,0L);
        try {
            armed.capture(other,player,1000);
            throw new AssertionError("Wrong Pokemon accepted");
        } catch(IllegalStateException expected) {}
        try {
            armed.capture(boss,other,1000);
            throw new AssertionError("Unregistered capturing player accepted");
        } catch(IllegalStateException expected) {}
        var captured=armed.capture(boss,player,1000);
        if(captured.stage()!=NativeCaptureJournal.Stage.CAPTURED || !player.equals(captured.capturedBy()))
            throw new AssertionError("Capture transition broken");
        try {
            captured.capture(boss,player,1001);
            throw new AssertionError("Duplicate completion accepted");
        } catch(IllegalStateException expected) {}

        Path dir=Files.createTempDirectory("pridefp-native-capture-");
        Path file=dir.resolve("native.bin");
        try {
            NativeCaptureJournal.save(file,armed);
            if(!armed.equals(NativeCaptureJournal.load(file)))
                throw new AssertionError("Armed journal did not roundtrip");
            NativeCaptureJournal.save(file,captured);
            if(!captured.equals(NativeCaptureJournal.load(file)))
                throw new AssertionError("Captured journal did not roundtrip");
            byte[] bytes=Files.readAllBytes(file);
            bytes[bytes.length-9]^=1;
            Files.write(file,bytes);
            try {
                NativeCaptureJournal.load(file);
                throw new AssertionError("CRC-corrupted journal accepted");
            } catch(java.io.IOException expected) {}
            System.out.println("PASS: native capture exact Pokemon UUID, owner match, one-time completion, persistence and CRC");
        } finally {
            Files.deleteIfExists(file);Files.delete(dir);
        }
    }
}
