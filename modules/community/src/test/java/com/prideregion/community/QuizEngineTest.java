package com.prideregion.community;

import java.nio.file.*;
import java.util.*;

/** Framework-free smoke tests: javac QuizEngine.java RewardLedger.java QuizEngineTest.java && java ... */
public final class QuizEngineTest {
    private static int total;
    private static void check(boolean b, String description) {
        total++; if (!b) throw new AssertionError(description);
    }
    public static void main(String[] args) throws Exception {
        QuizEngine engine = new QuizEngine(500);
        UUID player = UUID.randomUUID();
        UUID rival = UUID.randomUUID();
        var question = new QuizEngine.Question("q1", QuizEngine.Kind.SCRAMBLE,
            "Unscramble", "Pikachu", List.of("Pika chu"), QuizEngine.Difficulty.EASY, 100);
        check(question.accepts("PIKACHU"), "case-insensitive accepted");
        check(question.accepts("Pika chu!!!"), "spacing punctuation accepted");
        check(!question.accepts("Charmander"), "wrong not accepted");
        check(QuizEngine.normalize("  Póké Mon! ").equals("pokemon"), "diacritic normalized");
        var round = engine.start(question, 1000, 30);
        check(round.challenge().length() == 7, "scrambled correctly sized");
        check(!round.challenge().equalsIgnoreCase("Pikachu"), "challenge differs");
        check(engine.submit(player,"One","wrong",1100).status() == QuizEngine.Status.INCORRECT, "wrong is non-winning");
        check(engine.submit(player,"One","PIKACHU",1200).status() == QuizEngine.Status.TOO_FAST, "spam cooldown");
        check(engine.submit(rival,"Two","pikachu",1300).status() == QuizEngine.Status.WIN, "first valid wins");
        check(engine.submit(player,"One","pikachu",1400).status() == QuizEngine.Status.NO_ROUND, "only one winner");
        check(engine.expire(31001).isEmpty(), "winning round never expires again");
        var shortRound = engine.start(question, 40000, 5);
        check(engine.submit(player,"One","Pikachu",45000).status() == QuizEngine.Status.EXPIRED, "deadline exclusive");
        check(engine.expire(45000).isPresent(), "timeout announced once");
        check(engine.active().isEmpty(), "expired state cleared");
        check(engine.start(question, 50000, 10) != null, "new round starts");
        try { engine.start(question, 50001, 10); throw new AssertionError("started second simultaneous round"); }
        catch (IllegalStateException expected) { total++; }
        engine.cancel();
        check(engine.active().isEmpty(), "cancel clears");
        Path tmp = Files.createTempDirectory("pride-community-ledger-test").resolve("ledger.tsv");
        RewardLedger ledger = new RewardLedger(tmp);
        UUID roundId = UUID.randomUUID();
        check(ledger.reserve(player, roundId, 100, 250).isPresent(), "reserved");
        check(ledger.reserve(player, roundId, 100, 250).isEmpty(), "duplicate blocked");
        check(ledger.reserve(player, UUID.randomUUID(), 200, 250).isEmpty(), "daily cap blocked");
        ledger = new RewardLedger(tmp);
        check(ledger.reservedOrPaidToday(player, java.time.LocalDate.now(java.time.ZoneOffset.UTC)) == 100, "persistent cap after restart");
        check(ledger.unresolved().size() == 1, "unresolved is not retried silently");
        ledger.update(roundId.toString() + ":" + player, RewardLedger.State.PAID);
        ledger = new RewardLedger(tmp);
        check(ledger.unresolved().isEmpty(), "confirmed payment persists");
        check(ledger.reserve(player, roundId, 100, 250).isEmpty(), "paid transaction cannot repeat");
        System.out.println("PASS " + total + " Pride Community core checks");
    }
}
