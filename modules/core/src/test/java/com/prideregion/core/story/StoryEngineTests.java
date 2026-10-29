package com.prideregion.core.story;

import com.prideregion.core.*;
import java.nio.file.Files;
import java.util.*;

public final class StoryEngineTests {
    static int assertions;
    static void check(boolean ok, String msg) {
        assertions++;
        if (!ok) throw new AssertionError(msg);
    }
    interface Throwing { void execute() throws Exception; }
    static void rejected(Throwing action) throws Exception {
        assertions++;
        try { action.execute(); throw new AssertionError("Expected denied dialogue"); }
        catch (IllegalArgumentException | IllegalStateException expected) { /* correct */ }
    }
    public static void main(String[] args) throws Exception {
        var mom = new StoryDefinition.Npc("mom","Mom",Set.of(),"pride:met_mom",
            "first","again",Map.of(
                "first",new StoryDefinition.Node("first","Go see the Professor.",
                    List.of(new StoryDefinition.Option("yes","I'll go.",null,"pride:professor_visit",
                        Set.of("pride:met_mom"),Set.of(),Set.of()))),
                "again",new StoryDefinition.Node("again","Be safe!",List.of())
            ));
        var professor = new StoryDefinition.Npc("sequoia","Professor Sequoia",Set.of("pride:met_mom"),
            "pride:talked_to_sequoia","first","again",Map.of(
                "first",new StoryDefinition.Node("first","Select your starter using the existing system.",
                    List.of(new StoryDefinition.Option("heard","Understood.",null,"pride:registration_intro",
                        Set.of("pride:talked_to_sequoia"),Set.of(),Set.of()))),
                "again",new StoryDefinition.Node("again","Keep researching!",List.of())
            ));
        var d = new StoryDefinition(Map.of("mom",mom,"sequoia",professor));
        var path = Files.createTempDirectory("storytests");
        var store = new ProgressionService(new ProfileStore(path));
        var engine = new StoryEngine(d,store);
        UUID p = UUID.randomUUID(), other = UUID.randomUUID();
        rejected(() -> engine.begin(p,"sequoia"));
        rejected(() -> engine.choose(p,"yes"));
        check(engine.begin(p,"mom").text().contains("Professor"),"Mom starts journey");
        rejected(() -> engine.choose(p,"unknown"));
        check(store.get(p).revision()==0,"Invalid option no writes");
        var done = engine.choose(p,"yes");
        check(done.options().isEmpty() && done.revision()==1,"Single transaction applied");
        check(store.get(p).facts().contains("pride:met_mom"),"Story fact stored");
        check(store.get(p).storyStage().equals("pride:professor_visit"),"Stage stored");
        check(engine.currentStage(p).equals("pride:professor_visit"),
            "Current stage is read-only and never changes the profile");
        check(StoryObjectiveGuide.forStage("pride:unstarted").contains("Mom"),
            "Fresh trainer is directed to Mom");
        check(StoryObjectiveGuide.forStage("pride:visit_sequoia").contains("Professor Sequoia"),
            "Mom completion tells player the next destination");
        check(StoryObjectiveGuide.forStage("pride:trainer_registration").contains("Technician Theo"),
            "Trainer registration gives a useful next destination");
        check(StoryObjectiveGuide.forStage("pride:route1_orientation").contains("Rin"),
            "Route 1 objective requires the scripted rival battle");
        check(StoryObjectiveGuide.forStage("pride:eclipse_detected").contains("canyon"),
            "Canyon rivalry is identified in the player's campaign directions");
        check(StoryObjectiveGuide.forStage("pride:ice_research").contains("summit"),
            "Summit rivalry is identified in the player's campaign directions");
        check(StoryObjectiveGuide.forStage("pride:ghost_memories").contains("Lieutenant"),
            "Eclipse Lieutenant battle is a required stage");
        check(StoryObjectiveGuide.forStage("pride:crisis_briefed").contains("shield"),
            "The Resonance Engine phase remains a mandatory objective");
        check(StoryObjectiveGuide.coveredStages() == 29,
            "All 27 milestone stages and the empty profile have directions");
        check(StoryObjectiveGuide.forStage("pride:unmapped").contains("Quest Book"),
            "Unknown custom stages fail safely with generic quest-book guidance");
        check(engine.completedNpcs(p).contains("mom"),
            "Completed NPC checkpoint can be projected to the player's advancement");
        check(engine.completedNpcs(other).isEmpty(),
            "One player's completed checkpoints never leak to another");
        check(engine.begin(p,"mom").text().equals("Be safe!"),"Repeat dialogue");
        rejected(() -> engine.choose(p,"yes"));
        check(engine.begin(p,"sequoia").text().contains("starter"),"Next character unlocked");
        check(engine.choose(p,"heard").revision()==2,"Second NPC progression stored");
        check(store.get(other).revision()==0,"Other player untouched");
        check(engine.begin(other,"mom").options().size()==1,"Other player begins independently");
        engine.cancel(other);
        rejected(() -> engine.choose(other,"yes"));
        check(new ProgressionService(new ProfileStore(path)).get(p).revision()==2,
            "Progression survives service restart");
        rejected(() -> new StoryDefinition.Npc("bad-name", "X",Set.of(),
            "pride:ok","first","first",Map.of("first",
            new StoryDefinition.Node("first","text",List.of()))));
        rejected(() -> new StoryDefinition.Npc("bad","X",Set.of(),
            "pride:ok","first","first",Map.of("first",
            new StoryDefinition.Node("first","text",List.of(new StoryDefinition.Option(
                "x","Choice","nonexistent",null,Set.of(),Set.of(),Set.of()))))));
        // Existing starter/registration milestones are read only from each player's tags.
        UUID fresh = UUID.randomUUID(), registered = UUID.randomUUID();
        Map<UUID, Set<String>> tagsByPlayer = new HashMap<>();
        tagsByPlayer.put(fresh, Set.of());
        tagsByPlayer.put(registered, Set.of("starter_chosen", "trainer_card_received"));
        var gated = new StoryDefinition.Npc("gate", "Gate", Set.of(),
            "pride:gate_passed", "first", "again", Map.of(
                "first", new StoryDefinition.Node("first", "Check onboarding", List.of(
                    new StoryDefinition.Option("proceed", "Continue", null, "pride:next",
                        Set.of("pride:gate_passed"), Set.of(), Set.of(),
                        Set.of("starter_chosen", "trainer_card_received")))),
                "again", new StoryDefinition.Node("again", "Welcome back", List.of())));
        var gatedEngine = new StoryEngine(new StoryDefinition(Map.of("gate", gated)),
            store, id -> tagsByPlayer.getOrDefault(id, Set.of()));
        check(gatedEngine.begin(fresh, "gate").options().isEmpty(),
            "Unregistered player cannot see gated option");
        rejected(() -> gatedEngine.choose(fresh, "proceed"));
        check(store.get(fresh).revision() == 0, "Denied action must not save a profile");
        check(gatedEngine.begin(registered, "gate").options().size() == 1,
            "Registered player can see verified option");
        check(gatedEngine.choose(registered, "proceed").revision() == 1,
            "Registered player progresses exactly one revision");
        check(!store.get(registered).facts().contains("pride:trainer_card_received"),
            "External tags must never be written as owned facts");
        rejected(() -> new StoryDefinition.Option("bad", "invalid", null, null,
            Set.of(), Set.of(), Set.of(), Set.of("admin")));
        check(LegacyOnboardingGate.filter(Set.of("admin", "starter_chosen"))
            .equals(Set.of("starter_chosen")), "Only allowlisted legacy markers visible");
        check(gatedEngine.begin(fresh, "gate").options().isEmpty(),
            "One player's eligibility never leaks into another player");
        // Recheck at choice time: prerequisites can change after opening a dialogue.
        UUID third = UUID.randomUUID();
        tagsByPlayer.put(third, Set.of("starter_chosen", "trainer_card_received"));
        check(gatedEngine.begin(third,"gate").options().size() == 1,
            "Choice visible while tags are valid");
        tagsByPlayer.put(third, Set.of("starter_chosen"));
        rejected(() -> gatedEngine.choose(third,"proceed"));
        check(store.get(third).revision()==0, "Revoked prerequisite blocks late submission");
        check(NativeNpcIdentity.tagFor("sequoia").equals("pride_npc_professor_sequoia"),
            "Existing Sequoia NPC tag mapped");
        check(NativeNpcIdentity.tagFor("avery").equals("pride_npc_avery"),
            "Existing Avery NPC tag mapped");
        check(NativeNpcIdentity.tagFor("mom").equals("pride_npc_mom"),
            "New native NPC tag is predictable");
        check(NativeNpcIdentity.tagFor("rin_canyon").equals("pride_npc_rin")
                && NativeNpcIdentity.tagFor("rin_summit").equals("pride_npc_rin"),
            "Rin's three story scenes authenticate against one canonical actor");
        check(NativeNpcIdentity.tagFor("ranger_epilogue")
                .equals("pride_npc_ranger_rowan"),
            "Captain Rowan's recurring scenes share one actor");
        rejected(() -> NativeNpcIdentity.tagFor("../escape"));
        // Normal Lab services should promote story automatically, without an
        // additional "Continue my Pride Region story" dialogue button.
        var autoMom = new StoryDefinition.Npc("mom","Mom",Set.of(),"pride:met_mom",
            "hello","again",Map.of(
                "hello",new StoryDefinition.Node("hello","Welcome",List.of(
                    new StoryDefinition.Option("begin","Start",null,"pride:visit_sequoia",
                        Set.of("pride:met_mom"),Set.of(),Set.of()))),
                "again",new StoryDefinition.Node("again","Hi again",List.of())));
        var autoSequoia = new StoryDefinition.Npc("sequoia","Sequoia",Set.of("pride:met_mom"),
            "pride:met_sequoia","hello","again",Map.of(
                "hello",new StoryDefinition.Node("hello","Welcome",List.of(
                    new StoryDefinition.Option("understood","Done",null,
                        "pride:trainer_registration",Set.of("pride:met_sequoia"),
                        Set.of(),Set.of(),Set.of("starter_chosen")))),
                "again",new StoryDefinition.Node("again","Welcome back",List.of())));
        var autoTheo = new StoryDefinition.Npc("theo","Theo",Set.of("pride:met_sequoia"),
            "pride:met_theo","hello","again",Map.of(
                "hello",new StoryDefinition.Node("hello","Register",List.of(
                    new StoryDefinition.Option("next","Done",null,"pride:field_orientation",
                        Set.of("pride:met_theo"),Set.of(),Set.of(),
                        Set.of("trainer_card_received")))),
                "again",new StoryDefinition.Node("again","Hello",List.of())));
        var autoAvery = new StoryDefinition.Npc("avery","Avery",Set.of("pride:met_theo"),
            "pride:met_avery","hello","again",Map.of(
                "hello",new StoryDefinition.Node("hello","Take your kit",List.of(
                    new StoryDefinition.Option("journey","Done",null,"pride:route1_orientation",
                        Set.of("pride:met_avery"),Set.of(),Set.of(),
                        Set.of("trainer_guide_received")))),
                "again",new StoryDefinition.Node("again","Be careful",List.of())));
        var autoDefinitions = new StoryDefinition(Map.of(
            "mom",autoMom,"sequoia",autoSequoia,"theo",autoTheo,"avery",autoAvery));
        Map<UUID,Set<String>> actualMarkers = new HashMap<>();
        UUID newbie = UUID.randomUUID(), veteranWithTags = UUID.randomUUID();
        var reconcile = new StoryEngine(autoDefinitions,store,
            id -> actualMarkers.getOrDefault(id,Set.of()));

        actualMarkers.put(newbie,Set.of("starter_chosen","trainer_card_received",
            "trainer_guide_received"));
        check(reconcile.reconcileOnboarding(newbie).isEmpty(),
            "Existing tags cannot bypass Mom's required introduction");
        check(store.get(newbie).revision()==0,"No premature profile write");
        check(reconcile.begin(newbie,"mom").options().size()==1,"Mom still offers start");
        reconcile.choose(newbie,"begin");
        check(reconcile.reconcileOnboarding(newbie).isEmpty(),
            "Sequoia encounter required in addition to starter tag");
        actualMarkers.put(newbie,Set.of("starter_chosen","sequoia_met"));
        check(reconcile.reconcileOnboarding(newbie).equals(List.of("sequoia")),
            "Professor promotion automatically recognizes original starter service");
        check(store.get(newbie).storyStage().equals("pride:trainer_registration"),
            "Automatic Professor stage matches established story");
        check(reconcile.reconcileOnboarding(newbie).isEmpty(),
            "Repeat Professor sync cannot increment revisions");
        actualMarkers.put(newbie,Set.of("starter_chosen","sequoia_met",
            "trainer_card_received"));
        check(reconcile.reconcileOnboarding(newbie).equals(List.of("theo")),
            "Trainer registration automatically promotes Theo checkpoint");
        actualMarkers.put(newbie,Set.of("starter_chosen","sequoia_met",
            "trainer_card_received","trainer_guide_received"));
        check(reconcile.reconcileOnboarding(newbie).equals(List.of("avery")),
            "Avery kit automatically promotes field orientation");
        check(store.get(newbie).storyStage().equals("pride:route1_orientation"),
            "All verified onboarding steps reached Route 1");
        check(store.get(newbie).revision()==4,"Exactly four owned writes including Mom");
        check(reconcile.reconcileOnboarding(newbie).isEmpty(),
            "Idempotent startup reconciliation");
        check(store.get(newbie).revision()==4,"No duplicate rewards or state writes");
        check(!store.get(newbie).facts().contains("pride:trainer_guide_received"),
            "Legacy tags are never imported into Pride-owned facts");
        actualMarkers.put(veteranWithTags,actualMarkers.get(newbie));
        check(reconcile.reconcileOnboarding(veteranWithTags).isEmpty(),
            "Veteran legacy tags do not magically begin the story");
        store.transition(veteranWithTags,"pride:visit_sequoia",
            Set.of("pride:met_mom"),Set.of());
        check(reconcile.reconcileOnboarding(veteranWithTags)
                .equals(List.of("sequoia","theo","avery")),
            "Old player with Mom already completed can reconcile entire lab sequence");
        check(store.get(newbie).revision()==4,"Other player reconciliation isolated");
        UUID inconsistent = UUID.randomUUID();
        store.transition(inconsistent,"pride:regional_finale",
            Set.of("pride:met_mom"),Set.of());
        actualMarkers.put(inconsistent,Set.of("sequoia_met","starter_chosen",
            "trainer_card_received","trainer_guide_received"));
        check(reconcile.reconcileOnboarding(inconsistent).isEmpty(),
            "Legacy reconciliation never downgrades an advanced story stage");
        check(store.get(inconsistent).storyStage().equals("pride:regional_finale"),
            "Advanced story checkpoint must be preserved");

        // An NPC conversation alone NEVER grants Thorn's win.
        // The existing League's verified league_grass marker is the sole
        // battle-win authority; replay and unrelated players are isolated.
        UUID firstGym = UUID.randomUUID(), noIntro = UUID.randomUUID();
        store.transition(firstGym, "pride:verdant_gym_intro",
            Set.of("pride:met_thorn"), Set.of());
        store.transition(noIntro, "pride:verdant_gym_intro",
            Set.of("pride:met_mom"), Set.of());
        var gymEngine = new StoryEngine(autoDefinitions, store,
            id -> actualMarkers.getOrDefault(id, Set.of()));
        check(!gymEngine.reconcileVerdantVictory(firstGym),
            "Talking to Thorn cannot falsely award a League win");
        actualMarkers.put(firstGym, Set.of("league_started"));
        check(!gymEngine.reconcileVerdantVictory(firstGym),
            "League start without a battle victory must not progress");
        actualMarkers.put(firstGym, Set.of("league_started", "league_grass"));
        check(gymEngine.reconcileVerdantVictory(firstGym),
            "Verified RCT Thorn win advances narrative toward Warden Elowen");
        check(store.get(firstGym).storyStage().equals("pride:verdant_victory"),
            "Win transitions to the Warden Elowen followup");
        long wonRevision = store.get(firstGym).revision();
        check(!gymEngine.reconcileVerdantVictory(firstGym)
                && store.get(firstGym).revision() == wonRevision,
            "Repeated battle sync must not write twice");
        check(!store.get(firstGym).facts().contains("pride:league_grass"),
            "The League marker is never copied into Pride Core-owned facts");
        actualMarkers.put(noIntro, Set.of("league_started", "league_grass"));
        // With only ONE original RCT Thorn actor, Route 1 + his genuine
        // League victory now completes the narrative Thorn checkpoint.
        UUID oneThorn = UUID.randomUUID();
        store.transition(oneThorn, "pride:verdant_approach",
            Set.of("pride:route1_briefed"), Set.of());
        actualMarkers.put(oneThorn, Set.of("league_started"));
        check(!gymEngine.reconcileVerdantVictory(oneThorn),
            "Talking to original RCT Thorn without winning cannot clear Gym");
        actualMarkers.put(oneThorn, Set.of("league_started", "league_grass"));
        check(gymEngine.reconcileVerdantVictory(oneThorn),
            "Original RCT win directly advances narrative without second Story Thorn");
        check(store.get(oneThorn).facts().contains("pride:met_thorn"),
            "Original RCT battle completion grants one story Thorn fact");
        check(store.get(oneThorn).storyStage().equals("pride:verdant_victory"),
            "Original one-Thorn encounter moves to Warden objective");
        long oneRevision = store.get(oneThorn).revision();
        check(!gymEngine.reconcileVerdantVictory(oneThorn)
                && store.get(oneThorn).revision() == oneRevision,
            "Official one-Thorn story handoff cannot be paid or written twice");
        check(!gymEngine.reconcileVerdantVictory(noIntro),
            "Gym win cannot bypass uncompleted Thorn story intro");
        check(store.get(noIntro).storyStage().equals("pride:verdant_gym_intro"),
            "No-intro player stage remains unchanged");
        UUID veteranGym = UUID.randomUUID();
        store.transition(veteranGym, "pride:regional_finale",
            Set.of("pride:met_thorn"), Set.of());
        actualMarkers.put(veteranGym, Set.of("league_started", "league_grass"));
        check(!gymEngine.reconcileVerdantVictory(veteranGym),
            "Verified legacy Gym win can never regress later campaign stages");
        check(StoryObjectiveGuide.forStage("pride:verdant_victory")
                .contains("Warden Elowen"),
            "Genuine Verdant win gives a concrete quest-book-compatible followup");
        check(StoryObjectiveGuide.coveredStages() == 29,
            "Every milestone stage and newly verified Verdant win has guidance");

        // Regression: Rin may be visited out of sequence without undoing a
        // real Gym victory, and existing profiles stuck at rival_introduced
        // must reconcile ONLY when the original League actually awarded Grass.
        UUID stuckRival = UUID.randomUUID();
        store.transition(stuckRival, "pride:rival_introduced",
            Set.of("pride:met_thorn", "pride:met_avery", "pride:met_rin"), Set.of());
        actualMarkers.put(stuckRival, Set.of("league_started", "league_grass"));
        check(gymEngine.reconcileVerdantVictory(stuckRival),
            "Verified Gym win recovers the historical Rin stage overwrite");
        check(store.get(stuckRival).storyStage().equals("pride:verdant_victory"),
            "Stuck rival stage advances to Warden rather than replaying the Gym");
        long recoveredRevision = store.get(stuckRival).revision();
        check(!gymEngine.reconcileVerdantVictory(stuckRival)
                && store.get(stuckRival).revision() == recoveredRevision,
            "Rin recovery is idempotent, with no repeat writes");

        UUID fakeRivalWin = UUID.randomUUID();
        store.transition(fakeRivalWin, "pride:rival_introduced",
            Set.of("pride:met_thorn"), Set.of());
        actualMarkers.put(fakeRivalWin, Set.of("league_started"));
        check(!gymEngine.reconcileVerdantVictory(fakeRivalWin),
            "Rin cannot imply an official Gym victory without league_grass");
        UUID noRivalThorn = UUID.randomUUID();
        store.transition(noRivalThorn, "pride:rival_introduced",
            Set.of("pride:met_rin"), Set.of());
        actualMarkers.put(noRivalThorn, Set.of("league_started", "league_grass"));
        check(!gymEngine.reconcileVerdantVictory(noRivalThorn),
            "Verified League tags cannot skip the Thorn story checkpoint");

        var rin = new StoryDefinition.Npc("rin", "Rin",
            Set.of("pride:met_avery"), "pride:met_rin",
            "hello", "again", Map.of(
                "hello", new StoryDefinition.Node("hello", "Rival encounter", List.of(
                    new StoryDefinition.Option("continue", "Continue", null,
                        "pride:rival_introduced", Set.of("pride:met_rin"),
                        Set.of(), Set.of()))),
                "again", new StoryDefinition.Node("again", "Hi", List.of())));
        var rinEngine = new StoryEngine(new StoryDefinition(Map.of("rin", rin)), store);
        UUID afterGym = UUID.randomUUID();
        store.transition(afterGym, "pride:verdant_victory",
            Set.of("pride:met_avery", "pride:met_thorn"), Set.of());
        rinEngine.begin(afterGym, "rin");
        rinEngine.choose(afterGym, "continue");
        check(store.get(afterGym).storyStage().equals("pride:verdant_victory"),
            "Late Rin dialogue does not roll verified Gym progress backwards");
        check(store.get(afterGym).facts().contains("pride:met_rin"),
            "Late Rin encounter still completes its optional story fact");
        UUID earlyRin = UUID.randomUUID();
        store.transition(earlyRin, "pride:verdant_approach",
            Set.of("pride:met_avery"), Set.of());
        rinEngine.begin(earlyRin, "rin");
        rinEngine.choose(earlyRin, "continue");
        check(store.get(earlyRin).storyStage().equals("pride:verdant_approach"),
            "Rin never overwrites mainline destination even before Verdant Gym");

        // Mandatory rival: Sequoia's verified starter selection establishes
        // Rin at the Lab without requiring a second NPC or altering stage.
        var labWithRin = new StoryEngine(new StoryDefinition(Map.of(
            "mom",autoMom,"sequoia",autoSequoia,"rin",rin)), store,
            id -> actualMarkers.getOrDefault(id, Set.of()));
        UUID newRival = UUID.randomUUID();
        labWithRin.begin(newRival, "mom");
        labWithRin.choose(newRival, "begin");
        actualMarkers.put(newRival, Set.of("sequoia_met"));
        check(labWithRin.reconcileOnboarding(newRival).isEmpty(),
            "Speaking to Sequoia without selecting a starter cannot introduce Rin");
        actualMarkers.put(newRival, Set.of("sequoia_met", "starter_chosen"));
        check(labWithRin.reconcileOnboarding(newRival).equals(List.of("sequoia", "rin")),
            "Actual starter choice completes Sequoia and mandatory Rin milestone");
        check(store.get(newRival).facts().contains("pride:met_rin"),
            "Rival becomes an owned story checkpoint automatically");
        check(store.get(newRival).storyStage().equals("pride:trainer_registration"),
            "Introducing Rin never hides Theo's next objective");
        long rivalRevision = store.get(newRival).revision();
        check(labWithRin.reconcileOnboarding(newRival).isEmpty()
                && store.get(newRival).revision() == rivalRevision,
            "Mandatory rival starter sync remains one-time");
        check(!store.get(newbie).facts().contains("pride:met_rin"),
            "Starter rival checkpoint does not appear in unrelated player profiles");
        UUID oldStarter = UUID.randomUUID();
        store.transition(oldStarter, "pride:field_orientation",
            Set.of("pride:met_mom", "pride:met_sequoia"), Set.of());
        actualMarkers.put(oldStarter, Set.of("sequoia_met", "starter_chosen"));
        check(labWithRin.reconcileOnboarding(oldStarter).equals(List.of("rin")),
            "Returning established trainer safely receives missing rival checkpoint");
        check(store.get(oldStarter).storyStage().equals("pride:field_orientation"),
            "Retroactive Rin reconciliation keeps advanced trainer destination");
        UUID noSequoia = UUID.randomUUID();
        store.transition(noSequoia, "pride:trainer_registration",
            Set.of("pride:met_mom"), Set.of());
        actualMarkers.put(noSequoia, Set.of("sequoia_met", "starter_chosen"));
        check(labWithRin.reconcileOnboarding(noSequoia).isEmpty(),
            "Legacy starter tags cannot skip the actual Sequoia story gate");

        // Dialogue-only completion cannot bypass scripted Act IV boss battles.
        var sableNpc = new StoryDefinition.Npc("sable", "Sable",
            Set.of("pride:eclipse_assault"), "pride:sable_warning",
            "main", "again", Map.of(
                "main", new StoryDefinition.Node("main", "Face me in battle.",
                    List.of(new StoryDefinition.Option("continue", "Continue", null,
                        "pride:sable_confrontation", Set.of("pride:sable_warning"),
                        Set.of(), Set.of()))),
                "again", new StoryDefinition.Node("again", "We have settled this.", List.of())));
        var sableStory = new StoryEngine(new StoryDefinition(Map.of("sable", sableNpc)), store);
        UUID bossPlayer = UUID.randomUUID(), bystander = UUID.randomUUID();
        store.grant(bossPlayer, "pride:eclipse_assault");
        store.grant(bystander, "pride:eclipse_assault");
        sableStory.begin(bossPlayer, "sable");
        rejected(() -> sableStory.choose(bossPlayer, "continue"));
        check(!store.get(bossPlayer).facts().contains("pride:sable_warning"),
            "Dialogue cannot substitute for authenticated Sable victory");
        store.grant(bossPlayer, "pride:sable_battle_won");
        check(sableStory.choose(bossPlayer, "continue").options().isEmpty(),
            "Verified Sable victory unlocks the canonical story checkpoint");
        check(!store.get(bystander).facts().contains("pride:sable_warning"),
            "Sable battle story completion remains player-isolated");

        System.out.println("PASS: "+assertions+" dialogue tests, guard rails, persistence and player isolation");
    }
}
