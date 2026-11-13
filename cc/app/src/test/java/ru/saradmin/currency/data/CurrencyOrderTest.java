package ru.saradmin.currency.data;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class CurrencyOrderTest {
    @Test public void movesCurrencyDownAndPreservesOtherItems() {
        List<String> result = CurrencyOrder.move(Arrays.asList("EUR", "KZT", "RUB", "EGP"), 0, 2);
        assertEquals(Arrays.asList("KZT", "RUB", "EUR", "EGP"), result);
    }
    @Test public void movesCurrencyUp() {
        List<String> result = CurrencyOrder.move(Arrays.asList("EUR", "KZT", "RUB"), 2, 0);
        assertEquals(Arrays.asList("RUB", "EUR", "KZT"), result);
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsInvalidPosition() {
        CurrencyOrder.move(Arrays.asList("EUR", "KZT"), 0, 2);
    }
}
