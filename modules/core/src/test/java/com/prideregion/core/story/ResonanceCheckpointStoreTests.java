package com.prideregion.core.story;

import java.nio.file.*;
import java.util.*;

public final class ResonanceCheckpointStoreTests {
    private static int checks;
    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("Checkpoint test " + checks);
    }
    public static void main(String[] args) throws Exception {
        var dir = Files.createTempDirectory("pride-finale-disk-");
        var disk = new ResonanceCheckpointStore(dir);
        var a = new ResonanceFinaleController();
        UUID player = UUID.randomUUID(), outsider = UUID.randomUUID();
        check(disk.load(player).isEmpty());
        a.start(player, true, true);
        a.conduit(player, "north");
        disk.save(a.checkpoint(player).orElseThrow());
        var restored = new ResonanceFinaleController();
        check(restored.restore(disk.load(player).orElseThrow()).conduits() == 1);
        check(restored.snapshot(outsider).phase() == ResonanceFinaleController.Phase.LOCKED);
        for (String direction : List.of("south", "east", "west"))
            restored.conduit(player, direction);
        disk.save(restored.checkpoint(player).orElseThrow());
        var afterRestart = new ResonanceFinaleController();
        check(afterRestart.restore(disk.load(player).orElseThrow())
            .phase() == ResonanceFinaleController.Phase.SHIELDS);
        disk.delete(player);
        check(disk.load(player).isEmpty());
        check(disk.load(outsider).isEmpty());
        System.out.println("PASS: " + checks + " atomic finale disk checkpoint tests");
    }
}
