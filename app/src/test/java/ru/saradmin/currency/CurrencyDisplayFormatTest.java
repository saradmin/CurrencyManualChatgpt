package ru.saradmin.currency;

import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;

public class CurrencyDisplayFormatTest {
    @Test public void alwaysShowsTwoFractionalDigits() {
        assertEquals("1.00", CurrencyDisplayFormat.format(new BigDecimal("1")));
        assertEquals("1.20", CurrencyDisplayFormat.format(new BigDecimal("1.2")));
    }
    @Test public void roundsHalfUpToTwoDigits() {
        assertEquals("1.24", CurrencyDisplayFormat.format(new BigDecimal("1.235")));
        assertEquals("1,234.57", CurrencyDisplayFormat.format(new BigDecimal("1234.567")));
    }
}
