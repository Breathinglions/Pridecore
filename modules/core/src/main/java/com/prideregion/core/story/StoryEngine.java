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
        var change = progression.transition(playerId, option.stage(),
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
                                String stage) {}
    private static final List<VerifiedStep> ONBOARDING = List.of(
        new VerifiedStep("sequoia", "understood", Set.of("starter_chosen", "sequoia_met"),
                "pride:trainer_registration"),
        new VerifiedStep("theo", "next", Set.of("trainer_card_received"),
                "pride:field_orientation"),
        new VerifiedStep("avery", "journey", Set.of("trainer_guide_received"),
                "pride:route1_orientation")
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

    public int npcCount() { return story.npcs().size(); }
}
