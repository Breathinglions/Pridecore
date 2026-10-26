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
        store.grant(veteranWithTags,"pride:met_mom");
        check(reconcile.reconcileOnboarding(veteranWithTags)
                .equals(List.of("sequoia","theo","avery")),
            "Old player with Mom already completed can reconcile entire lab sequence");
        check(store.get(newbie).revision()==4,"Other player reconciliation isolated");
        System.out.println("PASS: "+assertions+" dialogue tests, guard rails, persistence and player isolation");
    }
}
