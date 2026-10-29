package com.prideregion.core.story;

import com.prideregion.core.*;
import java.nio.file.Files;
import java.util.*;

public final class StoryBattleSessionRouterTests {
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("Battle session router invariant " + checks);
    }
    public static void main(String[] args) throws Exception {
        var service = new ProgressionService(new ProfileStore(
            Files.createTempDirectory("story-session-router-")));
        var router = new StoryBattleSessionRouter(new StoryBattleProof(service));
        check(StoryBattleSessionRouter.acceptedTrainers().size() == 6);
        for (var battle : StoryBattleProof.definitions()) {
            UUID player = UUID.randomUUID(), other = UUID.randomUUID(),
                 battleId = UUID.randomUUID();
            String trainer = StoryBattleSessionRouter.acceptedTrainers().get(battle.id());
            service.grant(player, battle.requiredFact());
            check(!router.started(new StoryBattleSessionRouter.NativeStart(
                battleId, player, battle.id(), "pride_story:forged")));
            check(!service.get(player).facts().contains(battle.earnedFact()));
            var start = new StoryBattleSessionRouter.NativeStart(
                battleId, player, battle.id(), trainer);
            check(router.started(start));
            check(router.started(start));
            check(!router.started(new StoryBattleSessionRouter.NativeStart(
                battleId, other, battle.id(), trainer)));
            check(!router.ended(battleId, other));
            check(!service.get(player).facts().contains(battle.earnedFact()));
            check(router.started(start));
            check(router.ended(battleId, player));
            check(service.get(player).facts().contains(battle.earnedFact()));
            check(!router.ended(battleId, player));
            UUID pending = UUID.randomUUID();
            check(router.started(new StoryBattleSessionRouter.NativeStart(
                pending, player, battle.id(), trainer)));
            router.disconnect(player);
            check(!router.ended(pending, player));
            check(!service.get(other).facts().contains(battle.earnedFact()));
        }
        System.out.println("PASS: " + checks + " native event routing assertions");
    }
}
