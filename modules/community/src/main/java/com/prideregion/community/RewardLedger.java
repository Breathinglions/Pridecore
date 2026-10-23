package com.prideregion.community;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Append-only reward claims. Reserving is durable before any external payment.
 * Unknown/PENDING claims are NEVER auto-retried: reconcile against the Impactor
 * account history first. This avoids duplicate payouts on a server crash.
 */
public final class RewardLedger {
    public enum State { PENDING, PAID, MANUAL_REVIEW }
    public record Claim(String transaction, UUID player, String roundId, int amount, LocalDate date, State state) { }
    private final Path file;
    private final Map<String, Claim> latest = new HashMap<>();
    public RewardLedger(Path file) throws IOException {
        this.file = file;
        Files.createDirectories(file.getParent());
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                Claim claim = fromLine(line);
                latest.put(claim.transaction(), claim);
            }
        }
    }
    public synchronized int reservedOrPaidToday(UUID player, LocalDate day) {
        return latest.values().stream().filter(c -> c.player().equals(player) && c.date().equals(day))
                .filter(c -> c.state() != null).mapToInt(Claim::amount).sum();
    }
    public synchronized Optional<Claim> reserve(UUID player, UUID roundId, int amount, int dailyCap) throws IOException {
        if (amount <= 0 || dailyCap <= 0) return Optional.empty();
        String id = roundId.toString() + ":" + player;
        if (latest.containsKey(id)) return Optional.empty();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (reservedOrPaidToday(player, today) + amount > dailyCap) return Optional.empty();
        Claim claim = new Claim(id, player, roundId.toString(), amount, today, State.PENDING);
        append(claim); latest.put(id, claim); return Optional.of(claim);
    }
    public synchronized void update(String id, State state) throws IOException {
        Claim claim = latest.get(id);
        if (claim == null || state == State.PENDING) throw new IllegalArgumentException("Invalid claim transition");
        if (claim.state() == State.PAID && state != State.PAID) throw new IllegalArgumentException("Paid claim is final");
        Claim next = new Claim(id, claim.player(), claim.roundId(), claim.amount(), claim.date(), state);
        append(next); latest.put(id, next);
    }
    public synchronized Collection<Claim> unresolved() {
        return latest.values().stream().filter(c -> c.state() != State.PAID).toList();
    }
    private void append(Claim claim) throws IOException {
        byte[] b = (String.join("\t", claim.transaction(), claim.player().toString(), claim.roundId(),
                Integer.toString(claim.amount()), claim.date().toString(), claim.state().name()) + "\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
            ByteBuffer bb = ByteBuffer.wrap(b); while (bb.hasRemaining()) ch.write(bb); ch.force(true);
        }
    }
    private Claim fromLine(String line) throws IOException {
        String[] f = line.split("\t", -1);
        if (f.length != 6) throw new IOException("Corrupt ledger row; rewards disabled until reconciled");
        try { return new Claim(f[0], UUID.fromString(f[1]), f[2], Integer.parseInt(f[3]), LocalDate.parse(f[4]), State.valueOf(f[5])); }
        catch (RuntimeException ex) { throw new IOException("Corrupt ledger row; rewards disabled until reconciled", ex); }
    }
}
