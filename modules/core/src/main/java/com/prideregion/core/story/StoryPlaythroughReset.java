package com.prideregion.core.story;

import com.prideregion.core.*;
import java.io.IOException;
import java.util.*;

/**
 * Operator-only selective reset of facts owned by the Module 02 campaign.
 * Never deletes profiles or alters external Minecraft tags, League badges,
 * player inventory, currency, FTB Quests, or LegendaryQuests.
 */
public final class StoryPlaythroughReset {
    public record Preview(String currentStage, int campaignFacts, Set<String> preserved) {}
    private final ProgressionService progression;
    private final StoryDefinition definition;

    public StoryPlaythroughReset(ProgressionService progression, StoryDefinition definition) {
        this.progression = Objects.requireNonNull(progression);
        this.definition = Objects.requireNonNull(definition);
    }

    public Set<String> ownedFacts() {
        Set<String> owned = new TreeSet<>();
        for (StoryDefinition.Npc npc : definition.npcs().values()) {
            owned.add(npc.completedFact());
            for (StoryDefinition.Node node : npc.nodes().values())
                for (StoryDefinition.Option choice : node.options()) {
                    owned.addAll(choice.grant());
                    owned.addAll(choice.revoke());
                }
        }
        for (StoryEvidenceEngine.Clue clue : StoryEvidenceEngine.definitions())
            owned.add(clue.earnedFact());
        for (StoryBattleProof.Encounter encounter : StoryBattleProof.definitions())
            owned.add(encounter.earnedFact());
        // Verified physical world and final battle milestones are deliberately
        // Module 02-owned even when the custom NPC graph is incomplete.
        owned.addAll(Set.of("pride:resonance_engine_stabilized",
            "pride:crisis_resolved", "pride:finale_completed"));
        return Set.copyOf(owned);
    }

    public Preview preview(UUID player) throws IOException {
        ProfileState profile = progression.get(player);
        Set<String> owned = ownedFacts();
        Set<String> preserved = new TreeSet<>(profile.facts());
        preserved.removeAll(owned);
        int count = profile.facts().size() - preserved.size();
        return new Preview(profile.storyStage(), count, Set.copyOf(preserved));
    }

    /** Atomic profile transition, keeps monotonic revision and unknown module facts. */
    public ProgressionService.Change reset(UUID player) throws IOException {
        ProfileState before = progression.get(player);
        Set<String> revoke = new HashSet<>(before.facts());
        revoke.retainAll(ownedFacts());
        return progression.transition(player, "pride:unstarted", Set.of(), revoke);
    }
}
