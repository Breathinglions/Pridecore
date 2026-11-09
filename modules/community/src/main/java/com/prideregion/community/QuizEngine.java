package com.prideregion.community;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.*;

/** Server-thread-owned, platform-independent round engine. No currency writes. */
public final class QuizEngine {
    public enum Kind { SCRAMBLE, TRIVIA }
    public enum Difficulty { EASY, MEDIUM, HARD, ULTRA }
    public enum Status { NO_ROUND, INCORRECT, TOO_FAST, EXPIRED, WIN }
    public record Question(String id, Kind kind, String prompt, String answer,
                           List<String> aliases, Difficulty difficulty, int points) {
        public Question {
            if (id == null || id.isBlank() || answer == null || answer.isBlank()
                    || kind == null || difficulty == null || points < 0) throw new IllegalArgumentException("Invalid question");
            if (aliases == null) aliases = List.of();
            aliases = List.copyOf(aliases);
        }
        public boolean accepts(String guess) {
            String candidate = normalize(guess);
            if (candidate.isBlank()) return false;
            if (candidate.equals(normalize(answer))) return true;
            for (String alias : aliases) if (candidate.equals(normalize(alias))) return true;
            return false;
        }
    }
    public record Round(UUID id, Question question, String challenge, long startedAt, long expiresAt) { }
    public record Result(Status status, Round round, UUID player, String playerName) { }
    private final SecureRandom random = new SecureRandom();
    private final Map<UUID, Long> lastGuessAt = new HashMap<>();
    private Round active;
    private boolean claimed;
    private final long guessCooldownMs;

    public QuizEngine(long guessCooldownMs) { this.guessCooldownMs = Math.max(0, guessCooldownMs); }
    public synchronized Optional<Round> active() { return Optional.ofNullable(active); }
    public synchronized Round start(Question question, long nowMillis, long durationSeconds) {
        if (active != null) throw new IllegalStateException("A round is already active");
        if (durationSeconds < 5 || durationSeconds > 600) throw new IllegalArgumentException("Bad duration");
        String challenge = question.kind() == Kind.SCRAMBLE ? scramble(question.answer()) : question.prompt();
        active = new Round(UUID.randomUUID(), question, challenge, nowMillis,
                Math.addExact(nowMillis, durationSeconds * 1000L));
        claimed = false;
        lastGuessAt.clear();
        return active;
    }
    public synchronized Result submit(UUID player, String name, String message, long nowMillis) {
        if (active == null || claimed) return new Result(Status.NO_ROUND, null, player, name);
        Round round = active;
        if (nowMillis >= round.expiresAt()) return new Result(Status.EXPIRED, round, player, name); // tick broadcasts timeout once
        Long prev = lastGuessAt.get(player);
        if (prev != null && nowMillis - prev < guessCooldownMs) return new Result(Status.TOO_FAST, round, player, name);
        lastGuessAt.put(player, nowMillis);
        if (!round.question().accepts(message)) return new Result(Status.INCORRECT, round, player, name);
        claimed = true;
        active = null;
        return new Result(Status.WIN, round, player, name);
    }
    public synchronized Optional<Round> expire(long nowMillis) {
        if (active == null || claimed || nowMillis < active.expiresAt()) return Optional.empty();
        Round expired = active;
        active = null;
        return Optional.of(expired);
    }
    public synchronized Optional<Round> cancel() {
        Round old = active;
        active = null;
        return Optional.ofNullable(old);
    }
    private String scramble(String answer) {
        String clean = answer.replaceAll("[^\\p{L}\\p{N}]", "").toUpperCase(Locale.ROOT);
        if (clean.length() < 3 || clean.codePoints().distinct().count() < 2)
            throw new IllegalArgumentException("Unscrambleable answer: " + answer);
        char[] result = clean.toCharArray();
        for (int round = 0; round < 16; round++) {
            for (int i = result.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1); char temp = result[i]; result[i] = result[j]; result[j] = temp;
            }
            if (!clean.equals(new String(result))) return new String(result);
        }
        throw new IllegalStateException("Unable to scramble answer: " + answer);
    }
    public static String normalize(String input) {
        if (input == null) return "";
        String s = Normalizer.normalize(input, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        return s.replaceAll("\\p{M}", "").replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
