package com.prideregion.core.story;

import java.util.Map;
import java.util.Set;

/**
 * Read-only, player-facing directions for current story checkpoints.
 * Only names the next destination; does not grant quests, items, battle wins or tags.
 * Kept in a separate table so we can eventually move directions into story.json.
 */
public final class StoryObjectiveGuide {
    private StoryObjectiveGuide() {}

    private static final Map<String, String> NEXT = Map.ofEntries(
        Map.entry("pride:unstarted", "Mom is waiting at home in Hearthleaf. Today feels like the beginning of something important."),
        Map.entry("pride:visit_sequoia", "Professor Sequoia has news for you at the Hearthleaf laboratory."),
        Map.entry("pride:trainer_registration", "Technician Theo has prepared your Trainer Card and Poképedia at the laboratory."),
        Map.entry("pride:field_orientation", "Avery is waiting at the laboratory with a few things for the road."),
        Map.entry("pride:route1_orientation", "Rin is somewhere along Route 1. Catch your first wild Pokémon and challenge Rin, then report to Ranger Mira about the eerie glow near the trees."),
        Map.entry("pride:verdant_approach", "Beyond Route 1, Thorn awaits at Verdant Gym. Something is stirring in the nearby forest."),
        Map.entry("pride:verdant_gym_intro", "Thorn is waiting at Verdant Gym. Warden Elowen needs help beyond the forest gates."),
        Map.entry("pride:verdant_victory", "Warden Elowen has discovered strange fragments in Verdant Forest."),
        Map.entry("pride:rival_introduced", "Rin has been looking for you on Route 1. Ranger Mira waits farther along the trail."),
        Map.entry("pride:forest_investigation", "Investigate the glowing amethyst fragments in Verdant Forest. Right-click the crystals to collect a Resonance trace."),
        Map.entry("pride:forest_traces_discovered", "Resonance trace collected! Follow the trail to Stonehaven City. Earn Rook's Bastion Badge, then seek Miner Lyra near the mountain."),
        Map.entry("pride:stonehaven_victory", "Rook's Bastion Badge is yours. Miner Lyra is investigating missing survey records near the Stonehaven mountain mine."),
        Map.entry("pride:rock_investigation", "Miner Lyra's missing survey records may reveal the truth. Examine the marked evidence in Stonehaven's Rock Mine."),
        Map.entry("pride:mine_evidence_discovered", "The missing survey page bears Eclipse markings. Find the Eclipse Scout and learn why they were near Stonehaven's old mine."),
        Map.entry("pride:eclipse_detected", "Rin has followed the same rumors to the canyon. Neither of you has the whole story yet."),
        Map.entry("pride:rival_debate", "The canyon meeting has raised more questions. Travel to Emberfall, earn Ashen's Inferno Badge, and investigate the foundry."),
        Map.entry("pride:emberfall_victory", "Ashen's Inferno Badge is yours. Meet Technician Sol at the Emberfall Foundry to investigate the unexplained heat."),
        Map.entry("pride:fire_district", "Researcher Vela at Meridian Institute has heard troubling reports from the foundry."),
        Map.entry("pride:institute_introduction", "Keeper Neris has found an unusual signal in the waterways near Kai's city."),
        Map.entry("pride:tidal_investigation", "Captain Rowan of the Pride Rangers is waiting for news from the river."),
        Map.entry("pride:ranger_investigation", "Engineer Quill is investigating a hidden circuit beneath Vega's power station."),
        Map.entry("pride:powergrid_reveal", "Aide Iris knows more than she can say about Meridian's missing risk reports."),
        Map.entry("pride:meridian_records", "Historian Toren has uncovered an old oath concerning the Resonance seals."),
        Map.entry("pride:dragon_lore", "Archivist Sera may know why the earliest warning was hidden beneath the ice."),
        Map.entry("pride:ice_research", "Rin is waiting at the summit. The road ahead feels different this time."),
        Map.entry("pride:rival_reunited", "The summit has changed everything. Medium Ori has heard echoes near Nyx's old sanctuary."),
        Map.entry("pride:ghost_memories", "An Eclipse Lieutenant stands between you and the truth about the operation."),
        Map.entry("pride:eclipse_infiltration", "Sable is waiting beyond the Eclipse outpost. She knows why Meridian hid the reports."),
        Map.entry("pride:sable_confrontation", "Sable's account points toward Director Cael at Meridian Institute."),
        Map.entry("pride:engine_confrontation", "Director Cael has seen the evidence. Captain Rowan is gathering the Rangers for what comes next."),
        Map.entry("pride:crisis_briefed", "The Resonance Engine is unstable. Four conduits and three ancient seals surround its restless core."),
        Map.entry("pride:victory_road_briefed", "Victory Road leads toward Champion Magnus and the final chapter of your League journey."),
        Map.entry("pride:champion_epilogue", "Your name is in the Hall of Fame. The region still holds stories no one has told.")
    );

    /** Display the correct next task when a battle fact changes but the stage
     * intentionally stays on the same checkpoint until the next NPC speaks.
     * No progression, quests, items or battle results are granted here.
     */
    public static String forStage(String stage, Set<String> facts) {
        return forStage(stage, facts, Set.of());
    }

    /**
     * Story milestones and existing verified capture markers drive directions.
     * This is read-only and never completes an FTB or external achievement.
     */
    public static String forStage(String stage, Set<String> facts, Set<String> legacy) {
        if ("pride:route1_orientation".equals(stage)) {
            boolean beatRin = facts != null && facts.contains("pride:rin_route1_won");
            boolean caught = legacy != null && legacy.contains("first_catch");
            if (beatRin && !caught)
                return "You've won your first Pokémon battle against Rin! "
                    + "Catch your first wild Pokémon, then report to Ranger Mira along Route 1.";
            if (beatRin)
                return "First Pokémon caught and Rin defeated! "
                    + "Find Ranger Mira along Route 1 to investigate the eerie glow near the trees.";
            if (caught)
                return "Your first Pokémon is caught! Challenge Rin along Route 1, "
                    + "then report to Ranger Mira.";
        }
        return forStage(stage);
    }

    public static String forStage(String stage) {
        return NEXT.getOrDefault(stage,
            "Your journey continues. Someone nearby may know what comes next.");
    }

    public static boolean hasStage(String stage) {
        return NEXT.containsKey(stage);
    }

    public static int coveredStages() {
        return NEXT.size();
    }
}
