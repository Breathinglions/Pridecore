package com.prideregion.core.story;

import com.prideregion.core.*;
import java.nio.file.Files;
import java.util.*;

public final class ResonanceFinaleCompletionTests {
    private static int assertions;
    private static void check(boolean ok) {
        assertions++;
        if (!ok) throw new AssertionError("Finale completion assertion " + assertions);
    }
    private static void denied(Throwing task) throws Exception {
        assertions++;
        try { task.run(); throw new AssertionError("Expected finale completion denial"); }
        catch (IllegalStateException expected) { }
    }
    private interface Throwing { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        var service = new ProgressionService(
            new ProfileStore(Files.createTempDirectory("pride-finale-completion-")));
        var controller = new ResonanceFinaleController();
        var completion = new ResonanceFinaleCompletion(service, controller);
        UUID player = UUID.randomUUID(), other = UUID.randomUUID();
        denied(() -> completion.complete(player));
        service.grant(player, "pride:cael_battle_won");
        denied(() -> completion.complete(player));
        service.grant(player, "pride:clue_engine");
        denied(() -> completion.complete(player));
        controller.start(player, true, true);
        denied(() -> completion.complete(player));
        for (String conduit : List.of("north","south","east","west"))
            controller.conduit(player, conduit);
        for (String shield : List.of("alpha","beta","gamma"))
            controller.shieldVictory(player, shield);
        denied(() -> completion.complete(player));
        controller.stabilize(player);
        denied(() -> completion.complete(other));
        check(completion.complete(player));
        check(completion.completed(player));
        check(!completion.complete(player));
        long revision = service.get(player).revision();
        controller.disconnect(player);
        check(completion.completed(player));
        check(service.get(player).revision() == revision);
        check(!completion.completed(other));
        System.out.println("PASS: " + assertions + " durable finale completion checks");
    }
}
