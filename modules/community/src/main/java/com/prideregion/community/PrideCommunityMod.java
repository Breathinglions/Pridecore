package com.prideregion.community;

import com.google.gson.*;
import com.prideregion.community.economy.ImpactorWalletBridge;
import com.prideregion.community.economy.RewardPayoutCoordinator;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.util.*;

/**
 * Fabric adapter for original Pride Community quiz engine.
 * The source is a development preview: Gradle/Minecraft compilation still needs QA.
 * Payouts are disabled by default; the 5.3.5 adapter and durable ledger
 * require in-server staging verification before enabling.
 */
public final class PrideCommunityMod implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("Pride Community");
    private final QuizEngine engine = new QuizEngine(500);
    private final Random random = new Random();
    private List<QuizEngine.Question> questions = List.of();
    private boolean autoRounds = false;
    private int intervalSeconds = 300;
    private int durationSeconds = 30;
    private int minimumPlayers = 1;
    private long lastRoundMillis = 0;
    private boolean payoutsEnabled = false;
    private int dailyRewardCap = 2000;
    private final EnumMap<QuizEngine.Difficulty, Integer> rewardTiers = new EnumMap<>(QuizEngine.Difficulty.class);
    private int scrambleReward = 150;
    private RewardPayoutCoordinator payoutCoordinator;
    private QuizLeaderboard leaderboard;
    private final Set<String> recentIds = new LinkedHashSet<>();
    private final Path directory = FabricLoader.getInstance().getConfigDir().resolve("pride_community");

    @Override public void onInitialize() {
        loadConfiguration();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            lastRoundMillis = System.currentTimeMillis();
            try {
                leaderboard = new QuizLeaderboard(directory.resolve("leaderboard.tsv"));
            } catch (Exception problem) {
                leaderboard = null;
                LOG.error("Quiz leaderboard unavailable: scoreboard disabled, retain file for recovery", problem);
            }
            try {
                // A failed/corrupt ledger must never permit payments.
                var ledger = new RewardLedger(directory.resolve("rewards.tsv"));
                payoutCoordinator = new RewardPayoutCoordinator(ledger,
                    new ImpactorWalletBridge(server::execute), dailyRewardCap);
                if (payoutCoordinator.unresolvedCount() > 0) {
                    LOG.warn("{} unresolved quiz payouts require manual reconciliation; never retry automatically",
                        payoutCoordinator.unresolvedCount());
                }
                LOG.info("Pride Community: Impactor wallet adapter ready; payoutsEnabled={}", payoutsEnabled);
            } catch (Exception problem) {
                payoutsEnabled = false;
                payoutCoordinator = null;
                LOG.error("Reward ledger unavailable: economy payouts DISABLED", problem);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            // The player UUID comes from Minecraft, never from the chat text.
            QuizEngine.Result outcome = engine.submit(sender.getUuid(), sender.getName().getString(),
                    message.getContent().getString(), System.currentTimeMillis());
            if (outcome.status() != QuizEngine.Status.WIN) return true;
            onWinner(sender.getServer(), outcome);
            // Do not show the plain winning answer a second time in ordinary chat.
            return false;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(CommandManager.literal("pridequiz")
                .then(CommandManager.literal("force").requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.literal("scramble").executes(ctx -> force(ctx.getSource().getServer(), QuizEngine.Kind.SCRAMBLE)))
                    .then(CommandManager.literal("trivia").executes(ctx -> force(ctx.getSource().getServer(), QuizEngine.Kind.TRIVIA))))
                .then(CommandManager.literal("stop").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
                    engine.cancel(); broadcast(ctx.getSource().getServer(), "EVENT", Formatting.GRAY, "Quiz round cancelled."); return 1;
                }))
                .then(CommandManager.literal("reload").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
                    if (engine.active().isPresent()) return 0;
                    loadConfiguration(); return 1;
                }))
                .then(CommandManager.literal("leaderboard").executes(ctx -> {
                    showLeaderboard(ctx.getSource()); return 1;
                }))
                .then(CommandManager.literal("status").executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> Text.literal("Pride Community: " +
                        (engine.active().isPresent() ? "round active" : "idle") + ", Impactor payouts=" + payoutsEnabled), false);
                    return 1;
                })));
            dispatcher.register(CommandManager.literal("prideannounce").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("channel", StringArgumentType.word())
                    .then(CommandManager.argument("message", StringArgumentType.greedyString()).executes(ctx -> {
                        String channel = StringArgumentType.getString(ctx,"channel").toUpperCase(Locale.ROOT);
                        if (!Set.of("PRIDE", "EVENT", "SHINY", "RAID", "LEGENDARY", "WORLD-BOSS").contains(channel)) return 0;
                        String body = StringArgumentType.getString(ctx,"message");
                        if (body.length() > 220) return 0;
                        broadcast(ctx.getSource().getServer(), channel, Formatting.AQUA, body);
                        return 1;
                    }))));
        });
        LOG.info("Pride Community dev preview loaded; payoutsEnabled={}", payoutsEnabled);
    }
    private void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        engine.expire(now).ifPresent(expired -> {
            broadcast(server, "QUIZ", Formatting.YELLOW,
                "Time's up! The answer was " + expired.question().answer() + ".");
            lastRoundMillis = now;
        });
        if (!autoRounds || engine.active().isPresent() ||
            server.getPlayerManager().getPlayerList().size() < minimumPlayers ||
            now - lastRoundMillis < intervalSeconds * 1000L) return;
        QuizEngine.Kind next = random.nextBoolean() ? QuizEngine.Kind.SCRAMBLE : QuizEngine.Kind.TRIVIA;
        force(server, next);
    }
    private int force(MinecraftServer server, QuizEngine.Kind kind) {
        if (engine.active().isPresent()) return 0;
        List<QuizEngine.Question> options = questions.stream().filter(q -> q.kind() == kind).toList();
        if (options.isEmpty()) return 0;
        List<QuizEngine.Question> fresh = options.stream().filter(q -> !recentIds.contains(q.id())).toList();
        if (fresh.isEmpty()) { recentIds.clear(); fresh = options; }
        QuizEngine.Question q = fresh.get(random.nextInt(fresh.size()));
        recentIds.add(q.id());
        if (recentIds.size() > 12) recentIds.remove(recentIds.iterator().next());
        QuizEngine.Round round = engine.start(q, System.currentTimeMillis(), durationSeconds);
        String kindLabel = kind == QuizEngine.Kind.SCRAMBLE ? "SCRAMBLE" : "QUIZ";
        broadcast(server, kindLabel, Formatting.GOLD, "[" + q.difficulty() + "] " +
            (kind == QuizEngine.Kind.SCRAMBLE ? "Unscramble this Pokemon: " + round.challenge() : round.challenge()));
        broadcast(server, kindLabel, Formatting.GRAY,
            "First correct answer wins. You have " + durationSeconds + " seconds.");
        return 1;
    }
    private void onWinner(MinecraftServer server, QuizEngine.Result result) {
        String tag = result.round().question().kind() == QuizEngine.Kind.SCRAMBLE ? "SCRAMBLE" : "QUIZ";
        String winnerText = " answered " + result.round().question().answer() + " correctly!";
        Text styledName = RankName.display(result.player(), result.playerName());
        lastRoundMillis = System.currentTimeMillis();
        if (leaderboard != null) {
            try {
                leaderboard.record(result.round().id(), result.player(), result.playerName(),
                    result.round().question().kind(), lastRoundMillis);
            } catch (Exception problem) {
                LOG.error("Failed to durably record quiz leaderboard win; retain file and investigate", problem);
            }
        }
        if (!payoutsEnabled || payoutCoordinator == null) {
            broadcastWinner(server, tag, styledName, winnerText + " (economy rewards off)");
            return;
        }
        int amount = result.round().question().kind() == QuizEngine.Kind.SCRAMBLE
                ? scrambleReward : rewardTiers.getOrDefault(result.round().question().difficulty(), 0);
        if (amount <= 0) {
            broadcastWinner(server, tag, styledName, winnerText + " (no prize configured)");
            return;
        }
        payoutCoordinator.award(result.player(), result.round().id(), amount).whenComplete((paid, error) ->
            server.execute(() -> {
                if (error != null || paid == null) {
                    LOG.error("Quiz payout failed unexpectedly for round {}", result.round().id(), error);
                    broadcastWinner(server, tag, styledName, winnerText + " Payment needs staff review.");
                    return;
                }
                switch (paid.outcome()) {
                    case PAID -> broadcastWinner(server, tag, styledName,
                        winnerText + " Earned " + amount + " PokéDollars!");
                    case BLOCKED -> broadcastWinner(server, tag, styledName,
                        winnerText + " Daily cap reached or reward already processed.");
                    case REVIEW, LEDGER_ERROR -> {
                        LOG.error("Pride quiz payout needs manual reconciliation: {} {}",
                            paid.transactionId(), paid.diagnostic());
                        broadcastWinner(server, tag, styledName,
                            winnerText + " Reward processing needs staff review.");
                    }
                }
            }));
    }
    private static void broadcastWinner(MinecraftServer server, String tag, Text styledName, String trailing) {
        Text message = Text.empty()
            .append(Text.literal("[" + tag + "] ").formatted(Formatting.GOLD, Formatting.BOLD))
            .append(styledName.copy())
            .append(Text.literal(trailing).formatted(Formatting.WHITE));
        server.getPlayerManager().broadcast(message, false);
    }
    private void showLeaderboard(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("[QUIZ LEADERBOARD]").formatted(Formatting.GOLD, Formatting.BOLD), false);
        if (leaderboard == null) {
            source.sendFeedback(() -> Text.literal("Leaderboard unavailable; check server logs.").formatted(Formatting.RED), false);
            return;
        }
        List<QuizLeaderboard.Entry> top = leaderboard.top(10);
        if (top.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No quiz wins recorded yet.").formatted(Formatting.GRAY), false);
            return;
        }
        int position = 1;
        for (QuizLeaderboard.Entry entry : top) {
            final int rank = position++;
            Text identity = RankName.display(entry.player(), entry.name());
            Text line = Text.empty()
                .append(Text.literal("#" + rank + " ").formatted(Formatting.YELLOW))
                .append(identity)
                .append(Text.literal("  " + entry.wins() + " wins  (Trivia "
                    + entry.triviaWins() + " | Scramble " + entry.scrambleWins() + ")")
                    .formatted(Formatting.GRAY));
            source.sendFeedback(() -> line, false);
        }
    }
    private static void broadcast(MinecraftServer server, String tag, Formatting color, String message) {
        Text txt = Text.empty().append(Text.literal("[" + tag + "] ").formatted(color, Formatting.BOLD))
                          .append(Text.literal(message).formatted(Formatting.WHITE));
        server.getPlayerManager().broadcast(txt, false);
    }
    private void loadConfiguration() {
        try {
            Files.createDirectories(directory);
            Path config = directory.resolve("config.json");
            Path bank = directory.resolve("questions.json");
            if (!Files.exists(config)) Files.writeString(config, PrideCommunityMod.class.getResourceAsStream("/defaults/config.json") == null ? "{}" :
                new String(Objects.requireNonNull(PrideCommunityMod.class.getResourceAsStream("/defaults/config.json")).readAllBytes()));
            if (!Files.exists(bank)) Files.writeString(bank,
                new String(Objects.requireNonNull(PrideCommunityMod.class.getResourceAsStream("/defaults/questions.json")).readAllBytes()));
            JsonObject settings = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
            autoRounds = settings.has("autoRounds") && settings.get("autoRounds").getAsBoolean();
            intervalSeconds = between(settings, "intervalSeconds", 300, 60, 86400);
            durationSeconds = between(settings, "answerSeconds", 30, 5, 600);
            minimumPlayers = between(settings, "minimumPlayers", 1, 1, 100);
            JsonObject rewards = settings.getAsJsonObject("rewards");
            if (rewards == null) throw new IllegalArgumentException("Missing reward config");
            String backend = rewards.get("economyBackend").getAsString();
            if (!Set.of("DISABLED", "IMPACTOR").contains(backend))
                throw new IllegalArgumentException("Unsupported economy backend " + backend);
            boolean wantPayouts = rewards.get("payoutsEnabled").getAsBoolean();
            payoutsEnabled = wantPayouts && backend.equals("IMPACTOR");
            dailyRewardCap = between(rewards, "dailyCapPokeDollars", 2000, 1, 100000);
            JsonObject tiers = rewards.getAsJsonObject("tiers");
            rewardTiers.clear();
            for (QuizEngine.Difficulty diff : QuizEngine.Difficulty.values()) {
                rewardTiers.put(diff, between(tiers, diff.name(), 0, 0, dailyRewardCap));
            }
            scrambleReward = between(tiers, "SCRAMBLE", 150, 0, dailyRewardCap);
            // Reload never resets the ledger; restart required if cap changes while payouts are on.
            if (payoutsEnabled && payoutCoordinator != null)
                LOG.warn("Payout config reloaded. Restart to apply any daily-cap change safely.");
            JsonArray input = JsonParser.parseString(Files.readString(bank)).getAsJsonArray();
            List<QuizEngine.Question> built = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (JsonElement el : input) {
                JsonObject v = el.getAsJsonObject();
                String id = v.get("id").getAsString();
                if (!ids.add(id)) throw new IllegalArgumentException("Duplicate question " + id);
                List<String> aliases = new ArrayList<>();
                if (v.has("aliases")) v.getAsJsonArray("aliases").forEach(a -> aliases.add(a.getAsString()));
                built.add(new QuizEngine.Question(id, QuizEngine.Kind.valueOf(v.get("kind").getAsString().toUpperCase(Locale.ROOT)),
                    v.get("prompt").getAsString(), v.get("answer").getAsString(), aliases,
                    QuizEngine.Difficulty.valueOf(v.get("difficulty").getAsString().toUpperCase(Locale.ROOT)), 0));
            }
            questions = List.copyOf(built);
            LOG.info("Pride Community loaded {} original questions. autoRounds={}", questions.size(), autoRounds);
        } catch (Exception ex) {
            autoRounds = false; payoutsEnabled = false; questions = List.of();
            LOG.error("Failed to load Pride Community configuration: quiz disabled", ex);
        }
    }
    private static int between(JsonObject src, String key, int fallback, int min, int max) {
        int value = src.has(key) ? src.get(key).getAsInt() : fallback;
        if (value < min || value > max) throw new IllegalArgumentException("Invalid " + key);
        return value;
    }
}
