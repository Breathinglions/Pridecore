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
        Map.entry("pride:route1_orientation", "Explore Route 1. Catch your first wild Pokémon and win your first battle, then report to Ranger Mira."),
        Map.entry("pride:verdant_approach", "Head toward Verdant Gym. Speak with Thorn and prepare for the Grass-type challenge."),
        Map.entry("pride:verdant_gym_intro", "Defeat Thorn in the official Verdant Gym battle to earn the Verdant Badge. Then see Warden Elowen."),
        Map.entry("pride:rival_introduced", "Continue toward Verdant Gym. Rin may challenge you along the way."),
        Map.entry("pride:forest_investigation", "Follow the forest disturbance with Warden Elowen. Then investigate the mine with Lyra after Rook's Gym."),
        Map.entry("pride:rock_investigation", "Ask Miner Lyra about the missing survey records and the suspected Eclipse trail."),
        Map.entry("pride:eclipse_detected", "Find Rin near the canyon and compare the evidence against Team Eclipse."),
        Map.entry("pride:rival_debate", "Investigate the foundry district after challenging Ashen's Fire Gym."),
        Map.entry("pride:fire_district", "Speak with Researcher Vela at Meridian Institute about the heat anomaly."),
        Map.entry("pride:institute_introduction", "Visit Keeper Neris and follow the waterway evidence after Kai's Water Gym."),
        Map.entry("pride:tidal_investigation", "Report the unusual waterway pulse to Captain Rowan of the Pride Rangers."),
        Map.entry("pride:ranger_investigation", "Talk to Engineer Quill about the hidden power circuit after Vega's Electric Gym."),
        Map.entry("pride:powergrid_reveal", "Meet Aide Iris and ask about Meridian's missing risk reports."),
        Map.entry("pride:meridian_records", "Consult Historian Toren about the ancient Resonance seals after Draven's Dragon Gym."),
        Map.entry("pride:dragon_lore", "Find Archivist Sera and review the frozen records after Freya's Ice Gym."),
        Map.entry("pride:ice_research", "Meet Rin at the summit and compare what you discovered."),
        Map.entry("pride:rival_reunited", "Speak with Medium Ori after Nyx's Ghost Gym to investigate the lingering memories."),
        Map.entry("pride:ghost_memories", "Investigate the Eclipse operation and speak with the Eclipse Lieutenant."),
        Map.entry("pride:eclipse_infiltration", "Confront Sable about Team Eclipse's actions."),
        Map.entry("pride:sable_confrontation", "Confront Director Cael at Meridian Institute about the Resonance Engine."),
        Map.entry("pride:engine_confrontation", "Report to Captain Rowan for the Resonance stabilization mission briefing."),
        Map.entry("pride:crisis_briefed", "Review your Pokémon League progress. Report to the League Guide about Victory Road."),
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
