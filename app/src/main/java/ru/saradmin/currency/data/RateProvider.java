package ru.saradmin.currency.data;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/** Fetches USD-normalized rates. Provider 1 is preferred; provider 2 fills gaps or acts as fallback. */
public final class RateProvider {
    public static final class Rate { public final BigDecimal value; public final int source; Rate(BigDecimal v,int s){value=v;source=s;} }
    public static final class Result { public final Map<String,Rate> rates; public final boolean anyApiSucceeded; Result(Map<String,Rate> r,boolean ok){rates=r;anyApiSucceeded=ok;} }

    public static Map<String,Rate> merge(Map<String,BigDecimal> first, Map<String,BigDecimal> second) {
        Map<String,Rate> merged=new LinkedHashMap<>();
        if(first!=null)for(Map.Entry<String,BigDecimal> e:first.entrySet())if(e.getValue()!=null&&e.getValue().signum()>0)merged.put(e.getKey(),new Rate(e.getValue(),1));
        if(second!=null)for(Map.Entry<String,BigDecimal> e:second.entrySet())if(e.getValue()!=null&&e.getValue().signum()>0&&!merged.containsKey(e.getKey()))merged.put(e.getKey(),new Rate(e.getValue(),2));
        merged.put("USD",new Rate(BigDecimal.ONE,first!=null?1:2)); return merged;
    }
    public Result fetch() {
        Map<String,BigDecimal> firstRates=null, secondRates=null; boolean ok=false;
        try { firstRates=parseFrankfurter(get("https://api.frankfurter.dev/v1/latest?base=USD")); ok=true; } catch(Exception ignored) { }
        try { secondRates=parseExchangeRate(get("https://open.er-api.com/v6/latest/USD")); ok=true; } catch(Exception ignored) { }
        return new Result(merge(firstRates,secondRates),ok);
    }
    static String get(String address) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection(); c.setConnectTimeout(9000); c.setReadTimeout(12000); c.setRequestMethod("GET"); c.setRequestProperty("Accept","application/json");
        try { int code=c.getResponseCode(); if(code<200||code>=300) throw new java.io.IOException("HTTP "+code); try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");} } finally { c.disconnect(); }
    }
    static Map<String,BigDecimal> parseFrankfurter(String json) throws Exception {
        JSONObject root=new JSONObject(json); if(!"USD".equals(root.optString("base"))) throw new java.io.IOException("Unexpected Frankfurter base"); JSONObject rates=root.getJSONObject("rates"); Map<String,BigDecimal> out=new LinkedHashMap<>(); java.util.Iterator<String> it=rates.keys(); while(it.hasNext()){String k=it.next(); BigDecimal v=new BigDecimal(rates.get(k).toString()); if(v.signum()>0)out.put(k,v);} return out;
    }
    static Map<String,BigDecimal> parseExchangeRate(String json) throws Exception {
        JSONObject root=new JSONObject(json); if(!"success".equals(root.optString("result")) || !"USD".equals(root.optString("base_code"))) throw new java.io.IOException("Unexpected ExchangeRate-API response"); JSONObject rates=root.getJSONObject("rates"); Map<String,BigDecimal> out=new LinkedHashMap<>(); java.util.Iterator<String> it=rates.keys(); while(it.hasNext()){String k=it.next(); BigDecimal v=new BigDecimal(rates.get(k).toString()); if(v.signum()>0)out.put(k,v);} return out;
    }
}
