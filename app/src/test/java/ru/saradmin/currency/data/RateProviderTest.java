package ru.saradmin.currency.data;

import org.junit.Test;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class RateProviderTest {
    @Test public void firstProviderWinsAndSecondFillsMissingCurrencies() {
        Map<String,BigDecimal> a=new LinkedHashMap<>(),b=new LinkedHashMap<>();
        a.put("EUR",new BigDecimal("0.92")); a.put("GBP",new BigDecimal("0.80"));
        b.put("EUR",new BigDecimal("0.93")); b.put("EGP",new BigDecimal("49"));
        Map<String,RateProvider.Rate> merged=RateProvider.merge(a,b);
        assertEquals(0,merged.get("EUR").value.compareTo(new BigDecimal("0.92")));
        assertEquals(1,merged.get("EUR").source);
        assertEquals(2,merged.get("EGP").source);
        assertEquals(0,merged.get("USD").value.compareTo(BigDecimal.ONE));
    }
    @Test public void secondProviderWorksWhenFirstIsUnavailable() {
        Map<String,BigDecimal> b=new LinkedHashMap<>(); b.put("KZT",new BigDecimal("500"));
        Map<String,RateProvider.Rate> merged=RateProvider.merge(null,b);
        assertEquals(2,merged.get("KZT").source);
        assertEquals(2,merged.get("USD").source);
    }
    @Test public void ignoresInvalidRates() {
        Map<String,BigDecimal> a=new LinkedHashMap<>(); a.put("BAD",BigDecimal.ZERO);
        assertFalse(RateProvider.merge(a,null).containsKey("BAD"));
    }
}
