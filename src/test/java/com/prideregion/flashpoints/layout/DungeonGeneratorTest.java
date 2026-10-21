package com.prideregion.flashpoints.layout;
import java.util.HashSet;
import java.util.Set;
public final class DungeonGeneratorTest {
    public static void main(String[] args) {
        Set<Integer> variants=new HashSet<>();
        for(long seed=-64;seed<936;seed++) {
            DungeonLayout a=DungeonGenerator.generate(seed);
            DungeonLayout b=DungeonGenerator.generate(seed);
            if(!a.sameAs(b))throw new AssertionError("non deterministic: "+seed);
            if(!DungeonGenerator.reachable(a))throw new AssertionError("unreachable boss: "+seed);
            if(!a.passable(a.entranceX(),a.entranceZ()) || !a.passable(a.bossX(),a.bossZ()))
                throw new AssertionError("blocked entrance or boss");
            if(a.volume()!=100*76*9 || a.filledBlocks()<2500)throw new AssertionError("wrong volume");
            variants.add(a.filledBlocks());
        }
        if(variants.size()<100)throw new AssertionError("insufficient seed variation: "+variants.size());
        System.out.println("PASS: 1,000 layouts; "+variants.size()+" distinct block counts");
    }
}
