package ru.saradmin.currency;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RateSourceLabelTest {
    @Test public void firstApiUsesNumberOne() { assertEquals("1", RateSourceLabel.forSource(1)); }
    @Test public void secondApiUsesNumberTwo() { assertEquals("2", RateSourceLabel.forSource(2)); }
    @Test public void manualOrFallbackHasNoNumber() { assertEquals("", RateSourceLabel.forSource(0)); }
}
