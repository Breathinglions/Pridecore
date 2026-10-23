package com.prideregion.community;
public final class RewardNoticeTest{
  private static int total=0;
  private static void check(boolean result){total++;if(!result)throw new AssertionError("check "+total);}
  public static void main(String[] args){
    check(RewardNotice.invitation(150,30,false).equals("First correct answer wins $150 · 30s [TEST: no payout]"));
    check(RewardNotice.invitation(350,30,true).equals("First correct answer wins $350 · 30s (daily cap applies)"));
    check(RewardNotice.invitation(0,30,false).contains("no cash prize"));
    check(RewardNotice.invitation(500,60,false).contains("TEST: no payout"));
    check(RewardNotice.winner("Sableye",false).equals(" solved Sableye! [TEST]"));
    check(RewardNotice.winner("Sableye",true).equals(" solved Sableye!"));
    try{RewardNotice.invitation(-1,30,false);throw new AssertionError("negative");}catch(IllegalArgumentException expected){total++;}
    try{RewardNotice.invitation(150,0,false);throw new AssertionError("invalid timer");}catch(IllegalArgumentException expected){total++;}
    System.out.println("PASS "+total+" reward announcement tests");
  }
}
