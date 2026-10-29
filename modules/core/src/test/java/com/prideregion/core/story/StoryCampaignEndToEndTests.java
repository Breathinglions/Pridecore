package com.prideregion.core.story;

import com.prideregion.core.*;
import java.nio.file.*;
import java.util.*;

/**
 * Real ProfileStore + battle proof/router + launch ledger + finale checkpoint
 * integration. Does not simulate RCT's network battle event implementation.
 */
public final class StoryCampaignEndToEndTests {
    private static int assertions;
    private static void check(boolean pass) {
        assertions++;
        if (!pass) throw new AssertionError("Campaign end-to-end invariant " + assertions);
    }
    private static void win(ProgressionService service, StoryBattleLaunchLedger launches,
                            StoryBattleSessionRouter router, UUID player, String id)
            throws Exception {
        String trainer = StoryBattleSessionRouter.acceptedTrainers().get(id);
        check(trainer != null);
        UUID trainerEntity = UUID.randomUUID(), battle = UUID.randomUUID();
        var launch = new StoryBattleLaunchLedger.Launch(player, id, trainerEntity);
        check(launches.reserve(launch));
        var start = launches.launched(player, trainerEntity, battle).orElseThrow();
        check(start.trainerIdentity().equals(trainer));
        check(launches.owns(battle, player));
        check(router.started(start));
        check(router.encounterForBattle(battle).orElseThrow().equals(id));
        check(!router.ended(battle, UUID.randomUUID()));
        launches.finished(battle);
        check(!service.get(player).facts().contains(
            StoryBattleProof.definitions().stream().filter(e -> e.id().equals(id))
                .findFirst().orElseThrow().earnedFact()));
        UUID second = UUID.randomUUID();
        check(launches.reserve(launch));
        var legit = launches.launched(player, trainerEntity, second).orElseThrow();
        check(router.started(legit));
        check(router.ended(second, player));
        launches.finished(second);
        check(!router.ended(second, player));
        check(!launches.active(player));
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("module02-full-campaign-");
        var service = new ProgressionService(new ProfileStore(dir.resolve("profiles")));
        var ledger = new StoryBattleLaunchLedger();
        var router = new StoryBattleSessionRouter(new StoryBattleProof(service));
        var finaleStore = new ResonanceCheckpointStore(dir.resolve("checkpoints"));
        UUID player = UUID.randomUUID(), stranger = UUID.randomUUID();

        // Milestone facts arise from prior story dialogue, not these battles.
        for (String id : List.of("rin_route1","rin_canyon","rin_summit",
                                "eclipse_lieutenant","sable","director_cael")) {
            var def = StoryBattleProof.definitions().stream()
                .filter(e -> e.id().equals(id)).findFirst().orElseThrow();
            service.grant(player, def.requiredFact());
            win(service, ledger, router, player, id);
            check(service.get(player).facts().contains(def.earnedFact()));
            check(!service.get(stranger).facts().contains(def.earnedFact()));
        }
        service.grant(player, "pride:clue_engine");
        var controller = new ResonanceFinaleController();
        var completion = new ResonanceFinaleCompletion(service, controller);
        check(controller.start(player, true, true).phase()
            == ResonanceFinaleController.Phase.CONDUITS);
        for (String cardinal : List.of("north","south","east","west")) {
            controller.conduit(player, cardinal);
            finaleStore.save(controller.checkpoint(player).orElseThrow());
        }
        check(controller.snapshot(player).phase()
            == ResonanceFinaleController.Phase.SHIELDS);

        for (String shield : List.of("alpha","beta","gamma")) {
            win(service, ledger, router, player, "shield_" + shield);
            check(service.get(player).facts().contains("pride:shield_" + shield + "_won"));
            // I/O can crash after verified battle credit but before the finale
            // checkpoint. The saved fact, not a client, restores this state.
            if (!shield.equals("beta")) {
                controller.shieldVictory(player, shield);
                finaleStore.save(controller.checkpoint(player).orElseThrow());
            } else {
                var next = new ResonanceFinaleController();
                next.restore(finaleStore.load(player).orElseThrow());
                check(next.snapshot(player).shields() == 1);
                next.shieldVictory(player, shield);
                controller = next;
                finaleStore.save(controller.checkpoint(player).orElseThrow());
            }
        }
        check(controller.snapshot(player).phase()
            == ResonanceFinaleController.Phase.STABILIZE);
        check(!completion.completed(player));
        controller.stabilize(player);
        finaleStore.save(controller.checkpoint(player).orElseThrow());
        check(completion.complete(player));
        check(!completion.complete(player));
        check(completion.completed(player));
        check(!completion.completed(stranger));
        finaleStore.delete(player);
        check(finaleStore.load(player).isEmpty());
        check(!ledger.active(player));
        check(!ledger.pending(player));
        System.out.println("PASS: " + assertions +
            " authenticated battle, crash recovery and full finale assertions");
    }
}
