package com.prideregion.core.cinema;

import com.google.gson.*;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Configurable, server-authoritative enter-radius trigger for saved Cinematic
 * Director scenes. This component NEVER teleports or modifies player progress,
 * badges, NPCs, battle state or story stage.
 *
 * Trigger state is per UUID + zone. Reentry is required before a second fire;
 * once-per-player completions are written atomically outside normal profiles.
 */
public final class LocationCinematicTriggerModule {
    private static final Logger LOG = LoggerFactory.getLogger("PrideCinemaZones");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final int MAX_ZONES = 128;
    private static final int DEFAULT_COOLDOWN = 600;

    private record Zone(String id, String scene, String dimension, double x, double y,
                        double z, double radius, boolean enabled, boolean oncePerPlayer,
                        int cooldownTicks, List<String> requireTags, List<String> excludeTags) {
        boolean contains(ServerPlayerEntity player) {
            if (!dimension.equals(player.getWorld().getRegistryKey().getValue().toString()))
                return false;
            // Horizontal entrance radius plus a small vertical allowance:
            // prevents triggering from another gym floor or underground.
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            return Math.abs(player.getY() - y) <= 4.0
                && dx * dx + dz * dz <= radius * radius;
        }
        boolean eligible(ServerPlayerEntity player) {
            Set<String> tags = player.getCommandTags();
            return tags.containsAll(requireTags)
                && excludeTags.stream().noneMatch(tags::contains);
        }
    }

    private static MinecraftServer server;
    private static Path zonesDir;
    private static Path completedDir;
    private static final Map<String, Zone> zones = new LinkedHashMap<>();
    private static final Map<String, Set<UUID>> completed = new HashMap<>();
    private static final Map<UUID, Set<String>> currentlyInside = new HashMap<>();
    private static final Map<String, Long> nextAllowedTick = new HashMap<>();
    private static long scannedTicks;

    private LocationCinematicTriggerModule() {}

    /** Registered once; no recurring task when the server is stopped. */
    public static void registerHooks() {
        ServerTickEvents.END_SERVER_TICK.register(LocationCinematicTriggerModule::tick);
    }

    public static void start(MinecraftServer active) throws IOException {
        stop();
        Path base = active.getSavePath(WorldSavePath.ROOT)
            .resolve("data").resolve("pride_core").resolve("cinema_triggers");
        zonesDir = base.resolve("zones");
        completedDir = base.resolve("completed");
        Files.createDirectories(zonesDir);
        Files.createDirectories(completedDir);
        server = active;
        try (var entries = Files.list(zonesDir)) {
            for (Path file : entries.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted().limit(MAX_ZONES + 1).toList()) {
                if (zones.size() >= MAX_ZONES) {
                    LOG.warn("More than {} cinema zones; remaining zones disabled", MAX_ZONES);
                    break;
                }
                try {
                    String name = file.getFileName().toString().replaceFirst("\\.json$", "");
                    Zone zone = readZone(file);
                    if (!zone.id().equals(name))
                        throw new IllegalArgumentException("zone filename/id mismatch");
                    zones.put(zone.id(), zone);
                    completed.put(zone.id(), readCompleted(zone.id()));
                } catch (Exception invalid) {
                    LOG.warn("Ignoring malformed cinematic trigger file {}: {}", file, invalid.toString());
                }
            }
        }
        LOG.info("Cinematic Location Triggers loaded: {} configured, {} enabled",
            zones.size(), zones.values().stream().filter(Zone::enabled).count());
    }

    public static void stop() {
        server = null;
        zonesDir = null;
        completedDir = null;
        zones.clear();
        completed.clear();
        currentlyInside.clear();
        nextAllowedTick.clear();
        scannedTicks = 0;
    }

    /** These commands are nested under /pridecore cinema and require OP 4 there. */
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> commands() {
        var root = CommandManager.literal("zone");
        root.then(CommandManager.literal("create")
            .then(CommandManager.argument("id", StringArgumentType.word())
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("radius", DoubleArgumentType.doubleArg(2.0, 32.0))
            .executes(ctx -> create(ctx.getSource(),
                StringArgumentType.getString(ctx, "id"),
                StringArgumentType.getString(ctx, "scene"),
                DoubleArgumentType.getDouble(ctx, "radius")))))));
        root.then(CommandManager.literal("enable")
            .then(CommandManager.argument("id", StringArgumentType.word())
            .executes(ctx -> setEnabled(ctx.getSource(),
                StringArgumentType.getString(ctx, "id"), true))));
        root.then(CommandManager.literal("disable")
            .then(CommandManager.argument("id", StringArgumentType.word())
            .executes(ctx -> setEnabled(ctx.getSource(),
                StringArgumentType.getString(ctx, "id"), false))));
        root.then(CommandManager.literal("once")
            .then(CommandManager.argument("id", StringArgumentType.word())
            .then(CommandManager.argument("value", BoolArgumentType.bool())
            .executes(ctx -> setOnce(ctx.getSource(),
                StringArgumentType.getString(ctx, "id"),
                BoolArgumentType.getBool(ctx, "value"))))));
        root.then(CommandManager.literal("cooldown")
            .then(CommandManager.argument("id", StringArgumentType.word())
            .then(CommandManager.argument("ticks", IntegerArgumentType.integer(20, 72000))
            .executes(ctx -> setCooldown(ctx.getSource(),
                StringArgumentType.getString(ctx, "id"),
                IntegerArgumentType.getInteger(ctx, "ticks"))))));
        root.then(CommandManager.literal("list")
            .executes(ctx -> list(ctx.getSource())));
        root.then(CommandManager.literal("info")
            .then(CommandManager.argument("id", StringArgumentType.word())
            .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "id")))));
        root.then(CommandManager.literal("reset")
            .then(CommandManager.argument("id", StringArgumentType.word())
            .then(CommandManager.argument("player", EntityArgumentType.player())
            .executes(ctx -> reset(ctx.getSource(),
                StringArgumentType.getString(ctx, "id"),
                EntityArgumentType.getPlayer(ctx, "player"))))));
        root.then(CommandManager.literal("remove")
            .then(CommandManager.argument("id", StringArgumentType.word())
            .executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "id")))));
        return root;
    }

    private static int create(ServerCommandSource source, String id,
                              String scene, double radius) {
        try {
            validateId(id); validateId(scene);
            if (server == null || zonesDir == null) throw new IOException("zone module not started");
            ServerPlayerEntity player = source.getPlayerOrThrow();
            if (zones.containsKey(id))
                throw new IOException("Zone already exists. Use info, enable or remove: " + id);
            if (zones.size() >= MAX_ZONES) throw new IOException("Zone limit reached");
            // Avoid creating an enabled zone pointing at a mistyped scene.
            Path sceneFile = server.getSavePath(WorldSavePath.ROOT)
                .resolve("data").resolve("pride_core").resolve("cinematics")
                .resolve(scene + ".json");
            if (!Files.isRegularFile(sceneFile))
                throw new IOException("Capture the cinematic first: /pridecore cinema capture "
                    + scene + " entrance");
            Zone zone = new Zone(id, scene,
                player.getWorld().getRegistryKey().getValue().toString(),
                player.getX(), player.getY(), player.getZ(), radius,
                false, true, DEFAULT_COOLDOWN, List.of(), List.of());
            writeZone(zone);
            zones.put(id, zone);
            completed.put(id, readCompleted(id)); // preserve once-history when an id is recreated
            return ok(source, "Created DISABLED camera zone " + id
                + " (scene=" + scene + ", radius=" + radius + "). "
                + "Walk outside the radius, then /pridecore cinema zone enable " + id);
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int setEnabled(ServerCommandSource source, String id, boolean enabled) {
        try {
            Zone z = require(id);
            Zone changed = new Zone(z.id(), z.scene(), z.dimension(),
                z.x(), z.y(), z.z(), z.radius(), enabled, z.oncePerPlayer(),
                z.cooldownTicks(), z.requireTags(), z.excludeTags());
            writeZone(changed);
            zones.put(id, changed);
            return ok(source, "Location cinematic " + id + (enabled ? " ENABLED" : " DISABLED")
                + ". Trigger occurs when a player enters the radius.");
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int setOnce(ServerCommandSource source, String id, boolean once) {
        try {
            Zone z = require(id);
            Zone changed = new Zone(z.id(), z.scene(), z.dimension(),
                z.x(), z.y(), z.z(), z.radius(), z.enabled(), once,
                z.cooldownTicks(), z.requireTags(), z.excludeTags());
            writeZone(changed);
            zones.put(id, changed);
            return ok(source, id + " oncePerPlayer=" + once);
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int setCooldown(ServerCommandSource source, String id, int ticks) {
        try {
            Zone z = require(id);
            Zone changed = new Zone(z.id(), z.scene(), z.dimension(),
                z.x(), z.y(), z.z(), z.radius(), z.enabled(), z.oncePerPlayer(),
                ticks, z.requireTags(), z.excludeTags());
            writeZone(changed);
            zones.put(id, changed);
            return ok(source, id + " cooldown=" + ticks + " ticks");
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int info(ServerCommandSource source, String id) {
        try {
            Zone z = require(id);
            return ok(source, id + ": scene=" + z.scene() + " world=" + z.dimension()
                + " center=(" + z.x() + "," + z.y() + "," + z.z() + ")"
                + " radius=" + z.radius() + " enabled=" + z.enabled()
                + " once=" + z.oncePerPlayer() + " cooldown=" + z.cooldownTicks()
                + " completions=" + completed.getOrDefault(id, Set.of()).size());
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int list(ServerCommandSource source) {
        if (zones.isEmpty()) return ok(source, "No location cinematic zones. "
            + "Stand at an entrance: /pridecore cinema zone create <id> <scene> <radius>");
        StringBuilder out = new StringBuilder("Cinematic zones: ");
        for (Zone zone : zones.values()) {
            if (out.length() > 500) { out.append("..."); break; }
            out.append(zone.id()).append(" -> ").append(zone.scene())
                .append(zone.enabled() ? " [ON]; " : " [OFF]; ");
        }
        return ok(source, out.toString());
    }

    private static int reset(ServerCommandSource source, String id,
                             ServerPlayerEntity player) {
        try {
            require(id);
            Set<UUID> done = completed.computeIfAbsent(id, k -> new HashSet<>());
            boolean removed = done.remove(player.getUuid());
            if (removed) {
                try { writeCompleted(id, done); }
                catch (IOException failure) { done.add(player.getUuid()); throw failure; }
            }
            // A player standing inside must leave and re-enter for a fresh event.
            nextAllowedTick.remove(id + "/" + player.getUuid());
            return ok(source, "Cinematic zone " + id + " reset for "
                + player.getName().getString() + ". Leave and re-enter its radius to test.");
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int remove(ServerCommandSource source, String id) {
        try {
            require(id);
            Files.deleteIfExists(zonePath(id));
            zones.remove(id);
            completed.remove(id);
            currentlyInside.values().forEach(set -> set.remove(id));
            return ok(source, "Removed cinematic zone " + id
                + ". Existing once-per-player history is kept on disk for safety.");
        } catch (Exception ex) { return error(source, ex); }
    }

    private static void tick(MinecraftServer tickingServer) {
        if (server != tickingServer || (++scannedTicks % SCAN_INTERVAL_TICKS) != 0
                || zones.isEmpty()) return;
        if ((scannedTicks % 200) == 0) {
            Set<UUID> connected = new HashSet<>();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList())
                connected.add(player.getUuid());
            currentlyInside.keySet().retainAll(connected);
            nextAllowedTick.keySet().removeIf(k -> {
                String uuid = k.substring(k.lastIndexOf('/') + 1);
                try { return !connected.contains(UUID.fromString(uuid)); }
                catch (IllegalArgumentException ex) { return true; }
            });
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            Set<String> inside = currentlyInside.computeIfAbsent(playerId, k -> new HashSet<>());
            for (Zone zone : zones.values()) {
                boolean within = player.isAlive() && !player.isSpectator()
                    && zone.contains(player);
                boolean wasInside = inside.contains(zone.id());
                if (!within) {
                    inside.remove(zone.id());
                    continue;
                }
                inside.add(zone.id());
                // State is updated even while a zone is disabled. Enabling an
                // entrance under someone's feet never instantly traps them.
                if (wasInside || !zone.enabled() || !zone.eligible(player)) continue;
                if (zone.oncePerPlayer()
                        && completed.getOrDefault(zone.id(), Set.of()).contains(playerId))
                    continue;
                String key = zone.id() + "/" + playerId;
                if (server.getTicks() < nextAllowedTick.getOrDefault(key, 0L)) continue;
                if (CameraDirectorModule.cameraBusy(player)) continue;
                try {
                    if (!CameraDirectorModule.playFromLocation(player, zone.scene()))
                        continue;
                    nextAllowedTick.put(key,
                        (long) server.getTicks() + zone.cooldownTicks());
                    if (zone.oncePerPlayer()) {
                        Set<UUID> seen = completed.computeIfAbsent(zone.id(), k -> new HashSet<>());
                        seen.add(playerId);
                        try { writeCompleted(zone.id(), seen); }
                        catch (IOException persistenceFailure) {
                            LOG.error("Cinematic zone {} played for {}, but once-history "
                                + "could not be persisted: {}", zone.id(), playerId,
                                persistenceFailure.toString());
                        }
                    }
                    LOG.info("Cinematic zone {} played scene {} privately for {}",
                        zone.id(), zone.scene(), player.getName().getString());
                } catch (IOException | RuntimeException failure) {
                    LOG.warn("Zone {} could not play cinematic {} for {}: {}",
                        zone.id(), zone.scene(), playerId, failure.toString());
                }
            }
        }
    }

    private static Zone require(String id) {
        validateId(id);
        Zone z = zones.get(id);
        if (z == null) throw new IllegalArgumentException("Unknown cinematic zone: " + id);
        return z;
    }

    private static void validateId(String id) {
        if (id == null || !id.matches("[a-z0-9_-]{1,48}"))
            throw new IllegalArgumentException("Use lowercase letters, digits, underscore or dash");
    }

    private static Path zonePath(String id) {
        validateId(id);
        if (zonesDir == null) throw new IllegalStateException("Cinematic zones not started");
        return zonesDir.resolve(id + ".json");
    }

    private static JsonObject serialize(Zone z) {
        JsonObject o = new JsonObject();
        o.addProperty("schema", 1);
        o.addProperty("id", z.id());
        o.addProperty("scene", z.scene());
        o.addProperty("dimension", z.dimension());
        o.addProperty("x", z.x());
        o.addProperty("y", z.y());
        o.addProperty("z", z.z());
        o.addProperty("radius", z.radius());
        o.addProperty("enabled", z.enabled());
        o.addProperty("oncePerPlayer", z.oncePerPlayer());
        o.addProperty("cooldownTicks", z.cooldownTicks());
        JsonArray requires = new JsonArray();
        for (String tag : z.requireTags()) requires.add(tag);
        o.add("requireTags", requires);
        JsonArray excludes = new JsonArray();
        for (String tag : z.excludeTags()) excludes.add(tag);
        o.add("excludeTags", excludes);
        return o;
    }

    private static Zone readZone(Path file) throws IOException {
        JsonObject o;
        try { o = JsonParser.parseString(Files.readString(file)).getAsJsonObject(); }
        catch (RuntimeException ex) { throw new IOException("Invalid cinematic zone JSON", ex); }
        Zone z = new Zone(
            o.get("id").getAsString(),
            o.get("scene").getAsString(),
            o.get("dimension").getAsString(),
            o.get("x").getAsDouble(), o.get("y").getAsDouble(),
            o.get("z").getAsDouble(), o.get("radius").getAsDouble(),
            o.get("enabled").getAsBoolean(), o.get("oncePerPlayer").getAsBoolean(),
            o.get("cooldownTicks").getAsInt(),
            readTags(o, "requireTags"), readTags(o, "excludeTags"));
        validateZone(z);
        return z;
    }

    private static List<String> readTags(JsonObject o, String key) {
        if (!o.has(key)) return List.of();
        JsonArray array = o.getAsJsonArray(key);
        if (array.size() > 16) throw new IllegalArgumentException("Too many zone tags");
        ArrayList<String> result = new ArrayList<>();
        for (JsonElement element : array) result.add(element.getAsString());
        return List.copyOf(result);
    }

    private static void validateZone(Zone z) {
        validateId(z.id());
        validateId(z.scene());
        if (!z.dimension().matches("[a-z0-9_./-]+:[a-z0-9_./-]+")
                || z.dimension().length() > 100)
            throw new IllegalArgumentException("Invalid trigger dimension");
        if (!Double.isFinite(z.x()) || !Double.isFinite(z.y())
                || !Double.isFinite(z.z()) || !Double.isFinite(z.radius())
                || Math.abs(z.x()) > 29_999_984 || Math.abs(z.z()) > 29_999_984
                || z.y() < -64 || z.y() > 1024
                || z.radius() < 2 || z.radius() > 32)
            throw new IllegalArgumentException("Invalid cinematic zone coordinates/radius");
        if (z.cooldownTicks() < 20 || z.cooldownTicks() > 72000)
            throw new IllegalArgumentException("Invalid zone cooldown");
        if (z.requireTags().size() > 16 || z.excludeTags().size() > 16)
            throw new IllegalArgumentException("Too many zone condition tags");
        for (String tag : z.requireTags())
            if (!tag.matches("[A-Za-z0-9_:-]{1,64}"))
                throw new IllegalArgumentException("Invalid required player tag");
        for (String tag : z.excludeTags())
            if (!tag.matches("[A-Za-z0-9_:-]{1,64}"))
                throw new IllegalArgumentException("Invalid excluded player tag");
    }

    private static void writeZone(Zone z) throws IOException {
        validateZone(z);
        atomicWrite(zonePath(z.id()), JSON.toJson(serialize(z)));
    }

    private static Set<UUID> readCompleted(String id) throws IOException {
        Path path = completedDir.resolve(id + ".txt");
        if (!Files.isRegularFile(path)) return new HashSet<>();
        List<String> lines = Files.readAllLines(path);
        if (lines.size() > 100_000)
            throw new IOException("Too many cinema completions in " + path);
        HashSet<UUID> result = new HashSet<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) continue;
            try { result.add(UUID.fromString(line.strip())); }
            catch (IllegalArgumentException invalid) {
                throw new IOException("Malformed cinematic completion UUID", invalid);
            }
        }
        return result;
    }

    private static void writeCompleted(String id, Set<UUID> players) throws IOException {
        validateId(id);
        if (players.size() > 100_000) throw new IOException("Too many zone completions");
        ArrayList<String> lines = new ArrayList<>();
        for (UUID uuid : players) lines.add(uuid.toString());
        Collections.sort(lines);
        atomicWrite(completedDir.resolve(id + ".txt"), String.join("\n", lines) + "\n");
    }

    private static void atomicWrite(Path file, String data) throws IOException {
        Path tmp = Files.createTempFile(file.getParent(), "pride-cinema-", ".tmp");
        try {
            Files.writeString(tmp, data);
            try { Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, file, REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(tmp); }
    }

    private static int ok(ServerCommandSource source, String detail) {
        source.sendFeedback(() -> Text.literal(detail), false);
        return 1;
    }
    private static int error(ServerCommandSource source, Exception ex) {
        source.sendError(Text.literal("Cinematic Zone: " + ex.getMessage()));
        return 0;
    }
}
