package com.prideregion.community;
public final class RewardNotice {
    private RewardNotice(){}
    public static String invitation(int amount,int seconds,boolean payoutsActive){
        if(amount<0||seconds<1||seconds>600) throw new IllegalArgumentException("Invalid reward");
        String time=" · "+seconds+"s";
        if(amount==0) return "First correct answer wins the round"+time+" (no cash prize)";
        return "First correct answer wins $"+amount+time+
            (payoutsActive?" (daily cap applies)":" [TEST: no payout]");
    }
    public static String winner(String answer,boolean payoutsActive){
        if(answer==null||answer.isBlank())throw new IllegalArgumentException("Missing answer");
        return " solved "+answer+"!"+(payoutsActive?"":" [TEST]");
    }
}
