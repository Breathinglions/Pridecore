package com.prideregion.core.story;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Atomic, strictly validated per-player in-progress finale checkpoint storage. */
public final class ResonanceCheckpointStore {
    private final Path directory;
    public ResonanceCheckpointStore(Path directory) { this.directory = Objects.requireNonNull(directory); }

    public void save(ResonanceFinaleController.Checkpoint cp) throws IOException {
        Objects.requireNonNull(cp);
        Files.createDirectories(directory);
        Path target = directory.resolve(cp.player() + ".checkpoint");
        Path tmp = Files.createTempFile(directory, ".resonance-", ".tmp");
        try {
            // A strict, compact format with a fixed phase and limited marker set.
            String serialized = "PRIDE_FINALE_V1\n" + cp.player() + "\n" +
                cp.phase() + "\n" + String.join(",", new TreeSet<>(cp.conduits())) +
                "\n" + String.join(",", new TreeSet<>(cp.shields())) + "\n";
            Files.writeString(tmp, serialized);
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(tmp); }
    }

    public Optional<ResonanceFinaleController.Checkpoint> load(UUID player) throws IOException {
        Path path = directory.resolve(player + ".checkpoint");
        if (!Files.exists(path)) return Optional.empty();
        if (Files.isSymbolicLink(path) || Files.size(path) > 512)
            throw new IOException("Unsafe finale checkpoint file");
        List<String> lines = Files.readAllLines(path);
        if (lines.size() != 5 || !"PRIDE_FINALE_V1".equals(lines.get(0))
                || !player.toString().equals(lines.get(1)))
            throw new IOException("Finale checkpoint format mismatch");
        try {
            return Optional.of(new ResonanceFinaleController.Checkpoint(
                player, ResonanceFinaleController.Phase.valueOf(lines.get(2)),
                markers(lines.get(3)), markers(lines.get(4))));
        } catch (RuntimeException ex) {
            throw new IOException("Invalid saved finale checkpoint", ex);
        }
    }
    private static Set<String> markers(String value) {
        if (value.isEmpty()) return Set.of();
        String[] parts = value.split(",", -1);
        Set<String> result = new HashSet<>(Arrays.asList(parts));
        if (result.size() != parts.length || result.contains(""))
            throw new IllegalArgumentException("Repeated or empty marker");
        return Set.copyOf(result);
    }
    public void delete(UUID player) throws IOException {
        Path file = directory.resolve(player + ".checkpoint");
        if (Files.isSymbolicLink(file)) throw new IOException("Unsafe checkpoint symlink");
        Files.deleteIfExists(file);
    }
}
