package com.prideregion.community;

import java.nio.file.*;
import java.util.*;

public final class QuizLeaderboardTest {
    private static int checked;
    private static void check(boolean condition, String description) {
        checked++;
        if (!condition) throw new AssertionError(description);
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("pride-community-leaderboard-");
        Path file = dir.resolve("leaderboard.tsv");
        UUID alex = UUID.randomUUID();
        UUID kai = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        var leaderboard = new QuizLeaderboard(file);
        check(leaderboard.top(10).isEmpty(), "empty leaderboard");
        check(leaderboard.record(a, alex, "TrainerAlex", QuizEngine.Kind.TRIVIA, 100), "trivia win");
        check(!leaderboard.record(a, alex, "TrainerAlex", QuizEngine.Kind.TRIVIA, 100), "same round duplicate rejected");
        check(leaderboard.record(b, kai, "TrainerKai", QuizEngine.Kind.SCRAMBLE, 200), "other player");
        check(leaderboard.record(c, alex, "TrainerAlex", QuizEngine.Kind.SCRAMBLE, 300), "mixed round");
        check(leaderboard.find(alex).orElseThrow().triviaWins() == 1, "trivia count");
        check(leaderboard.find(alex).orElseThrow().scrambleWins() == 1, "scramble count");
        check(leaderboard.find(alex).orElseThrow().wins() == 2, "total wins");
        check(leaderboard.top(1).get(0).player().equals(alex), "ranking");
        check(leaderboard.top(1).size() == 1, "limit");
        leaderboard = new QuizLeaderboard(file);
        check(leaderboard.find(alex).orElseThrow().wins() == 2, "restart preserved scores");
        check(!leaderboard.record(a, alex, "TrainerAlex", QuizEngine.Kind.TRIVIA, 350), "restart dedupe");
        check(leaderboard.top(10).size() == 2, "persistent two players");
        try {
            leaderboard.record(UUID.randomUUID(), alex, "bad\\tname", QuizEngine.Kind.TRIVIA, 1);
            throw new AssertionError("invalid name accepted");
        } catch (IllegalArgumentException expected) { checked++; }
        Files.writeString(file, "\ninvalid-data\n", StandardOpenOption.APPEND);
        try {
            new QuizLeaderboard(file);
            throw new AssertionError("corrupt leaderboard accepted");
        } catch (java.io.IOException expected) { checked++; }
        System.out.println("PASS " + checked + " Pride Community leaderboard checks");
    }
}
