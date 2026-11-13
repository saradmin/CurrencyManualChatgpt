package ru.saradmin.currency.core;

import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;

public class PairRateTest {
    @Test public void oneToOnePairRemainsOneToOneInBothDirections() {
        BigDecimal rate = new BigDecimal("1");
        assertEquals(0, PairRate.orient(rate, true).compareTo(new BigDecimal("1")));
        assertEquals(0, PairRate.orient(rate, false).compareTo(new BigDecimal("1")));
    }

    @Test public void zeroRateRemainsZeroInBothDirectionsAndIsNotInverted() {
        assertEquals(0, PairRate.orient(BigDecimal.ZERO, true).compareTo(BigDecimal.ZERO));
        assertEquals(0, PairRate.orient(BigDecimal.ZERO, false).compareTo(BigDecimal.ZERO));
    }

    @Test public void reverseDirectionUsesReciprocal() {
        BigDecimal rate = new BigDecimal("53.45");
        assertEquals(0, PairRate.orient(rate, true).compareTo(rate));
        assertEquals(0, PairRate.orient(rate, false).compareTo(new BigDecimal("1").divide(rate, new java.math.MathContext(16, java.math.RoundingMode.HALF_UP))));
    }
}
