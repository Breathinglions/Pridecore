package com.prideregion.core.story;

import com.prideregion.core.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Failure-injection test with a real atomic profile file, no game runtime.
 * Simulates a transient profile I/O fault after a genuine battle was armed.
 */
public final class StoryBattlePersistenceRecoveryTests {
    private static int assertions;
    private static void check(boolean good) {
        assertions++;
        if (!good) throw new AssertionError("Verified victory recovery test " + assertions);
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("pride-victory-io-recovery-");
        ProfileStore disk = new ProfileStore(dir);
        ProgressionService service = new ProgressionService(disk);
        StoryBattleSessionRouter router = new StoryBattleSessionRouter(
            new StoryBattleProof(service));
        StoryBattleResultRetryQueue retries = new StoryBattleResultRetryQueue();
        UUID owner = UUID.randomUUID(), battle = UUID.randomUUID();
        service.grant(owner, "pride:met_rin");
        var start = new StoryBattleSessionRouter.NativeStart(
            battle, owner, "rin_route1", "pride_story:rin_route1");
        check(router.started(start));
        Path profilePath = disk.path(owner);
        Path backup = dir.resolve(owner + ".safe-backup");
        Files.move(profilePath, backup);
        Files.createDirectory(profilePath);
        boolean failedClosed = false;
        try {
            router.ended(battle, owner);
        } catch (IOException expected) {
            failedClosed = true;
        } finally {
            Files.delete(profilePath);
            Files.move(backup, profilePath);
        }
        check(failedClosed);
        check(router.encounterForBattle(battle).isPresent());
        check(!service.get(owner).facts().contains("pride:rin_route1_won"));
        check(retries.schedule(battle, owner));
        for (int i = 0; i < StoryBattleResultRetryQueue.RETRY_TICKS - 1; i++)
            check(retries.tick().due().isEmpty());
        var due = retries.tick();
        check(due.due().size() == 1 && due.due().getFirst().attempt() == 1);
        check(router.ended(battle, due.due().getFirst().winner()));
        retries.forget(battle);
        check(retries.size() == 0);
        check(!router.ended(battle, owner));
        check(service.get(owner).facts().contains("pride:rin_route1_won"));
        long revision = service.get(owner).revision();
        check(!service.grant(owner, "pride:rin_route1_won").changed());
        check(service.get(owner).revision() == revision);
        System.out.println("PASS: " + assertions +
            " atomic victory recovery assertions with injected profile I/O failure");
    }
}
