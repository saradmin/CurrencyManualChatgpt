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

/** Persistent currency factors normalized to USD internally; USD is never imposed as the UI base. */
public final class CurrencyRepository {
    private static final String PREFS="currency_manual_v3"; private static final int MAX_CURRENCIES=6;
    private final SharedPreferences prefs; private final LinkedHashMap<String,BigDecimal> factors=new LinkedHashMap<>(); private final LinkedHashMap<String,Integer> sources=new LinkedHashMap<>(); private final LinkedHashMap<String,BigDecimal> apiCache=new LinkedHashMap<>(); private final LinkedHashMap<String,Integer> apiSources=new LinkedHashMap<>();
    private String baseCode; private long lastUpdate; private long intervalMinutes=15; private boolean staleFallback;
    public CurrencyRepository(Context context){prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);load();}
    private void load(){String raw=prefs.getString("currencies",null); if(raw!=null)try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String c=o.getString("code");BigDecimal f=new BigDecimal(o.getString("factor"));if(CurrencyCatalog.find(c)!=null&&f.signum()>0){factors.put(c,f);sources.put(c,o.optInt("source",0));}}baseCode=prefs.getString("base",null);}catch(Exception e){factors.clear();sources.clear();}
        if(factors.isEmpty()){factors.put("USD",BigDecimal.ONE);factors.put("EUR",new BigDecimal("0.925"));factors.put("KZT",new BigDecimal("492"));factors.put("RUB",new BigDecimal("91.5"));for(String c:factors.keySet())sources.put(c,-1);baseCode="USD";}
        if(!factors.containsKey(baseCode))baseCode=factors.keySet().iterator().next();lastUpdate=prefs.getLong("lastUpdate",0);intervalMinutes=prefs.getLong("interval",15);
        try { JSONObject cache=new JSONObject(prefs.getString("apiCache","{}")); java.util.Iterator<String> keys=cache.keys(); while(keys.hasNext()){String c=keys.next();JSONObject v=cache.getJSONObject(c);BigDecimal f=new BigDecimal(v.getString("factor"));if(f.signum()>0){apiCache.put(c,f);apiSources.put(c,v.optInt("source",0));}} } catch(Exception ignored) { }}
    public synchronized void save(){try{JSONArray a=new JSONArray();for(Map.Entry<String,BigDecimal> e:factors.entrySet()){JSONObject o=new JSONObject();o.put("code",e.getKey());o.put("factor",e.getValue().toPlainString());o.put("source",sources.containsKey(e.getKey())?sources.get(e.getKey()):0);a.put(o);}JSONObject cache=new JSONObject(); for(Map.Entry<String,BigDecimal> e:apiCache.entrySet()){JSONObject v=new JSONObject();v.put("factor",e.getValue().toPlainString());v.put("source",apiSources.get(e.getKey()));cache.put(e.getKey(),v);}
            prefs.edit().putString("currencies",a.toString()).putString("base",baseCode).putLong("lastUpdate",lastUpdate).putLong("interval",intervalMinutes).putString("apiCache",cache.toString()).apply();}catch(Exception ignored){}}
    public synchronized List<CurrencyItem> getCurrencies(){List<CurrencyItem> r=new ArrayList<>();for(String c:factors.keySet())r.add(CurrencyCatalog.find(c));return r;}
    public synchronized String getBaseCode(){return baseCode;} public synchronized void setBaseCode(String c){if(!factors.containsKey(c))throw new IllegalArgumentException();baseCode=c;save();}
    public synchronized BigDecimal getFactor(String c){return factors.get(c);} public synchronized BigDecimal getRate(String c){return ru.saradmin.currency.core.ConversionEngine.rate(factors.get(baseCode),factors.get(c));}
    public synchronized int getSource(String c){return staleFallback?0:(sources.containsKey(c)?Math.max(0,sources.get(c)):0);} public synchronized long getLastUpdate(){return lastUpdate;} public synchronized long getIntervalMinutes(){return intervalMinutes;} public synchronized void setIntervalMinutes(long m){if(m!=15&&m!=30&&m!=60&&m!=360&&m!=720&&m!=1440)throw new IllegalArgumentException();intervalMinutes=m;save();}
    public synchronized boolean shouldRefresh(){return lastUpdate==0||System.currentTimeMillis()-lastUpdate>=intervalMinutes*60000L;}
    public synchronized void markFallback(){staleFallback=true;} public synchronized void markRefreshFailed(){staleFallback=true;}
    public synchronized BigDecimal getCachedApiFactor(String c){return apiCache.get(c);} public synchronized int getCachedApiSource(String c){return apiSources.containsKey(c)?apiSources.get(c):0;}
    public synchronized void applyApiRates(Map<String,RateProvider.Rate> rates){for(Map.Entry<String,RateProvider.Rate> e:rates.entrySet()){apiCache.put(e.getKey(),e.getValue().value);apiSources.put(e.getKey(),e.getValue().source);} for(String c:factors.keySet()){RateProvider.Rate r=rates.get(c);if(r!=null && (!sources.containsKey(c) || sources.get(c)!=0)){factors.put(c,r.value);sources.put(c,r.source);}}if(rates.containsKey("USD"))factors.put("USD",BigDecimal.ONE);lastUpdate=System.currentTimeMillis();staleFallback=false;save();}
    public synchronized void setRateFromBase(String target,BigDecimal rate){if(!factors.containsKey(target)||target.equals(baseCode)||rate==null||rate.signum()<=0)throw new IllegalArgumentException();factors.put(target,ru.saradmin.currency.core.ConversionEngine.factorFromRate(factors.get(baseCode),rate));sources.put(target,0);staleFallback=false;save();}
    public synchronized boolean addCurrency(String code,BigDecimal rate){if(rate==null||rate.signum()<=0)return false;return addCurrencyFactor(code,ru.saradmin.currency.core.ConversionEngine.factorFromRate(factors.get(baseCode),rate),0);}
    public synchronized boolean addCurrencyFactor(String code,BigDecimal factor,int source){if(factors.size()>=MAX_CURRENCIES||factors.containsKey(code)||CurrencyCatalog.find(code)==null||factor==null||factor.signum()<=0)return false;factors.put(code,factor);sources.put(code,source);save();return true;}
    public synchronized void removeCurrency(String code){if(factors.size()<=2||code.equals(baseCode))throw new IllegalArgumentException("Keep at least two currencies and choose another base first");factors.remove(code);sources.remove(code);save();}
    public synchronized int getMaxCurrencies(){return MAX_CURRENCIES;}
}
