package com.prideregion.flashpoints.layout;

import java.util.Arrays;

public record DungeonLayout(long seed, int width, int height, int length,
                            int entranceX, int entranceZ, int bossX, int bossZ, byte[] blocks) {
    public DungeonLayout {
        blocks = blocks.clone();
        if (blocks.length != width * height * length) throw new IllegalArgumentException("volume mismatch");
    }
    @Override public byte[] blocks() { return blocks.clone(); }
    public int volume() { return blocks.length; }
    public int filledBlocks() {
        int count = 0;
        for (byte material : blocks) if (material != 0) count++;
        return count;
    }
    public int index(int x, int y, int z) { return (y * length + z) * width + x; }
    public byte material(int index) { return blocks[index]; }
    public byte material(int x, int y, int z) { return blocks[index(x,y,z)]; }
    public int x(int index) { return index % width; }
    public int z(int index) { return (index / width) % length; }
    public int y(int index) { return index / (width * length); }
    public boolean passable(int x, int z) {
        return x >= 0 && x < width && z >= 0 && z < length
            && material(x,0,z) != 0 && material(x,1,z) == 0 && material(x,2,z) == 0;
    }
    public boolean sameAs(DungeonLayout d) {
        return width==d.width && height==d.height && length==d.length
            && entranceX==d.entranceX && entranceZ==d.entranceZ
            && bossX==d.bossX && bossZ==d.bossZ && Arrays.equals(blocks,d.blocks);
    }
}
