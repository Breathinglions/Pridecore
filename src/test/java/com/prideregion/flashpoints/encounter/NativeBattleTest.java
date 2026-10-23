package com.prideregion.flashpoints.encounter;

import java.nio.file.*;
import java.util.*;

/** Test the exact battle shape and durable one-time completion. */
public final class NativeBattleTest {
    private static final UUID BOSS=UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID PLAYER=UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final UUID STRANGER=UUID.fromString("33333333-4444-5555-6666-777777777777");
    private static BattleOutcomeRules.Actor wild(UUID id){
        return new BattleOutcomeRules.Actor(true,false,Set.of(id),Set.of());
    }
    private static BattleOutcomeRules.Actor player(UUID id) {
        return new BattleOutcomeRules.Actor(false,true,Set.of(STRANGER),Set.of(id));
    }
    private static void absent(List<BattleOutcomeRules.Actor> winners,
                               List<BattleOutcomeRules.Actor> losers,boolean capture){
        if(BattleOutcomeRules.classify(BOSS,PLAYER,winners,losers,capture).isPresent())
            throw new AssertionError("Unrelated/ambiguous battle gave completion credit");
    }
    public static void main(String[] args)throws Exception {
        var human=player(PLAYER);var beast=wild(BOSS);
        if(BattleOutcomeRules.classify(BOSS,PLAYER,List.of(human),List.of(beast),false)
                .orElseThrow()!=BattleOutcomeRules.Result.DEFEATED)
            throw new AssertionError("Victory not recognized");
        if(BattleOutcomeRules.classify(BOSS,PLAYER,List.of(beast),List.of(human),false)
                .orElseThrow()!=BattleOutcomeRules.Result.PLAYER_LOST)
            throw new AssertionError("Player loss not recognized");
        absent(List.of(human),List.of(beast),true);
        absent(List.of(player(STRANGER)),List.of(beast),false);
        absent(List.of(human),List.of(wild(STRANGER)),false);
        absent(List.of(human,player(STRANGER)),List.of(beast),false);
        absent(List.of(human),List.of(beast,wild(STRANGER)),false);
        absent(List.of(new BattleOutcomeRules.Actor(false,false,Set.of(BOSS),Set.of())),
            List.of(human),false);
        absent(List.of(human),List.of(new BattleOutcomeRules.Actor(true,false,Set.of(BOSS,STRANGER),Set.of())),false);

        var key=new EncounterLedger.Key("multiworld:fptest",1047,-39,-19,-5);
        var armed=new NativeBattleProgress.Snapshot(key,BOSS,PLAYER,
            NativeBattleProgress.Stage.ARMED,null,0);
        UUID battle=UUID.randomUUID();
        var defeat=armed.resolve(BattleOutcomeRules.Result.DEFEATED,battle,1234);
        if(defeat.stage()!=NativeBattleProgress.Stage.DEFEATED ||
            !battle.equals(defeat.battleId()))throw new AssertionError("Outcome missing");
        var loss=armed.resolve(BattleOutcomeRules.Result.PLAYER_LOST,battle,1235);
        if(loss.stage()!=NativeBattleProgress.Stage.PLAYER_LOST)
            throw new AssertionError("Loss missing");
        try{
            defeat.resolve(BattleOutcomeRules.Result.PLAYER_LOST,battle,9999);
            throw new AssertionError("Duplicate terminal result permitted");
        }catch(IllegalStateException expected){}
        Path dir=Files.createTempDirectory("pridefp-native-battle-");
        Path file=dir.resolve("battle.bin");
        try{
            NativeBattleJournal.save(file,armed);
            if(!NativeBattleJournal.load(file).equals(armed))
                throw new AssertionError("Armed disk record did not roundtrip");
            NativeBattleJournal.save(file,defeat);
            if(!NativeBattleJournal.load(file).equals(defeat))
                throw new AssertionError("Defeat record did not roundtrip");
            NativeBattleJournal.save(file,loss);
            if(!NativeBattleJournal.load(file).equals(loss))
                throw new AssertionError("Loss record did not roundtrip");
            byte[] damaged=Files.readAllBytes(file);
            damaged[damaged.length-9]^=1;
            Files.write(file,damaged);
            try {
                NativeBattleJournal.load(file);
                throw new AssertionError("Corrupt journal accepted");
            }catch(java.io.IOException expected){}
            System.out.println("PASS: exact Cobblemon player/wild battle matching, capture exclusion, win/loss, CRC persistence and replay protection");
        }finally{
            Files.deleteIfExists(file);Files.delete(dir);
        }
    }
}
