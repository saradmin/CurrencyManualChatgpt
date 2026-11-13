package ru.saradmin.currency.core;

import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

public class ConversionEngineTest {
    @Test public void convertsBetweenCurrenciesWithoutChangingAmount() {
        BigDecimal result=ConversionEngine.convert(new BigDecimal("100"),new BigDecimal("1"),new BigDecimal("0.925"));
        assertEquals(0,result.compareTo(new BigDecimal("92.5")));
    }
    @Test public void calculatesCrossRateFromAnyBase() {
        BigDecimal rate=ConversionEngine.rate(new BigDecimal("0.925"),new BigDecimal("492"));
        assertEquals(0,rate.compareTo(new BigDecimal("531.8918918918919")));
    }
    @Test public void manualRateIsNormalizedAgainstCurrentBase() {
        BigDecimal factor=ConversionEngine.factorFromRate(new BigDecimal("0.925"),new BigDecimal("100"));
        assertEquals(0,factor.compareTo(new BigDecimal("92.5")));
    }
    @Test public void rejectsNonPositiveFactors() {
        try { ConversionEngine.rate(BigDecimal.ZERO,BigDecimal.ONE); fail("Expected validation error"); }
        catch(IllegalArgumentException expected) { }
    }
}
