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
        Map.entry("pride:ice_research", "Investigate the concealed Meridian warning archive near Sera, then meet Rin at the summit. The road ahead feels different this time."),
        Map.entry("pride:rival_reunited", "The summit has changed everything. Earn Nyx's Phantom Badge, then find Medium Ori near the old Ghost sanctuary."),
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
        // The milestone stage describes the chapter; its clue and badge
        // requirements can still be outstanding. Explain the actual next
        // action rather than prematurely sending players to a locked NPC.
        Set<String> storyFacts = facts == null ? Set.of() : facts;
        Set<String> badges = legacy == null ? Set.of() : legacy;
        if ("pride:fire_district".equals(stage))
            return storyFacts.contains("pride:clue_foundry")
                ? "Bring the Foundry's unusual heat readings to Researcher Vela at Meridian Institute."
                : "Investigate the strange heat source inside Emberfall Foundry before visiting Researcher Vela.";
        if ("pride:institute_introduction".equals(stage))
            return badges.contains("league_water")
                ? "Keeper Neris is investigating a strange pulse along Azure Bay's waterways. Speak with her."
                : "Travel to Azure Bay, earn Kai's Tidal Badge, then speak with Keeper Neris beside the waterways.";
        if ("pride:tidal_investigation".equals(stage))
            return storyFacts.contains("pride:clue_river")
                ? "Bring the unusual waterway pulse evidence to Captain Rowan of the Pride Rangers."
                : "Investigate the unusual pulse beside Azure Bay's waterway before reporting to Captain Rowan.";
        if ("pride:ranger_investigation".equals(stage))
            return badges.contains("league_electric")
                ? "Meet Engineer Quill beneath Voltaris Power Station to examine the unauthorized power supply."
                : "Earn Vega's Voltage Badge in Voltaris City, then meet Engineer Quill at the power station.";
        if ("pride:powergrid_reveal".equals(stage))
            return storyFacts.contains("pride:clue_grid")
                ? "Bring the hidden circuit evidence to Aide Iris at Meridian Institute."
                : "Inspect the concealed electrical circuit inside Voltaris Power Station before visiting Aide Iris.";
        if ("pride:meridian_records".equals(stage))
            return badges.contains("league_dragon")
                ? "Meet Historian Toren near Drakespire's ancient Dragon shrine to investigate the seals."
                : "Earn Draven's Draco Badge in Drakespire City, then speak with Historian Toren.";
        if ("pride:dragon_lore".equals(stage)) {
            if (!storyFacts.contains("pride:clue_ward"))
                return "Investigate the ancient Resonance ward at Drakespire's central plaza Dragon statue.";
            return badges.contains("league_ice")
                ? "Bring news of the ancient ward to Archivist Sera in Frostveil's hidden archive."
                : "The ancient ward has been examined. Earn Freya's Glacier Badge, then speak with Archivist Sera.";
        }
        if ("pride:ice_research".equals(stage))
            return storyFacts.contains("pride:clue_ice")
                ? "The concealed Meridian warning has been discovered. Meet Rin at Pride Summit."
                : "Investigate the concealed Meridian warning archive in Frostveil, then meet Rin at Pride Summit.";
        if ("pride:rival_reunited".equals(stage))
            return badges.contains("league_ghost")
                ? "Find Medium Ori near the old sanctuary in Ebonreach City and uncover its memory echoes."
                : "Travel to Ebonreach City, earn Nyx's Phantom Badge, then find Medium Ori near the sanctuary.";
        if ("pride:ghost_memories".equals(stage)) {
            if (!storyFacts.contains("pride:clue_echo"))
                return "Investigate the ancient memory echo near Ebonreach's abandoned sanctuary before seeking the Eclipse Lieutenant.";
            return storyFacts.contains("pride:eclipse_lieutenant_won")
                ? "The Eclipse Lieutenant's Pokémon have yielded. Speak to the Lieutenant to learn Sable's plan."
                : "The memory echo confirms a deeper danger. Confront the Eclipse Lieutenant in a Pokémon battle.";
        }
        if ("pride:eclipse_infiltration".equals(stage))
            return storyFacts.contains("pride:sable_battle_won")
                ? "Sable has lost the battle. Speak with her to learn why Eclipse rose against Meridian."
                : "Seek Sable at the Eclipse stronghold and confront her in a Pokémon battle.";
        if ("pride:sable_confrontation".equals(stage))
            return storyFacts.contains("pride:cael_battle_won")
                ? "Speak with Director Cael about the Engine after your Pokémon battle."
                : "Bring Sable's warning to Director Cael at Meridian Institute and confront him.";
        if ("pride:engine_confrontation".equals(stage))
            return storyFacts.contains("pride:clue_engine")
                ? "Bring the Engine's diagnostics to Captain Rowan for the final Ranger briefing."
                : "Investigate the Resonance Engine's central diagnostics before returning to Captain Rowan.";
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
