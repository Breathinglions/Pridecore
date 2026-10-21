package com.prideregion.flashpoints.layout;
import java.util.HashSet;
import java.util.Set;

public final class StandardDungeonGeneratorTest {
    public static void main(String[] args) {
        Set<Integer> variants=new HashSet<>();
        for(long seed=0;seed<150;seed++) {
            DungeonLayout a=StandardDungeonGenerator.generate(seed);
            DungeonLayout b=StandardDungeonGenerator.generate(seed);
            if(!a.sameAs(b))throw new AssertionError("Non-deterministic seed "+seed);
            if(a.width()!=188||a.length()!=164||a.height()!=14)
                throw new AssertionError("Unexpected standard dimensions");
            if(!DungeonGenerator.reachable(a))throw new AssertionError("Unreachable boss "+seed);
            if(!a.passable(a.entranceX(),a.entranceZ())||!a.passable(a.bossX(),a.bossZ()))
                throw new AssertionError("Spawn or boss obstructed: "+seed);
            if(a.filledBlocks()<20000)throw new AssertionError("Suspiciously empty layout");
            variants.add(a.filledBlocks());
        }
        if(variants.size()<100)throw new AssertionError("Insufficient variation: "+variants.size());
        System.out.println("PASS: 150 deterministic 16-room Standard layouts with reachable bosses");
    }
}
