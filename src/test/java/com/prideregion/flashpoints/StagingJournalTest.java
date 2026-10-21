package com.prideregion.flashpoints;

import com.prideregion.flashpoints.layout.DungeonGenerator;
import com.prideregion.flashpoints.layout.DungeonLayout;
import com.prideregion.flashpoints.layout.StandardDungeonGenerator;
import java.nio.file.*;
import java.util.UUID;

public final class StagingJournalTest {
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("pridefp-test-");
        Path path=dir.resolve("active.bin");
        try {
            UUID owner=UUID.randomUUID();
            DungeonLayout classic=DungeonGenerator.generate(1047);
            StagingJournal.Snapshot initial=new StagingJournal.Snapshot(
                "test:flashpoint_test",owner,-250,100,90,classic);
            StagingJournal.save(path,initial);
            StagingJournal.Snapshot restored=StagingJournal.load(path);
            if(!restored.dimension().equals(initial.dimension())||!restored.owner().equals(owner)
                || restored.x()!=-250 || restored.y()!=100 || restored.z()!=90
                || !restored.layout().sameAs(initial.layout()))
                throw new AssertionError("Classic layout roundtrip failed");
            try {
                StagingJournal.save(path,initial);
                throw new AssertionError("Concurrent journal overwrite allowed");
            } catch(FileAlreadyExistsException expected) {}
            StagingJournal.remove(path);

            DungeonLayout standard=StandardDungeonGenerator.generate(2084);
            StagingJournal.save(path,new StagingJournal.Snapshot(
                "test:flashpoint_test",owner,1234,80,-2000,standard));
            if(!StagingJournal.load(path).layout().sameAs(standard))
                throw new AssertionError("Large layout recovery failed");
            byte[] data=Files.readAllBytes(path);
            data[data.length-11]^=1;
            Files.write(path,data);
            try {
                StagingJournal.load(path);
                throw new AssertionError("Tampered journal accepted");
            } catch(java.io.IOException expected) {}
            System.out.println("PASS: write-ahead record roundtrip, non-overwrite, corruption rejection");
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(dir.resolve("active.bin.pending"));
            Files.deleteIfExists(dir);
        }
    }
}
