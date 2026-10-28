package com.prideregion.core.story;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.io.IOException;

/**
 * Read-only mirror of completed Pride-owned story facts into ordinary
 * per-player vanilla advancements, usable by FTB Quests advancement tasks.
 *
 * It never completes an FTB Quest, awards rewards, or writes legacy tags.
 * Reentrant/idempotent: grantCriterion returns false if already completed.
 * On login, missing advancements are restored from Module 01 persisted facts.
 */
public final class StoryAdvancementBridge {
    private StoryAdvancementBridge() {}

    public static int synchronize(ServerPlayerEntity player, StoryEngine engine) throws IOException {
        MinecraftServer server = player.getServer();
        if (server == null) return 0;
        int granted = 0;
        var advancements = server.getAdvancementLoader();
        var tracker = player.getAdvancementTracker();
        for (String npcId : engine.completedNpcs(player.getUuid())) {
            var entry = advancements.get(Identifier.of("pride_story", npcId));
            if (entry != null && tracker.grantCriterion(entry, "done")) granted++;
        }
        // Investigation proofs are per-player, reward-free vanilla advancements.
        // Never mark them from dialogue or from a different player's discovery.
        for (String evidenceId : engine.completedEvidence(player.getUuid())) {
            var entry = advancements.get(Identifier.of("pride_story", "clue_" + evidenceId));
            if (entry != null && tracker.grantCriterion(entry, "done")) granted++;
        }
        return granted;
    }
}
