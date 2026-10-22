package com.prideregion.flashpoints.encounter;

import com.prideregion.flashpoints.layout.DungeonLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic room encounter blueprint. Does NOT spawn entities automatically.
 * Position coordinates are relative to the Standard dungeon's block origin.
 * This is staging metadata, not a complete quest, boss AI or battle lifecycle.
 */
public final class DungeonEncounterPlan {
    public enum Kind { ENTRANCE, COMBAT, ELITE, PUZZLE, REST, TREASURE, BOSS }
    public record Room(int id, int localX, int localZ, Kind kind,
                       String species, int level) {
        public boolean combat() { return kind==Kind.COMBAT || kind==Kind.ELITE || kind==Kind.BOSS; }
    }
    public record Plan(long seed, List<Room> rooms) {
        public Plan { rooms=List.copyOf(rooms); }
        public Room room(int id) {
            if(id < 0 || id >= rooms.size())throw new IllegalArgumentException("Room ID must be 0..15");
            return rooms.get(id);
        }
        public long combatRooms() { return rooms.stream().filter(Room::combat).count(); }
    }

    private static final String[] COMMON={"golbat","graveler","sableye","mawile","haunter","boldore"};
    private static final String[] ELITES={"aggron","gengar","golurk"};
    private static final String[] BOSSES={"tyranitar","metagross","garchomp"};

    private DungeonEncounterPlan() {}

    public static Plan standard(long seed, DungeonLayout layout) {
        if(layout.width()!=188 || layout.length()!=164 || layout.height()!=14)
            throw new IllegalArgumentException("Encounter blueprint requires Standard dungeon");
        Random random=new Random(seed ^ 0x5F3759DFDEADBEEFL);
        ArrayList<Room> rooms=new ArrayList<>(16);
        for(int row=0;row<4;row++)for(int col=0;col<4;col++) {
            int id=row*4+col;
            // These nominal positions are at most two blocks from the true
            // procedural room centres. They are always within each room floor.
            int x=23+col*46, z=21+row*39;
            if(!layout.passable(x,z))
                throw new IllegalStateException("Room marker inaccessible: "+id+", seed "+seed);
            Kind kind=kind(id);
            String species="";
            int level=0;
            if(kind==Kind.COMBAT) {
                species=COMMON[random.nextInt(COMMON.length)];
                level=24+random.nextInt(13);
            } else if(kind==Kind.ELITE) {
                species=ELITES[random.nextInt(ELITES.length)];
                level=39+random.nextInt(7);
            } else if(kind==Kind.BOSS) {
                species=BOSSES[random.nextInt(BOSSES.length)];
                level=49+random.nextInt(7);
            }
            rooms.add(new Room(id,x,z,kind,species,level));
        }
        return new Plan(seed,rooms);
    }
    private static Kind kind(int id) {
        return switch(id) {
            case 4 -> Kind.ENTRANCE;
            case 11 -> Kind.BOSS;
            case 3, 14 -> Kind.ELITE;
            case 2 -> Kind.PUZZLE;
            case 12 -> Kind.REST;
            case 8, 15 -> Kind.TREASURE;
            default -> Kind.COMBAT;
        };
    }
}
