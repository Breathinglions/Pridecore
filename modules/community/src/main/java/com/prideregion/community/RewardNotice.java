package com.prideregion.community;

import java.util.Locale;

/** Central presentation formatter. This never changes amounts deposited by Impactor. */
public final class RewardNotice {
    /** Pixel-art PokéDollar symbol supplied by the Pride Identity Polymer resource pack. */
    public static final String DEFAULT_CURRENCY_SYMBOL = "\uE122";
    private RewardNotice() {}

    public static String validateCurrencySymbol(String symbol) {
        if (symbol == null || symbol.isBlank() || symbol.length() > 24 ||
            symbol.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid currency symbol");
        }
        return symbol;
    }

    /** Displays the configured PokéDollar symbol, never the US dollar sign. */
    public static String money(int amount, String symbol) {
        if (amount < 0) throw new IllegalArgumentException("Negative reward");
        validateCurrencySymbol(symbol);
        return symbol + String.format(Locale.US, "%,d", amount);
    }

    public static String invitation(int amount, int seconds, boolean payoutsActive, String symbol) {
        if (seconds < 1 || seconds > 600) throw new IllegalArgumentException("Invalid round duration");
        String time = " · " + seconds + "s";
        if (amount == 0) return "First correct answer wins the round" + time + " (no cash prize)";
        return "First correct answer wins " + money(amount, symbol) + time
            + (payoutsActive ? " (daily cap applies)" : " [TEST: no payout]");
    }

    public static String winner(String answer, boolean payoutsActive) {
        if (answer == null || answer.isBlank()) throw new IllegalArgumentException("Missing answer");
        return " solved " + answer + "!" + (payoutsActive ? "" : " [TEST]");
    }

    public static String paidAmount(int amount, String symbol) {
        if (amount <= 0) throw new IllegalArgumentException("Invalid deposit confirmation");
        return " +" + money(amount, symbol);
    }
}
