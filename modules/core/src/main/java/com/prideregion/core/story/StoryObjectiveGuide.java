package com.prideregion.core.story;

import java.util.Map;

/**
 * Read-only, player-facing directions for current story checkpoints.
 * Only names the next destination; does not grant quests, items, battle wins or tags.
 * Kept in a separate table so we can eventually move directions into story.json.
 */
public final class StoryObjectiveGuide {
    private StoryObjectiveGuide() {}

    private static final Map<String, String> NEXT = Map.ofEntries(
        Map.entry("pride:unstarted", "Speak with Mom at home in Hearthleaf to begin your journey."),
        Map.entry("pride:visit_sequoia", "Visit Professor Sequoia at the Hearthleaf Pokémon Lab. Choose a starter if you have not already."),
        Map.entry("pride:trainer_registration", "Register with Technician Theo at the Pokémon Lab to receive your Trainer Card and Poképedia."),
        Map.entry("pride:field_orientation", "Talk to Avery at the Pokémon Lab and collect your one-time field kit."),
        Map.entry("pride:route1_orientation", "Explore Route 1, catch a wild Pokémon and defeat Rin in the scripted rival battle. Then report to Ranger Mira."),
        Map.entry("pride:verdant_approach", "Head toward Verdant Gym. Speak with Thorn and prepare for the Grass-type challenge."),
        Map.entry("pride:verdant_gym_intro", "Defeat Thorn in the official Verdant Gym battle to earn the Verdant Badge. Then see Warden Elowen."),
        Map.entry("pride:verdant_victory", "Verdant Gym cleared! Speak with Warden Elowen in Verdant Forest and investigate the unusual Resonance disturbances."),
        Map.entry("pride:rival_introduced", "Defeat Rin in the Route 1 rival battle before reporting to Ranger Mira and progressing to Verdant Gym."),
        Map.entry("pride:forest_investigation", "Follow the forest disturbance with Warden Elowen. Then investigate the mine with Lyra after Rook's Gym."),
        Map.entry("pride:rock_investigation", "Ask Miner Lyra about the missing survey records and the suspected Eclipse trail."),
        Map.entry("pride:eclipse_detected", "Meet Rin at the canyon, discuss Team Eclipse, and defeat him in the scripted rematch before continuing."),
        Map.entry("pride:rival_debate", "Win Rin's canyon battle, then investigate the foundry district after Ashen's Fire Gym."),
        Map.entry("pride:fire_district", "Speak with Researcher Vela at Meridian Institute about the heat anomaly."),
        Map.entry("pride:institute_introduction", "Visit Keeper Neris and follow the waterway evidence after Kai's Water Gym."),
        Map.entry("pride:tidal_investigation", "Report the unusual waterway pulse to Captain Rowan of the Pride Rangers."),
        Map.entry("pride:ranger_investigation", "Talk to Engineer Quill about the hidden power circuit after Vega's Electric Gym."),
        Map.entry("pride:powergrid_reveal", "Meet Aide Iris and ask about Meridian's missing risk reports."),
        Map.entry("pride:meridian_records", "Consult Historian Toren about the ancient Resonance seals after Draven's Dragon Gym."),
        Map.entry("pride:dragon_lore", "Find Archivist Sera and review the frozen records after Freya's Ice Gym."),
        Map.entry("pride:ice_research", "Meet Rin at the summit and defeat him in the scripted rival battle before investigating the Ghost memories."),
        Map.entry("pride:rival_reunited", "Defeat Rin at the summit, then speak with Medium Ori after Nyx's Ghost Gym."),
        Map.entry("pride:ghost_memories", "Find the Eclipse Lieutenant and win the scripted battle before confronting the Eclipse operative."),
        Map.entry("pride:eclipse_infiltration", "Defeat the Eclipse Lieutenant, confront Sable, and win her scripted boss battle."),
        Map.entry("pride:sable_confrontation", "Defeat Sable, confront Director Cael at Meridian Institute and win his scripted boss battle."),
        Map.entry("pride:engine_confrontation", "Defeat Director Cael, examine the engine diagnostics and report to Captain Rowan."),
        Map.entry("pride:crisis_briefed", "Activate all four engine conduits, defeat the three shield defenders and stabilize the Resonance core before visiting the League Guide."),
        Map.entry("pride:victory_road_briefed", "Complete the official Pokémon League and defeat Champion Magnus, then visit the Hall of Fame Keeper."),
        Map.entry("pride:champion_epilogue", "Your Championship chapter is complete. Check the Quest Book for postgame and Legendary Expeditions.")
    );

    public static String forStage(String stage) {
        return NEXT.getOrDefault(stage,
            "Open the FTB Quest Book and check your current main-story quest.");
    }

    public static boolean hasStage(String stage) {
        return NEXT.containsKey(stage);
    }

    public static int coveredStages() {
        return NEXT.size();
    }
}
