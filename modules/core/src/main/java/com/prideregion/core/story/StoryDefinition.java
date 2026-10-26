package com.prideregion.core.story;

import com.prideregion.core.ProfileState;
import java.util.*;
import java.util.regex.Pattern;

/** Validated, immutable dialogue graph. No command execution or external payouts. */
public record StoryDefinition(Map<String, Npc> npcs) {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    public StoryDefinition {
        Objects.requireNonNull(npcs);
        if (npcs.isEmpty() || npcs.size() > 64) throw new IllegalArgumentException("NPC count invalid");
        npcs = Map.copyOf(npcs);
        for (var entry : npcs.entrySet()) {
            if (!validId(entry.getKey()) || !entry.getKey().equals(entry.getValue().id()))
                throw new IllegalArgumentException("NPC key does not match id");
        }
    }
    public static boolean validId(String id) { return id != null && ID.matcher(id).matches(); }
    public static boolean owned(String id) { return id != null && ProfileState.validKey(id); }

    public record Npc(String id, String name, Set<String> requires,
                      String completedFact, String start, String repeat, Map<String, Node> nodes) {
        public Npc {
            if (!validId(id) || name == null || name.isBlank() || name.length() > 64 ||
                    requires == null || requires.size() > 16 || requires.stream().anyMatch(x -> !owned(x)) ||
                    !owned(completedFact) || !validId(start) || !validId(repeat) ||
                    nodes == null || nodes.isEmpty() || nodes.size() > 32)
                throw new IllegalArgumentException("Invalid NPC definition " + id);
            requires = Set.copyOf(requires);
            nodes = Map.copyOf(nodes);
            if (!nodes.containsKey(start) || !nodes.containsKey(repeat))
                throw new IllegalArgumentException("Missing entry/repeat node for " + id);
            for (var entry : nodes.entrySet()) {
                if (!entry.getKey().equals(entry.getValue().id()))
                    throw new IllegalArgumentException("Node key mismatch in " + id);
                for (Option option : entry.getValue().options()) {
                    if (option.next() != null && !nodes.containsKey(option.next()))
                        throw new IllegalArgumentException("Missing linked node " + option.next());
                }
            }
        }
    }
    public record Node(String id, String text, List<Option> options) {
        public Node {
            if (!validId(id) || text == null || text.isBlank() || text.length() > 480
                    || options == null || options.size() > 8)
                throw new IllegalArgumentException("Invalid dialogue node " + id);
            options = List.copyOf(options);
            Set<String> used = new HashSet<>();
            for (Option option : options)
                if (!used.add(option.id())) throw new IllegalArgumentException("Duplicate option " + option.id());
        }
    }
    public record Option(String id, String text, String next, String stage,
                         Set<String> grant, Set<String> revoke, Set<String> requires) {
        public Option {
            if (!validId(id) || text == null || text.isBlank() || text.length() > 160 ||
                    (next != null && !validId(next)) ||
                    (stage != null && !owned(stage)) || grant == null || revoke == null ||
                    requires == null || grant.size() > 16 || revoke.size() > 16 || requires.size() > 16)
                throw new IllegalArgumentException("Invalid option " + id);
            if (grant.stream().anyMatch(x -> !owned(x)) ||
                    revoke.stream().anyMatch(x -> !owned(x)) ||
                    requires.stream().anyMatch(x -> !owned(x)) ||
                    !Collections.disjoint(grant, revoke))
                throw new IllegalArgumentException("Invalid option facts " + id);
            grant = Set.copyOf(grant);
            revoke = Set.copyOf(revoke);
            requires = Set.copyOf(requires);
        }
    }
}
