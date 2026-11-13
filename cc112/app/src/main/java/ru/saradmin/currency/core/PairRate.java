package ru.saradmin.currency.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** A manually maintained quote for one currency pair, independent of other pairs. */
public final class PairRate {
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private PairRate() { }

    public static BigDecimal orient(BigDecimal storedRate, boolean sameDirection) {
        if (storedRate == null || storedRate.signum() <= 0)
            throw new IllegalArgumentException("Pair rate must be greater than zero");
        return sameDirection ? storedRate : BigDecimal.ONE.divide(storedRate, MC);
    }
}
