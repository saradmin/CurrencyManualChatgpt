package ru.saradmin.currency.data;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RateSourceResolverTest {
    @Test public void usdToEgpUsesEgpProvider() {
        assertEquals(2, RateSourceResolver.resolve("USD", "EGP", 1, 2, null));
    }

    @Test public void egpToUsdUsesSameEgpProvider() {
        assertEquals(2, RateSourceResolver.resolve("EGP", "USD", 2, 1, null));
    }

    @Test public void manualPairSourceOverridesCurrencySourcesInBothDirections() {
        assertEquals(0, RateSourceResolver.resolve("USD", "EGP", 1, 2, 0));
        assertEquals(0, RateSourceResolver.resolve("EGP", "USD", 2, 1, 0));
    }
}
