package com.prideregion.community.economy;

import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.transactions.EconomyTransaction;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Impactor 5.3.5 Fabric API bridge. Only compiled against the original, unmodified
 * Impactor jar. No commands, internal Impactor classes, or second currency.
 *
 * Account acquisition is asynchronous. Deposits are dispatched to the supplied
 * Minecraft server-thread executor, NOT performed on Impactor's storage threads.
 * Failures are returned; the caller MUST NOT retry an uncertain payment blindly.
 */
public final class ImpactorWalletBridge implements RewardPayoutCoordinator.Wallet {
    private final Executor minecraftServerThread;

    public ImpactorWalletBridge(Executor minecraftServerThread) {
        this.minecraftServerThread = Objects.requireNonNull(minecraftServerThread);
    }

    @Override
    public CompletableFuture<Boolean> deposit(UUID player, int amount) {
        CompletableFuture<Boolean> completed = new CompletableFuture<>();
        if (player == null || amount <= 0) {
            completed.completeExceptionally(new IllegalArgumentException("Invalid payout"));
            return completed;
        }
        try {
            EconomyService economy = EconomyService.instance();
            Currency currency = economy.currencies().primary();
            if (currency == null) throw new IllegalStateException("Impactor primary currency unavailable");
            // Always use the primary currency, not a fixed name or a command.
            economy.account(currency, player).whenComplete((account, lookupError) -> {
                if (lookupError != null) {
                    completed.completeExceptionally(lookupError);
                    return;
                }
                if (account == null) {
                    completed.completeExceptionally(new IllegalStateException("No Impactor account"));
                    return;
                }
                try {
                    minecraftServerThread.execute(() -> {
                        try {
                            if (!player.equals(account.owner()))
                                throw new IllegalStateException("Account owner mismatch");
                            if (!currency.equals(account.currency()))
                                throw new IllegalStateException("Currency changed during lookup");
                            BigDecimal credit = BigDecimal.valueOf(amount);
                            EconomyTransaction transaction = account.deposit(credit);
                            if (transaction == null) throw new IllegalStateException("Null transaction result");
                            completed.complete(transaction.successful());
                        } catch (Throwable depositError) {
                            completed.completeExceptionally(depositError);
                        }
                    });
                } catch (Throwable schedulingError) {
                    completed.completeExceptionally(schedulingError);
                }
            });
        } catch (Throwable startupError) {
            // Linkage errors/disabled service are treated as unavailable, no free money.
            completed.completeExceptionally(startupError);
        }
        return completed;
    }
}
