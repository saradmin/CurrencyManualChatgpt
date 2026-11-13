package ru.saradmin.currency.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Converts through a private normalization factor. No market or external reference rate is used. */
public final class ConversionEngine {
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private ConversionEngine() { }

    /** factor is a manually maintained relative scale; rate is units of target per one base. */
    public static BigDecimal rate(BigDecimal baseFactor, BigDecimal targetFactor) {
        if (baseFactor == null || targetFactor == null || baseFactor.signum() <= 0 || targetFactor.signum() <= 0) {
            throw new IllegalArgumentException("Currency factors must be positive");
        }
        return targetFactor.divide(baseFactor, MC);
    }

    public static BigDecimal convert(BigDecimal amount, BigDecimal baseFactor, BigDecimal targetFactor) {
        if (amount == null) return BigDecimal.ZERO;
        return amount.multiply(rate(baseFactor, targetFactor), MC);
    }

    /** When user enters 1 base = rate target, update target's normalized factor. */
    public static BigDecimal factorFromRate(BigDecimal baseFactor, BigDecimal rate) {
        if (baseFactor == null || rate == null || baseFactor.signum() <= 0 || rate.signum() <= 0) {
            throw new IllegalArgumentException("Rate must be greater than zero");
        }
        return baseFactor.multiply(rate, MC);
    }
}
