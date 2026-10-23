package com.prideregion.community;

public final class RewardNoticeTest {
    private static int total;
    private static void check(boolean pass, String note) {
        total++;
        if (!pass) throw new AssertionError(note);
    }
    private static void invalid(Runnable job) {
        try { job.run(); throw new AssertionError("accepted invalid formatting"); }
        catch (IllegalArgumentException expected) { total++; }
    }
    public static void main(String[] ignored) {
        String currency = RewardNotice.DEFAULT_CURRENCY_SYMBOL;
        check(currency.equals("\uE122"), "currency glyph codepoint");
        check(RewardNotice.money(150, currency).equals(currency + "150"), "regular amount");
        check(RewardNotice.money(2000, currency).equals(currency + "2,000"), "grouped amount");
        check(RewardNotice.invitation(150,30,false,currency).equals(
            "First correct answer wins " + currency + "150 · 30s [TEST: no payout]"), "test mode amount");
        check(RewardNotice.invitation(350,30,true,currency).equals(
            "First correct answer wins " + currency + "350 · 30s (daily cap applies)"), "active amount");
        check(RewardNotice.invitation(0,30,false,currency).contains("no cash prize"), "zero prize");
        check(RewardNotice.paidAmount(500, currency).equals(" +" + currency + "500"), "confirmed payment");
        check(RewardNotice.invitation(250,60,false,"₽").contains("₽250"), "config override ruble fallback");
        check(RewardNotice.winner("Sableye",false).equals(" solved Sableye! [TEST]"), "test winner");
        check(RewardNotice.winner("Sableye",true).equals(" solved Sableye!"), "paid winner");
        check(!RewardNotice.invitation(100,30,false,currency).contains("$"), "no US dollar sign");
        invalid(() -> RewardNotice.money(-1, currency));
        invalid(() -> RewardNotice.invitation(100,0,false,currency));
        invalid(() -> RewardNotice.validateCurrencySymbol(""));
        invalid(() -> RewardNotice.validateCurrencySymbol("\n"));
        invalid(() -> RewardNotice.validateCurrencySymbol("x".repeat(25)));
        invalid(() -> RewardNotice.paidAmount(0,currency));
        System.out.println("PASS " + total + " PokéDollar reward formatting checks");
    }
}
