package ru.saradmin.currency;

import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;

public class CurrencyDisplayFormatTest {
    @Test public void alwaysShowsTwoFractionalDigits() {
        assertEquals("1.00", CurrencyDisplayFormat.format(new BigDecimal("1")));
        assertEquals("1.20", CurrencyDisplayFormat.format(new BigDecimal("1.2")));
    }
    @Test public void ratesAlwaysShowFourDecimalsAndDoNotRoundPositiveTinyRatesToZero() {
        assertEquals("1.0000", CurrencyDisplayFormat.formatRate(new BigDecimal("1")));
        assertEquals("1.2346", CurrencyDisplayFormat.formatRate(new BigDecimal("1.23456")));
        assertEquals("0.0001", CurrencyDisplayFormat.formatRate(new BigDecimal("0.000011")));
        assertEquals("0.0001", CurrencyDisplayFormat.formatRate(new BigDecimal("0.000049")));
        assertEquals("0.0001", CurrencyDisplayFormat.formatRate(new BigDecimal("0.0001")));
        assertEquals("12 345.6789", CurrencyDisplayFormat.formatRate(new BigDecimal("12345.67891")));
    }
    @Test public void groupsEditableAndTypedNumbersWithoutRounding() {
        assertEquals("1 234 567.890123", CurrencyDisplayFormat.formatEditable(new BigDecimal("1234567.890123")));
        assertEquals("1 234 567.", CurrencyDisplayFormat.formatInput("1234567."));
    }
    @Test public void roundsHalfUpToTwoDigitsForAmounts() {
        assertEquals("1.24", CurrencyDisplayFormat.format(new BigDecimal("1.235")));
        assertEquals("1 234.57", CurrencyDisplayFormat.format(new BigDecimal("1234.567")));
        assertEquals("1 234 567.89", CurrencyDisplayFormat.format(new BigDecimal("1234567.89")));
    }
}
