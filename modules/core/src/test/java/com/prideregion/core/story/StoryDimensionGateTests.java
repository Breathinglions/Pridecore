package com.prideregion.core.story;

public final class StoryDimensionGateTests {
    private static int checks;
    private static void ok(boolean value) {
        checks++;
        if (!value) throw new AssertionError("Story dimension gate test " + checks);
    }
    private static void reject(String input) {
        boolean denied = false;
        try { StoryDimensionGate.parse(input); }
        catch (IllegalArgumentException expected) { denied = true; }
        ok(denied);
    }
    public static void main(String[] args) {
        var open = StoryDimensionGate.open();
        ok(!open.restricted());
        ok(open.permits("minecraft:overworld"));
        ok(open.permits("minecraft:the_nether"));
        ok(StoryDimensionGate.parse(" ").permits("modded:staging"));
        var restricted = StoryDimensionGate.parse(
            " minecraft:overworld , multiverse:pride_region_test ");
        ok(restricted.restricted());
        ok(restricted.allowed().size() == 2);
        ok(restricted.permits("multiverse:pride_region_test"));
        ok(restricted.permits("minecraft:overworld"));
        ok(!restricted.permits("minecraft:the_nether"));
        ok(!restricted.permits(null));
        reject("minecraft:overworld,minecraft:overworld");
        reject("minecraft:overworld,");
        reject("minecraft:Overworld");
        reject("overworld");
        reject("../world");
        reject("*");
        reject("example:" + "bad/".repeat(0) + "Space");
        reject("n:a,n:b,n:c,n:d,n:e,n:f,n:g,n:h,n:i,n:j,n:k,n:l,n:m,n:n,n:o,n:p,n:q");
        System.out.println("PASS: " + checks + " world-isolated story gate checks");
    }
}
