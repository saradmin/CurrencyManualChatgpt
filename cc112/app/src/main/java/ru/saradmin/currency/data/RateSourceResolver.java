package ru.saradmin.currency.data;

/** Resolves the provider badge for a quote derived from USD-normalized API rates. */
public final class RateSourceResolver {
    private RateSourceResolver() { }

    public static int resolve(String base, String target, int baseSource, int targetSource, Integer pairSource) {
        if (pairSource != null) return Math.max(0, pairSource);
        if ("USD".equals(base)) return Math.max(0, targetSource);
        if ("USD".equals(target)) return Math.max(0, baseSource);
        return Math.max(0, targetSource);
    }
}
