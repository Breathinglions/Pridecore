package com.prideregion.flashpoints.encounter;

import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

/** Standalone regression tests for immutable, checksummed UUID associations. */
public final class EncounterLedgerTest {
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("pridefp-entity-ledger-");
        Path path=dir.resolve("entities.bin");
        try {
            var key=new EncounterLedger.Key("multiworld:fptest",1047L,-29,-20,1);
            UUID garchomp=UUID.fromString("12345678-1234-1234-1234-123456789abc");
            UUID elite=UUID.fromString("87654321-4321-4321-4321-cba987654321");
            var empty=new EncounterLedger.Snapshot(key,Map.of());
            var one=empty.bind(11,garchomp);
            EncounterLedger.save(path,one);
            if(!Files.exists(path))throw new AssertionError("No disk record");
            var read=EncounterLedger.load(path);
            if(!read.equals(one) || read.bindings().size()!=1)
                throw new AssertionError("Journal roundtrip failed");
            if(read.bindings().get(11)!=garchomp && !read.bindings().get(11).equals(garchomp))
                throw new AssertionError("Missing recorded boss UUID");
            try {
                read.bind(3,garchomp);
                throw new AssertionError("Duplicate UUID was permitted");
            } catch(IllegalArgumentException expected) {}
            try {
                read.bind(11,elite);
                throw new AssertionError("Overwriting same room was permitted");
            } catch(IllegalArgumentException expected) {}
            var two=read.bind(3,elite);
            EncounterLedger.save(path,two);
            if(!EncounterLedger.load(path).equals(two))
                throw new AssertionError("Atomic ledger overwrite failed");
            var removed=two.forget(11);
            EncounterLedger.save(path,removed);
            if(!EncounterLedger.load(path).equals(removed) ||
               EncounterLedger.load(path).bindings().containsKey(11))
                throw new AssertionError("Forget changed wrong association");
            byte[] corrupt=Files.readAllBytes(path);
            corrupt[corrupt.length-13]^=1;
            Files.write(path,corrupt);
            try {
                EncounterLedger.load(path);
                throw new AssertionError("Corrupt ledger was accepted");
            } catch(java.io.IOException expected) {}
            if(!two.bindings().containsKey(11) || empty.bindings().size()!=0)
                throw new AssertionError("Snapshot was mutated");
            System.out.println("PASS: ledger UUID persistence, binding guards, atomic overwrite, forget and CRC rejection");
        } finally {
            Files.deleteIfExists(path);
            Files.delete(dir);
        }
    }
}
