package ru.saradmin.currency.core;

import org.junit.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConversionEngineTest {

    @Test
    public void convertsBetweenCurrenciesWithoutChangingAmount() {
        BigDecimal usdFactor = new BigDecimal("1");
        BigDecimal eurFactor = new BigDecimal("0.925");
        BigDecimal kztFactor = new BigDecimal("492");
        BigDecimal amount = new BigDecimal("100");

        BigDecimal usdToEur = ConversionEngine.convert(
                amount, usdFactor, eurFactor);

        assertEquals(0, new BigDecimal("92.5").compareTo(usdToEur));

        BigDecimal eurToKzt = ConversionEngine.convert(
                amount, eurFactor, kztFactor);

        assertEquals(
                0,
                eurToKzt.setScale(8, RoundingMode.HALF_UP)
                        .compareTo(new BigDecimal("53189.18918919"))
        );
    }

    @Test
    public void changingBaseDoesNotAlterNumericAmount() {
        BigDecimal amount = new BigDecimal("125.50");
        BigDecimal before = amount;

        ConversionEngine.convert(
                amount,
                new BigDecimal("1"),
                new BigDecimal("0.925")
        );

        assertEquals(0, before.compareTo(amount));
    }

    @Test
    public void rejectsZeroRate() {
        try {
            ConversionEngine.factorFromRate(
                    BigDecimal.ONE,
                    BigDecimal.ZERO
            );
            fail("Expected validation error");
        } catch (IllegalArgumentException expected) {
            // Expected result
        }
    }
}