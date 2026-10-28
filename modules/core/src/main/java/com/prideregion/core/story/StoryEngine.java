package com.prideregion.core.story;

import com.prideregion.core.ProfileState;
import com.prideregion.core.ProgressionService;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

/**
 * Session-safe story selection. All mutations go through Module 01. Never pays
 * rewards, fabricates starter ownership or changes existing Minecraft player tags.
 * Commands/callbacks must execute on the server's main thread.
 */
public final class StoryEngine {
    public record ChoiceView(String id, String text) {}
    public record Reply(String npcId, String npcName, String text,
                        List<ChoiceView> options, long revision) {}
    private record Session(String npcId, String nodeId) {}
    private final StoryDefinition story;
    private final ProgressionService progression;
    private final Function<UUID, Set<String>> externalFacts;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public StoryEngine(StoryDefinition story, ProgressionService progression) {
        this(story, progression, ignored -> Set.of());
    }

    public StoryEngine(StoryDefinition story, ProgressionService progression,
                       Function<UUID, Set<String>> externalFacts) {
        this.story = Objects.requireNonNull(story);
        this.progression = Objects.requireNonNull(progression);
        this.externalFacts = Objects.requireNonNull(externalFacts);
    }

    private Set<String> legacyFor(UUID player) {
        return LegacyOnboardingGate.filter(Objects.requireNonNull(
            externalFacts.apply(player), "Player markers unavailable"));
    }

    public synchronized Reply begin(UUID playerId, String npcId) throws IOException {
        var npc = story.npcs().get(npcId);
        if (npc == null) throw new IllegalArgumentException("Unknown NPC: " + npcId);
        ProfileState p = progression.get(playerId);
        if (!p.facts().containsAll(npc.requires()))
            throw new IllegalStateException("This NPC has no story dialogue available yet");
        String nodeId = p.facts().contains(npc.completedFact()) ? npc.repeat() : npc.start();
        sessions.put(playerId, new Session(npcId, nodeId));
        return reply(npc, npc.nodes().get(nodeId), p, legacyFor(playerId));
    }

    public synchronized Reply choose(UUID playerId, String optionId) throws IOException {
        var session = sessions.get(playerId);
        if (session == null) throw new IllegalStateException("Talk to the NPC first");
        var npc = story.npcs().get(session.npcId());
        var node = npc.nodes().get(session.nodeId());
        ProfileState before = progression.get(playerId);
        if (!before.facts().containsAll(npc.requires())) {
            sessions.remove(playerId);
            throw new IllegalStateException("Prerequisites changed; reopen the conversation");
        }
        var option = node.options().stream().filter(o -> o.id().equals(optionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown dialogue choice"));
        if (!before.facts().containsAll(option.requires()))
            throw new IllegalStateException("Choice prerequisites not met");
        if (!legacyFor(playerId).containsAll(option.requiresLegacy()))
            throw new IllegalStateException("Complete the existing starter, registration or field objective first");
        // Rin's introduction is an optional Route 1 encounter. Recording it
        // after Thorn's story introduction must never roll the active Gym /
        // forest objective backwards to "rival_introduced". Preserve Rin's
        // completed fact, but keep the current mainline stage intact.
        String destinationStage = option.stage();
        if ("rin".equals(npc.id())
                && "pride:rival_introduced".equals(destinationStage)
                && before.facts().contains("pride:met_thorn")) {
            destinationStage = null;
        }
        var change = progression.transition(playerId, destinationStage,
                option.grant(), option.revoke());
        if (option.next() == null) {
            sessions.remove(playerId);
            return new Reply(npc.id(), npc.name(), "Conversation finished. Your progress was saved.",
                    List.of(), change.profile().revision());
        }
        sessions.put(playerId, new Session(npc.id(), option.next()));
        return reply(npc, npc.nodes().get(option.next()), change.profile(), legacyFor(playerId));
    }

    /**
     * Safe migration of verified *existing* onboarding success markers into our
     * own story checkpoints. Invoked by native NPC actions and at player login.
     * This DOES NOT select Pokémon, issue items, write tags or complete FTB Quests.
     *
     * No player can bypass Mom or skip a missing prerequisite. This deliberately
     * handles only the three already-installed legacy onboarding services.
     */
    private record VerifiedStep(String npc, String choice, Set<String> legacyMarkers,
                                String previousStage, String stage) {}
    private static final List<VerifiedStep> ONBOARDING = List.of(
        new VerifiedStep("sequoia", "understood", Set.of("starter_chosen", "sequoia_met"),
                "pride:visit_sequoia", "pride:trainer_registration"),
        new VerifiedStep("theo", "next", Set.of("trainer_card_received"),
                "pride:trainer_registration", "pride:field_orientation"),
        new VerifiedStep("avery", "journey", Set.of("trainer_guide_received"),
                "pride:field_orientation", "pride:route1_orientation")
    );

    public synchronized List<String> reconcileOnboarding(UUID playerId) throws IOException {
        Set<String> verified = legacyFor(playerId); // read-only, per-player
        List<String> advanced = new ArrayList<>();
        for (VerifiedStep step : ONBOARDING) {
            ProfileState p = progression.get(playerId);
            var npc = story.npcs().get(step.npc());
            if (npc == null) continue; // custom content can omit an NPC
            if (p.facts().contains(npc.completedFact())) continue; // idempotent
            if (!p.facts().containsAll(npc.requires())) break; // preserve Mom/sequence
            if (!verified.containsAll(step.legacyMarkers())) break; // legacy service incomplete
            if (!step.previousStage().equals(p.storyStage())) break; // never roll story backwards

            // Verify that configuration still represents the expected safe step.
            var start = npc.nodes().get(npc.start());
            if (start == null) throw new IllegalStateException("Onboarding node missing");
            var option = start.options().stream()
                .filter(o -> o.id().equals(step.choice())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Onboarding action missing"));
            if (!step.stage().equals(option.stage())
                    || !option.grant().contains(npc.completedFact())
                    || !verified.containsAll(option.requiresLegacy())
                    || !p.facts().containsAll(option.requires())
                    || !option.revoke().isEmpty())
                throw new IllegalStateException("Onboarding definition changed; automatic sync refused");

            var result = progression.transition(playerId, option.stage(),
                    option.grant(), Set.of());
            if (result.changed()) advanced.add(step.npc());
        }
        return List.copyOf(advanced);
    }

    /**
     * Post-battle narrative handoff. The *official League* is the only authority
     * that awards a win: league_grass is written by its first-win callback.
     *
     * A dialogue alone cannot pass this gate. Old verified winners can recover
     * on login or /pridecore story sync. The expected stage and Mom->Thorn fact
     * ensure this cannot skip the campaign or roll a progressed player backwards.
     * No badges, currency, FTB task completion or legacy tags are written here.
     */
    public synchronized boolean reconcileVerdantVictory(UUID playerId) throws IOException {
        ProfileState current = progression.get(playerId);
        // Existing players may have met Rin after Thorn and had their current
        // stage overwritten by the optional rival introduction. The owned
        // Thorn checkpoint plus official League markers establish eligibility
        // to recover without fabricating any battle result or regressing players
        // already in Act I or later.
        if (!Set.of("pride:verdant_gym_intro", "pride:rival_introduced")
                .contains(current.storyStage()))
            return false;
        if (!current.facts().contains("pride:met_thorn"))
            return false;
        Set<String> legacy = legacyFor(playerId);
        if (!legacy.contains("league_started") || !legacy.contains("league_grass"))
            return false;
        return progression.transition(playerId, "pride:verdant_victory",
            Set.of(), Set.of()).changed();
    }

    public synchronized void cancel(UUID playerId) { sessions.remove(playerId); }

    private Reply reply(StoryDefinition.Npc npc, StoryDefinition.Node node,
                        ProfileState player, Set<String> verifiedLegacy) {
        List<ChoiceView> choices = node.options().stream()
                .filter(o -> player.facts().containsAll(o.requires()))
                .filter(o -> verifiedLegacy.containsAll(o.requiresLegacy()))
                .map(o -> new ChoiceView(o.id(), o.text())).toList();
        return new Reply(npc.id(), npc.name(), node.text(), choices, player.revision());
    }

    /**
     * Pure read-only completed story checkpoints; may be projected to hidden
     * Minecraft advancements for individual FTB Quests tasks.
     */
    public synchronized Set<String> completedNpcs(UUID playerId) throws IOException {
        var owned = progression.get(playerId).facts();
        Set<String> done = new TreeSet<>();
        for (var npc : story.npcs().values())
            if (owned.contains(npc.completedFact())) done.add(npc.id());
        return Set.copyOf(done);
    }

    /** Read-only stage lookup for next-objective hints; never advances progress. */
    public synchronized String currentStage(UUID playerId) throws IOException {
        return progression.get(playerId).storyStage();
    }

    public int npcCount() { return story.npcs().size(); }
}
