package com.prideregion.community.economy;

import com.prideregion.community.RewardLedger;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RewardPayoutCoordinatorTest {
    private static int checks;
    private static void ok(boolean condition, String explanation) {
        checks++; if (!condition) throw new AssertionError(explanation);
    }
    public static void main(String[] args) throws Exception {
        var ledgerPath = Files.createTempDirectory("pride-impactor-test").resolve("payout.tsv");
        UUID player = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        int[] deposits = {0};
        RewardLedger ledger = new RewardLedger(ledgerPath);
        RewardPayoutCoordinator.Wallet wallet = (id, amount) -> {
            deposits[0]++; return CompletableFuture.completedFuture(true);
        };
        var coordinator = new RewardPayoutCoordinator(ledger, wallet, 400);
        var r = coordinator.award(player, a, 150).join();
        ok(r.outcome() == RewardPayoutCoordinator.Outcome.PAID, "paid only after success");
        ok(deposits[0] == 1, "one deposit");
        ok(coordinator.award(player, a, 150).join().outcome() == RewardPayoutCoordinator.Outcome.BLOCKED, "no double award");
        ok(deposits[0] == 1, "duplicate never touches wallet");
        ok(coordinator.award(player, UUID.randomUUID(), 300).join().outcome() == RewardPayoutCoordinator.Outcome.BLOCKED, "daily cap");
        ok(deposits[0] == 1, "cap never touches wallet");
        ledger = new RewardLedger(ledgerPath);
        coordinator = new RewardPayoutCoordinator(ledger, wallet, 400);
        ok(coordinator.award(player, a, 150).join().outcome() == RewardPayoutCoordinator.Outcome.BLOCKED, "paid after restart still blocked");
        ok(coordinator.reservedToday(player) == 150, "daily cap survives restart");
        UUID pendingRound = UUID.randomUUID();
        CompletableFuture<Boolean> pending = new CompletableFuture<>();
        var delayed = new RewardPayoutCoordinator(ledger, (id, amount) -> pending, 400);
        var call = delayed.award(player, pendingRound, 200);
        ok(!call.isDone(), "waits for provider result");
        ok(delayed.reservedToday(player) == 350, "pending counts towards daily cap");
        var restart = new RewardPayoutCoordinator(new RewardLedger(ledgerPath), wallet, 400);
        ok(restart.award(player, pendingRound, 200).join().outcome() == RewardPayoutCoordinator.Outcome.BLOCKED,
                "crash-pending may not auto-repay");
        pending.completeExceptionally(new IllegalStateException("provider outage"));
        ok(call.join().outcome() == RewardPayoutCoordinator.Outcome.REVIEW, "provider exception requires review");
        ok(delayed.unresolvedCount() == 1, "unresolved claim retained");
        var failed = new RewardPayoutCoordinator(ledger, (id, amount) -> CompletableFuture.completedFuture(false), 1000);
        ok(failed.award(player, UUID.randomUUID(), 200).join().outcome() == RewardPayoutCoordinator.Outcome.REVIEW,
                "provider non-success requires review");
        ok(failed.unresolvedCount() == 2, "both non-success remain blocked");
        UUID other = UUID.randomUUID();
        var malformed = new RewardPayoutCoordinator(ledger, (id, amount) -> { throw new IllegalStateException("offline"); }, 1000);
        ok(malformed.award(other, UUID.randomUUID(), 100).join().outcome() == RewardPayoutCoordinator.Outcome.REVIEW,
                "synchronous startup failure is safe");
        ok(malformed.award(other, UUID.randomUUID(), 0).join().outcome() == RewardPayoutCoordinator.Outcome.BLOCKED,
                "zero reward rejected");
        // Even if another player attempts the SAME round ID, only one wallet may be credited.
        UUID winnerRound = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        int before = deposits[0];
        var perRound = new RewardPayoutCoordinator(ledger, wallet, 1000);
        ok(perRound.award(first, winnerRound, 100).join().outcome() == RewardPayoutCoordinator.Outcome.PAID,
            "first player has winning claim");
        ok(perRound.award(second, winnerRound, 100).join().outcome() == RewardPayoutCoordinator.Outcome.BLOCKED,
            "second player cannot collect same round");
        ok(deposits[0] == before + 1, "cross-player duplicate never touches wallet");
        perRound = new RewardPayoutCoordinator(new RewardLedger(ledgerPath), wallet, 1000);
        ok(perRound.award(second, winnerRound, 100).join().outcome() == RewardPayoutCoordinator.Outcome.BLOCKED,
            "cross-player claim blocked after restart");
        ok(perRound.unresolvedClaims(5).size() >= 1, "staff audit can inspect unresolved claims");
        System.out.println("PASS " + checks + " Impactor reward-coordinator checks");
    }
}
