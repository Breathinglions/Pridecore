package com.prideregion.community;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Persistent quiz wins independent of currency. One durable row per winning round;
 * duplicate round IDs cannot inflate totals after restart.
 * A corrupt file fails closed rather than displaying fictional statistics.
 */
public final class QuizLeaderboard {
    public record Entry(UUID player, String name, int triviaWins, int scrambleWins, long lastWinMillis) {
        public int wins() { return triviaWins + scrambleWins; }
    }
    private final Path file;
    private final Map<UUID, Entry> scores = new HashMap<>();
    private final Set<UUID> roundIds = new HashSet<>();

    public QuizLeaderboard(Path file) throws IOException {
        this.file = Objects.requireNonNull(file);
        Files.createDirectories(file.getParent());
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isEmpty()) continue;
                Win win = parse(line);
                if (roundIds.add(win.round())) accumulate(win);
            }
        }
    }

    private record Win(UUID round, UUID player, String name, QuizEngine.Kind kind, long when) { }

    /** Returns false if this round was already recorded. */
    public synchronized boolean record(UUID round, UUID player, String name,
                                       QuizEngine.Kind kind, long when) throws IOException {
        if (round == null || player == null || kind == null || when < 0 ||
            name == null || !name.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException("Invalid quiz win");
        }
        if (roundIds.contains(round)) return false;
        Win win = new Win(round, player, name, kind, when);
        byte[] data = (round + "\t" + player + "\t" + name + "\t" + kind + "\t" + when + "\n")
            .getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(file,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            ByteBuffer bytes = ByteBuffer.wrap(data);
            while (bytes.hasRemaining()) channel.write(bytes);
            channel.force(true);
        }
        roundIds.add(round);
        accumulate(win);
        return true;
    }

    private void accumulate(Win win) {
        Entry old = scores.get(win.player());
        int trivia = old == null ? 0 : old.triviaWins();
        int scramble = old == null ? 0 : old.scrambleWins();
        if (win.kind() == QuizEngine.Kind.TRIVIA) trivia++;
        else scramble++;
        scores.put(win.player(), new Entry(win.player(), win.name(), trivia, scramble, win.when()));
    }

    public synchronized List<Entry> top(int count) {
        return scores.values().stream()
            .sorted(Comparator.comparingInt(Entry::wins).reversed()
                .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Entry::player))
            .limit(Math.max(0, count)).toList();
    }

    public synchronized Optional<Entry> find(UUID player) {
        return Optional.ofNullable(scores.get(player));
    }

    private static Win parse(String line) throws IOException {
        String[] fields = line.split("\t", -1);
        if (fields.length != 5) throw new IOException("Malformed leaderboard row");
        try {
            UUID round = UUID.fromString(fields[0]);
            UUID player = UUID.fromString(fields[1]);
            String name = fields[2];
            if (!name.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("Invalid name");
            QuizEngine.Kind kind = QuizEngine.Kind.valueOf(fields[3]);
            long when = Long.parseLong(fields[4]);
            if (when < 0) throw new IllegalArgumentException("Invalid timestamp");
            return new Win(round, player, name, kind, when);
        } catch (RuntimeException e) {
            throw new IOException("Malformed leaderboard row; retain file for operator recovery", e);
        }
    }
}
