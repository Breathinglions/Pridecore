package com.prideregion.flashpoints.encounter;

import com.prideregion.flashpoints.layout.DungeonLayout;
import com.prideregion.flashpoints.layout.StandardDungeonGenerator;

public final class DungeonEncounterPlanTest {
    public static void main(String[] args) {
        for(long seed=0;seed<100;seed++) {
            DungeonLayout layout=StandardDungeonGenerator.generate(seed);
            var a=DungeonEncounterPlan.standard(seed,layout);
            var b=DungeonEncounterPlan.standard(seed,layout);
            if(!a.equals(b))throw new AssertionError("Encounter plan not deterministic for "+seed);
            if(a.rooms().size()!=16 || a.combatRooms()!=11)
                throw new AssertionError("Room count incorrect for "+seed);
            if(a.room(4).kind()!=DungeonEncounterPlan.Kind.ENTRANCE ||
               a.room(11).kind()!=DungeonEncounterPlan.Kind.BOSS)
                throw new AssertionError("Missing entrance or boss for "+seed);
            for(int i=0;i<16;i++) {
                var room=a.room(i);
                if(room.id()!=i || !layout.passable(room.localX(),room.localZ()))
                    throw new AssertionError("Inaccessible encounter room "+i+" seed "+seed);
                if(room.combat()) {
                    if(room.species().isBlank() || room.level()<20 || room.level()>60)
                        throw new AssertionError("Invalid species/level: room "+i+" seed "+seed);
                } else if(!room.species().isEmpty() || room.level()!=0) {
                    throw new AssertionError("Noncombat room has spawn data: "+i);
                }
            }
        }
        System.out.println("PASS: 100 deterministically assigned Standard encounter blueprints and accessible floor markers");
    }
}
