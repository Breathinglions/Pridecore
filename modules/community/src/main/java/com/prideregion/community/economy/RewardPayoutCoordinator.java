package com.prideregion.community.economy;

import com.prideregion.community.RewardLedger;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Crash-conservative reward coordinator; reserves the unique claim on disk before
 * touching Impactor. Unknown or failed transactions remain reserved and require
 * manual review. This favors never paying twice over automatic retry.
 *
 * The Ledger cannot form an ACID transaction with Impactor's storage. A crash
 * between the economy deposit and PAID record remains PENDING for reconciliation.
 */
public final class RewardPayoutCoordinator {
    @FunctionalInterface public interface Wallet {
        CompletableFuture<Boolean> deposit(UUID player, int amount);
    }
    public enum Outcome { PAID, BLOCKED, REVIEW, LEDGER_ERROR }
    public record PaymentResult(Outcome outcome, String transactionId, int amount, String diagnostic) { }
    private final RewardLedger ledger;
    private final Wallet wallet;
    private final int dailyCap;

    public RewardPayoutCoordinator(RewardLedger ledger, Wallet wallet, int dailyCap) {
        this.ledger = Objects.requireNonNull(ledger);
        this.wallet = Objects.requireNonNull(wallet);
        if (dailyCap <= 0) throw new IllegalArgumentException("dailyCap must be positive");
        this.dailyCap = dailyCap;
    }

    public CompletableFuture<PaymentResult> award(UUID player, UUID roundId, int amount) {
        final Optional<RewardLedger.Claim> maybe;
        try {
            maybe = ledger.reserve(player, roundId, amount, dailyCap);
        } catch (IOException | RuntimeException e) {
            // No deposit is attempted if reservation was not confirmed durable.
            return CompletableFuture.completedFuture(new PaymentResult(
                    Outcome.LEDGER_ERROR, "", amount, e.toString()));
        }
        if (maybe.isEmpty()) return CompletableFuture.completedFuture(new PaymentResult(
                Outcome.BLOCKED, roundId + ":" + player, amount, "Duplicate or daily cap"));
        var claim = maybe.orElseThrow();
        final CompletableFuture<Boolean> deposited;
        try { deposited = Objects.requireNonNull(wallet.deposit(player, amount)); }
        catch (Throwable e) { return finishUncertain(claim, e.toString()); }
        return deposited.handle((success, error) -> {
            if (error != null || !Boolean.TRUE.equals(success)) {
                return resolve(claim, RewardLedger.State.MANUAL_REVIEW,
                        Outcome.REVIEW, error == null ? "Provider returned non-success" : error.toString());
            }
            return resolve(claim, RewardLedger.State.PAID, Outcome.PAID, "Confirmed success");
        });
    }

    private CompletableFuture<PaymentResult> finishUncertain(RewardLedger.Claim claim, String error) {
        return CompletableFuture.completedFuture(resolve(claim,
                RewardLedger.State.MANUAL_REVIEW, Outcome.REVIEW, error));
    }
    private PaymentResult resolve(RewardLedger.Claim claim, RewardLedger.State state,
                                  Outcome outcome, String description) {
        try {
            ledger.update(claim.transaction(), state);
            return new PaymentResult(outcome, claim.transaction(), claim.amount(), description);
        } catch (IOException | RuntimeException e) {
            // Deposit may have gone through, but PAID status could not be persisted.
            // Existing PENDING row remains the durable block against another credit.
            return new PaymentResult(Outcome.LEDGER_ERROR, claim.transaction(), claim.amount(),
                    "Unable to finalize payout ledger: " + e);
        }
    }
    public int reservedToday(UUID player) {
        return ledger.reservedOrPaidToday(player, LocalDate.now(ZoneOffset.UTC));
    }
    public int unresolvedCount() { return ledger.unresolved().size(); }
}
