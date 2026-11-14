package ru.saradmin.currency.data;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/** Fetches direct quotes for each requested currency pair. */
public final class RateProvider {
    public static final class Rate { public final BigDecimal value; public final int source; Rate(BigDecimal v,int s){value=v;source=s;} }
    public static final class Result { public final Map<String,Rate> rates; public final boolean anyApiSucceeded; Result(Map<String,Rate> r,boolean ok){rates=r;anyApiSucceeded=ok;} }

    public static Map<String,Rate> merge(Map<String,BigDecimal> first, Map<String,BigDecimal> second) {
        Map<String,Rate> merged=new LinkedHashMap<>();
        if(first!=null)for(Map.Entry<String,BigDecimal> e:first.entrySet())if(e.getValue()!=null&&e.getValue().signum()>0)merged.put(e.getKey(),new Rate(e.getValue(),1));
        if(second!=null)for(Map.Entry<String,BigDecimal> e:second.entrySet())if(e.getValue()!=null&&e.getValue().signum()>0&&!merged.containsKey(e.getKey()))merged.put(e.getKey(),new Rate(e.getValue(),2));
        merged.put("USD",new Rate(BigDecimal.ONE,first!=null?1:2)); return merged;
    }
    public Result fetch() { return fetch(java.util.Arrays.asList("USD", "EUR", "GBP", "RUB", "KZT", "EGP")); }

    /** Fetch one requested pair only. Source 1 is a direct Frankfurter quote;
     * source 2 is a cross-rate derived from the USD-based ExchangeRate-API. */
    public Rate fetchPair(String base, String target) {
        if (base == null || target == null || base.equals(target)) return null;
        try {
            Map<String,BigDecimal> direct = parseFrankfurter(get("https://api.frankfurter.dev/v1/latest?base=" + base));
            BigDecimal value = direct.get(target);
            if (value != null && value.signum() > 0) return new Rate(value, 1);
        } catch (Exception ignored) { }
        try {
            Map<String,BigDecimal> usd = parseExchangeRate(get("https://open.er-api.com/v6/latest/USD"));
            BigDecimal baseUsd = "USD".equals(base) ? BigDecimal.ONE : usd.get(base);
            BigDecimal targetUsd = "USD".equals(target) ? BigDecimal.ONE : usd.get(target);
            if (baseUsd != null && targetUsd != null && baseUsd.signum() > 0 && targetUsd.signum() > 0)
                return new Rate(targetUsd.divide(baseUsd, new java.math.MathContext(16, java.math.RoundingMode.HALF_UP)), 2);
        } catch (Exception ignored) { }
        return null;
    }

    /** Fetch all quotes from one newly added currency to the existing currencies.
     * Frankfurter is requested once; ExchangeRate-API is queried once only for
     * targets not covered by Frankfurter. No cached or synthetic USD factors are used.
     */
    public Map<String,Rate> fetchPairsForCurrency(String base, java.util.List<String> targets) {
        Map<String,Rate> out = new LinkedHashMap<>();
        if (base == null || targets == null || targets.isEmpty()) return out;
        Map<String,BigDecimal> direct = null;
        try { direct = parseFrankfurter(get("https://api.frankfurter.dev/v1/latest?base=" + base)); }
        catch (Exception ignored) { }
        Map<String,BigDecimal> usd = null;
        boolean needsFallback = false;
        for (String target : targets) {
            if (target != null && !target.equals(base) && (direct == null || !direct.containsKey(target))) {
                needsFallback = true; break;
            }
        }
        if (needsFallback) {
            try { usd = parseExchangeRate(get("https://open.er-api.com/v6/latest/USD")); }
            catch (Exception ignored) { }
        }
        BigDecimal baseUsd = "USD".equals(base) ? BigDecimal.ONE : (usd == null ? null : usd.get(base));
        for (String target : targets) {
            if (target == null || target.equals(base)) continue;
            BigDecimal value = direct == null ? null : direct.get(target);
            int source = 1;
            if (value == null && usd != null && baseUsd != null && baseUsd.signum() > 0) {
                BigDecimal targetUsd = "USD".equals(target) ? BigDecimal.ONE : usd.get(target);
                if (targetUsd != null && targetUsd.signum() > 0) {
                    value = targetUsd.divide(baseUsd, new java.math.MathContext(16, java.math.RoundingMode.HALF_UP));
                    source = 2;
                }
            }
            if (value != null && value.signum() > 0) out.put(base + ">" + target, new Rate(value, source));
        }
        return out;
    }

    /** Fetches quotes for a newly added currency against the existing currency list.
     * One Frankfurter request is made with the new currency as base, followed by
     * at most one USD-based fallback request. Returned keys are NEW>OTHER only.
     */
    public Result fetchForCurrency(String newCurrency, java.util.List<String> existingCurrencies) {
        Map<String,Rate> pairs = new LinkedHashMap<>();
        Map<String,BigDecimal> direct = null;
        Map<String,BigDecimal> usdFallback = null;
        try { direct = parseFrankfurter(get("https://api.frankfurter.dev/v1/latest?base=" + newCurrency)); }
        catch (Exception ignored) { }
        try { usdFallback = parseExchangeRate(get("https://open.er-api.com/v6/latest/USD")); }
        catch (Exception ignored) { }
        for (String target : existingCurrencies) {
            if (newCurrency.equals(target)) continue;
            BigDecimal value = direct == null ? null : direct.get(target);
            int source = 1;
            if ((value == null || value.signum() <= 0) && usdFallback != null) {
                BigDecimal newUsd = "USD".equals(newCurrency) ? BigDecimal.ONE : usdFallback.get(newCurrency);
                BigDecimal targetUsd = "USD".equals(target) ? BigDecimal.ONE : usdFallback.get(target);
                if (newUsd != null && targetUsd != null && newUsd.signum() > 0 && targetUsd.signum() > 0) {
                    value = targetUsd.divide(newUsd, new java.math.MathContext(16, java.math.RoundingMode.HALF_UP));
                    source = 2;
                }
            }
            if (value != null && value.signum() > 0)
                pairs.put(newCurrency + ">" + target, new Rate(value, source));
        }
        return new Result(pairs, !pairs.isEmpty());
    }

    public Result fetch(java.util.List<String> currencies) {
        Map<String,Rate> pairs = new LinkedHashMap<>();
        boolean any = false;
        Map<String,BigDecimal> usdFallback = null;
        try { usdFallback = parseExchangeRate(get("https://open.er-api.com/v6/latest/USD")); any = true; }
        catch (Exception ignored) { }
        for (String base : currencies) {
            Map<String,BigDecimal> direct = null;
            try {
                direct = parseFrankfurter(get("https://api.frankfurter.dev/v1/latest?base=" + base));
                any = true;
            } catch (Exception ignored) { }
            for (String target : currencies) {
                if (base.equals(target)) continue;
                BigDecimal value = direct == null ? null : direct.get(target);
                int source = 1;
                if (value == null && usdFallback != null) {
                    BigDecimal baseUsd = "USD".equals(base) ? BigDecimal.ONE : usdFallback.get(base);
                    BigDecimal targetUsd = "USD".equals(target) ? BigDecimal.ONE : usdFallback.get(target);
                    if (baseUsd != null && targetUsd != null) {
                        value = targetUsd.divide(baseUsd, new java.math.MathContext(16, java.math.RoundingMode.HALF_UP));
                        source = 2;
                    }
                }
                if (value != null && value.signum() > 0)
                    pairs.put(base + ">" + target, new Rate(value, source));
            }
        }
        // Keep USD factors available for adding currencies in the existing UI.
        if (usdFallback != null) for (Map.Entry<String,BigDecimal> e : usdFallback.entrySet())
            pairs.putIfAbsent("USD>" + e.getKey(), new Rate(e.getValue(), 2));
        return new Result(pairs, any && !pairs.isEmpty());
    }
    static String get(String address) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection(); c.setConnectTimeout(9000); c.setReadTimeout(12000); c.setRequestMethod("GET"); c.setRequestProperty("Accept","application/json");
        try { int code=c.getResponseCode(); if(code<200||code>=300) throw new java.io.IOException("HTTP "+code); try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");} } finally { c.disconnect(); }
    }
    static Map<String,BigDecimal> parseFrankfurter(String json) throws Exception {
        JSONObject root=new JSONObject(json); if(root.optString("base").length()!=3) throw new java.io.IOException("Unexpected Frankfurter base"); JSONObject rates=root.getJSONObject("rates"); Map<String,BigDecimal> out=new LinkedHashMap<>(); java.util.Iterator<String> it=rates.keys(); while(it.hasNext()){String k=it.next(); BigDecimal v=new BigDecimal(rates.get(k).toString()); if(v.signum()>0)out.put(k,v);} return out;
    }
    static Map<String,BigDecimal> parseExchangeRate(String json) throws Exception {
        JSONObject root=new JSONObject(json); if(!"success".equals(root.optString("result")) || !"USD".equals(root.optString("base_code"))) throw new java.io.IOException("Unexpected ExchangeRate-API response"); JSONObject rates=root.getJSONObject("rates"); Map<String,BigDecimal> out=new LinkedHashMap<>(); java.util.Iterator<String> it=rates.keys(); while(it.hasNext()){String k=it.next(); BigDecimal v=new BigDecimal(rates.get(k).toString()); if(v.signum()>0)out.put(k,v);} return out;
    }
}
