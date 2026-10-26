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

    public synchronized void cancel(UUID playerId) { sessions.remove(playerId); }

    private Reply reply(StoryDefinition.Npc npc, StoryDefinition.Node node,
                        ProfileState player, Set<String> verifiedLegacy) {
        List<ChoiceView> choices = node.options().stream()
                .filter(o -> player.facts().containsAll(o.requires()))
                .filter(o -> verifiedLegacy.containsAll(o.requiresLegacy()))
                .map(o -> new ChoiceView(o.id(), o.text())).toList();
        return new Reply(npc.id(), npc.name(), node.text(), choices, player.revision());
    }

    public int npcCount() { return story.npcs().size(); }
}
