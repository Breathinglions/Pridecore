package com.prideregion.flashpoints.encounter;

import java.nio.file.*;
import java.util.UUID;

/** Regression guard for nonlethal combat phases, persisted outcome and replay safety. */
public final class ActionBossTest {
    public static void main(String[] args)throws Exception {
        var key=new EncounterLedger.Key("multiworld:fptest",1047,-39,-19,-5);
        UUID boss=UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID op=UUID.fromString("22222222-3333-4444-5555-666666666666");
        UUID other=UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        var state=ActionBossState.arm(key,boss,op,1234);
        if(state.hp()!=300 || state.phase()!=ActionBossState.Phase.ACTIVE)
            throw new AssertionError("Invalid initial action boss");
        for(int i=0;i<26;i++)state=state.hit(9,1235+i);
        if(state.hp()!=66)throw new AssertionError("Wrong pre-weakened HP: "+state.hp());
        state=state.hit(50,1300);
        if(state.hp()!=60 || state.phase()!=ActionBossState.Phase.WEAKENED)
            throw new AssertionError("Did not clamp at capture ready 20%");
        try {state.hit(10,1301);throw new AssertionError("WEAKENED takes damage");}
        catch(IllegalStateException expected){}
        try {state.capture(other,op,1400);throw new AssertionError("Wrong Pokemon accepted");}
        catch(IllegalStateException expected){}
        try {state.capture(boss,other,1400);throw new AssertionError("Wrong player accepted");}
        catch(IllegalStateException expected){}
        var captured=state.capture(boss,op,1500);
        if(captured.phase()!=ActionBossState.Phase.CAPTURED)throw new AssertionError("Not captured");
        try {captured.finish(op,1600);throw new AssertionError("Duplicate finish permitted");}
        catch(IllegalStateException expected){}
        var finished=state.finish(op,1600);
        if(finished.phase()!=ActionBossState.Phase.FINISHED)throw new AssertionError("Not finished");
        try {finished.capture(boss,op,1700);throw new AssertionError("Captured finished boss");}
        catch(IllegalStateException expected){}
        Path dir=Files.createTempDirectory("pride-action-");
        Path file=dir.resolve("action.bin");
        try {
            for(var variant:new ActionBossState.Snapshot[]{state,captured,finished}) {
                ActionBossJournal.save(file,variant);
                if(!ActionBossJournal.load(file).equals(variant))
                    throw new AssertionError("Action journal roundtrip failed");
            }
            byte[] bytes=Files.readAllBytes(file);bytes[bytes.length-11]^=1;
            Files.write(file,bytes);
            try {ActionBossJournal.load(file);throw new AssertionError("Corrupt journal accepted");}
            catch(java.io.IOException expected){}
        }finally {
            Files.deleteIfExists(file);Files.delete(dir);
        }
        System.out.println("PASS: custom action boss HP clamp, weakened/capture/finish paths, anti-replay and durable CRC journal");
    }
}
