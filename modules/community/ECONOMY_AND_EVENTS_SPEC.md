# Pride Community: Impactor 5.3.5 verified API & events

## Exact API inspected from user's Impactor-Fabric-5.3.5+1.21.1.jar
```
EconomyService.instance()
service.currencies().primary() -> Currency
service.account(Currency, UUID) -> CompletableFuture<Account>
account.deposit(BigDecimal) -> EconomyTransaction
transaction.successful() -> boolean
transaction.result() -> EconomyResultType
```
These method signatures were inspected using Java 21 `javap` and the bridge was
compiled against the user's actual version. The bridge uses none of Impactor's
internal implementation classes.

## Payout algorithm (fail closed)
1. Correct answer is accepted exactly once by the round engine.
2. Reward cap accounts for both pending and already-paid claims.
3. Append a unique `(round UUID, player UUID, amount)` PENDING row and fsync.
4. Resolve Impactor primary currency and player account asynchronously.
5. Dispatch deposit to Minecraft server-thread executor.
6. Check the transaction result; on success append/fsync PAID, then announce money.
7. Unknown/failure outcomes are marked MANUAL_REVIEW; never auto-retry.
8. If ledger finalization fails, existing PENDING remains; staff reconcile manually.

**This is at-most-once automatic payout attempt, not guaranteed exactly-once
atomic semantics across two independent storage systems.** Manual review is
required for crash windows and provider timeouts.

## Staging reward proposal, not active
SCRAMBLE 150 / EASY 100 / MEDIUM 200 / HARD 350 / ULTRA 500 PokéDollars.
Daily player cap proposal: 2000. Rewards from Minecraft combat, questlines,
and other sources remain separately controlled.

## Future modules
Use the same Pride Community event bus for: quizzes, Pokémon scramble, shiny
captures, legendary encounters, raid notices, Flashpoints/Operations/World Boss,
market events and vote notifications. Integrate each feature through real APIs
rather than intercepting arbitrary chat strings or modifying original upstream
JARs. Preserve existing LuckPerms rank glyphs/colors and v0.3.1 resource pack.
