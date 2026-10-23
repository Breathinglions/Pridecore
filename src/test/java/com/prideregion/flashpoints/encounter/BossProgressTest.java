package com.prideregion.flashpoints.encounter;

import java.nio.file.*;
import java.util.UUID;

/** No Minecraft dependency; CI validates state, durability and replay safety. */
public final class BossProgressTest {
    public static void main(String[] args) throws Exception {
        var key=new EncounterLedger.Key("multiworld:fptest",1047,-29,-20,1);
        UUID op=UUID.fromString("e04a0514-927c-425e-bb08-3219f5a4aab4");
        var initial=BossProgress.initial(key,op);
        if(initial.stage()!=BossProgress.Stage.READY || initial.attempts()!=0)
            throw new AssertionError("Invalid initial state");
        var active=BossProgress.transition(initial,BossProgress.Action.START,op,123);
        var captured=BossProgress.transition(active,BossProgress.Action.CAPTURE,op,456);
        var defeated=BossProgress.transition(active,BossProgress.Action.DEFEAT,op,456);
        if(!captured.terminal() || !defeated.terminal())
            throw new AssertionError("Boss resolution not terminal");
        for(var action:BossProgress.Action.values()) {
            try {
                BossProgress.transition(captured,action,op,789);
                throw new AssertionError("Terminal capture allowed action "+action);
            } catch(IllegalStateException expected) {}
        }
        try {
            BossProgress.transition(initial,BossProgress.Action.CAPTURE,op,1);
            throw new AssertionError("Capture without start accepted");
        } catch(IllegalStateException expected) {}
        var wiped=BossProgress.transition(active,BossProgress.Action.WIPE,op,300);
        try {
            BossProgress.transition(wiped,BossProgress.Action.DEFEAT,op,400);
            throw new AssertionError("Defeat allowed after wipe without retry");
        } catch(IllegalStateException expected) {}
        var retry=BossProgress.transition(wiped,BossProgress.Action.RETRY,op,500);
        if(retry.stage()!=BossProgress.Stage.ACTIVE || retry.attempts()!=2)
            throw new AssertionError("Retry attempt count incorrect");
        if(!BossProgress.transition(retry,BossProgress.Action.DEFEAT,op,600).terminal())
            throw new AssertionError("Retry cannot complete");
        Path dir=Files.createTempDirectory("pridefp-boss-");
        Path journal=dir.resolve("boss.bin");
        try {
            BossProgressJournal.save(journal,active);
            if(!BossProgressJournal.load(journal).equals(active))
                throw new AssertionError("Initial journal roundtrip failed");
            BossProgressJournal.save(journal,captured);
            if(!BossProgressJournal.load(journal).equals(captured))
                throw new AssertionError("Overwrite journal roundtrip failed");
            byte[] bytes=Files.readAllBytes(journal);
            bytes[bytes.length-11]^=1;
            Files.write(journal,bytes);
            try {
                BossProgressJournal.load(journal);
                throw new AssertionError("Corrupt boss outcome record accepted");
            } catch(java.io.IOException expected) {}
            System.out.println("PASS: captured/defeated/wiped state transitions, retry, terminal guard, journal durability and checksum");
        } finally {
            Files.deleteIfExists(journal);Files.delete(dir);
        }
    }
}
