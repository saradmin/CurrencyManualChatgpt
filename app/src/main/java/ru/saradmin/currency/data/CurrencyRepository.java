package ru.saradmin.currency.data;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.saradmin.currency.model.CurrencyItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Local-only storage. Factors are relative to the first configured currency, not an external rate source. */
public final class CurrencyRepository {
    private static final String PREFS = "currency_manual_v2";
    private static final int MAX_CURRENCIES = 6;
    private final SharedPreferences prefs;
    private final LinkedHashMap<String, BigDecimal> factors = new LinkedHashMap<>();
    private String baseCode;

    public CurrencyRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        String raw = prefs.getString("currencies", null);
        if (raw != null) {
            try {
                JSONArray array = new JSONArray(raw);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    String code = o.getString("code");
                    BigDecimal factor = new BigDecimal(o.getString("factor"));
                    if (CurrencyCatalog.find(code) != null && factor.signum() > 0) factors.put(code, factor);
                }
                baseCode = prefs.getString("base", null);
            } catch (Exception ignored) {
                factors.clear();
            }
        }
        if (factors.isEmpty()) {
            // Starter values are examples only. They are manual editable values, not live rates.
            factors.put("USD", BigDecimal.ONE);
            factors.put("EUR", new BigDecimal("0.925"));
            factors.put("KZT", new BigDecimal("492"));
            factors.put("RUB", new BigDecimal("91.5"));
            baseCode = "USD";
            save();
        }
        if (!factors.containsKey(baseCode)) baseCode = factors.keySet().iterator().next();
    }

    public synchronized void save() {
        try {
            JSONArray array = new JSONArray();
            for (Map.Entry<String, BigDecimal> entry : factors.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("code", entry.getKey());
                o.put("factor", entry.getValue().toPlainString());
                array.put(o);
            }
            prefs.edit().putString("currencies", array.toString()).putString("base", baseCode).apply();
        } catch (Exception ignored) { }
    }

    public synchronized List<CurrencyItem> getCurrencies() {
        List<CurrencyItem> result = new ArrayList<>();
        for (String code : factors.keySet()) result.add(CurrencyCatalog.find(code));
        return result;
    }
    public synchronized String getBaseCode() { return baseCode; }
    public synchronized void setBaseCode(String code) {
        if (!factors.containsKey(code)) throw new IllegalArgumentException("Unknown currency");
        baseCode = code; save();
    }
    public synchronized BigDecimal getFactor(String code) { return factors.get(code); }
    public synchronized BigDecimal getRate(String code) {
        BigDecimal base = factors.get(baseCode), target = factors.get(code);
        if (base == null || target == null) return BigDecimal.ZERO;
        return ru.saradmin.currency.core.ConversionEngine.rate(base, target);
    }
    public synchronized void setRateFromBase(String targetCode, BigDecimal rate) {
        if (!factors.containsKey(targetCode) || targetCode.equals(baseCode)) throw new IllegalArgumentException("Invalid target currency");
        BigDecimal next = ru.saradmin.currency.core.ConversionEngine.factorFromRate(factors.get(baseCode), rate);
        factors.put(targetCode, next); save();
    }
    public synchronized boolean addCurrency(String code, BigDecimal rateFromBase) {
        if (factors.size() >= MAX_CURRENCIES || factors.containsKey(code) || CurrencyCatalog.find(code) == null || rateFromBase == null || rateFromBase.signum() <= 0) return false;
        factors.put(code, ru.saradmin.currency.core.ConversionEngine.factorFromRate(factors.get(baseCode), rateFromBase));
        save(); return true;
    }
    public synchronized int getMaxCurrencies() { return MAX_CURRENCIES; }
}
