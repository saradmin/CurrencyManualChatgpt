package ru.saradmin.currency;

/** Short labels displayed inside the rate-source badges. */
public final class RateSourceLabel {
    private RateSourceLabel() {}
    public static String forSource(int source) {
        if (source == 1) return "1";
        if (source == 2) return "2";
        return "";
    }
}
