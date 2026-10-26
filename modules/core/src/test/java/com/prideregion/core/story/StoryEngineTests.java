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
        System.out.println("PASS: "+assertions+" dialogue tests, guard rails, persistence and player isolation");
    }
}
