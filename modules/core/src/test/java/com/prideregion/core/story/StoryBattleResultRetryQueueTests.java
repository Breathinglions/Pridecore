package com.prideregion.core.story;

import java.util.*;

public final class StoryBattleResultRetryQueueTests {
    private static int checks;
    private static void ok(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Victory retry invariant " + checks);
    }
    public static void main(String[] args) {
        var queue = new StoryBattleResultRetryQueue();
        UUID battle = UUID.randomUUID(), player = UUID.randomUUID();
        ok(queue.schedule(battle, player));
        ok(queue.schedule(battle, player));
        ok(!queue.schedule(battle, UUID.randomUUID()));
        ok(!queue.schedule(null, player));
        ok(!queue.schedule(battle, null));
        ok(queue.size() == 1);
        for (int attempt = 1; attempt <= StoryBattleResultRetryQueue.MAX_RETRIES; attempt++) {
            for (int tick = 0; tick < StoryBattleResultRetryQueue.RETRY_TICKS - 1; tick++) {
                var batch = queue.tick();
                ok(batch.due().isEmpty() && batch.expired().isEmpty());
            }
            var next = queue.tick();
            ok(next.due().size() == 1);
            ok(next.due().getFirst().attempt() == attempt);
            ok(next.due().getFirst().battle().equals(battle));
            ok(next.expired().isEmpty());
        }
        for (int tick = 0; tick < StoryBattleResultRetryQueue.RETRY_TICKS - 1; tick++)
            ok(queue.tick().expired().isEmpty());
        var expired = queue.tick();
        ok(expired.due().isEmpty());
        ok(expired.expired().size() == 1);
        ok(expired.expired().getFirst().battle().equals(battle));
        ok(queue.size() == 0);
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        ok(queue.schedule(UUID.randomUUID(), alice));
        ok(queue.schedule(UUID.randomUUID(), bob));
        queue.cancelPlayer(alice);
        ok(queue.size() == 1);
        queue.clear();
        ok(queue.size() == 0);
        for (int i = 0; i < StoryBattleResultRetryQueue.MAX_PENDING; i++)
            ok(queue.schedule(UUID.randomUUID(), UUID.randomUUID()));
        ok(!queue.schedule(UUID.randomUUID(), UUID.randomUUID()));
        queue.forget(battle);
        ok(queue.size() == StoryBattleResultRetryQueue.MAX_PENDING);
        queue.clear();
        ok(queue.size() == 0);
        System.out.println("PASS: " + checks + " bounded verified victory retry checks");
    }
}
