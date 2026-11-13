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
    private final SharedPreferences prefs; private final LinkedHashMap<String,BigDecimal> factors=new LinkedHashMap<>(); private final LinkedHashMap<String,Integer> sources=new LinkedHashMap<>(); private final LinkedHashMap<String,BigDecimal> apiCache=new LinkedHashMap<>(); private final LinkedHashMap<String,Integer> apiSources=new LinkedHashMap<>(); private final LinkedHashMap<String,Boolean> pinned=new LinkedHashMap<>();
    private String baseCode; private long lastUpdate; private long intervalMinutes=15; private boolean staleFallback; private Snapshot editSnapshot;
    public CurrencyRepository(Context context){prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);load();}
    private void load(){String raw=prefs.getString("currencies",null); if(raw!=null)try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String c=o.getString("code");BigDecimal f=new BigDecimal(o.getString("factor"));if(CurrencyCatalog.find(c)!=null&&f.signum()>0){factors.put(c,f);sources.put(c,o.optInt("source",0));pinned.put(c,o.optBoolean("pinned",false));}}baseCode=prefs.getString("base",null);}catch(Exception e){factors.clear();sources.clear();}
        if(factors.isEmpty()){factors.put("USD",BigDecimal.ONE);factors.put("EUR",new BigDecimal("0.925"));factors.put("KZT",new BigDecimal("492"));factors.put("RUB",new BigDecimal("91.5"));for(String c:factors.keySet())sources.put(c,-1);baseCode="USD";}
        if(!factors.containsKey(baseCode))baseCode=factors.keySet().iterator().next();lastUpdate=prefs.getLong("lastUpdate",0);intervalMinutes=prefs.getLong("interval",15);
        try { JSONObject cache=new JSONObject(prefs.getString("apiCache","{}")); java.util.Iterator<String> keys=cache.keys(); while(keys.hasNext()){String c=keys.next();JSONObject v=cache.getJSONObject(c);BigDecimal f=new BigDecimal(v.getString("factor"));if(f.signum()>0){apiCache.put(c,f);apiSources.put(c,v.optInt("source",0));}} } catch(Exception ignored) { }}
    private static final class Snapshot {
        final LinkedHashMap<String,BigDecimal> factors;
        final LinkedHashMap<String,Integer> sources;
        final LinkedHashMap<String,BigDecimal> apiCache;
        final LinkedHashMap<String,Integer> apiSources;
        final LinkedHashMap<String,Boolean> pinned;
        final String baseCode;
        long lastUpdate; final long intervalMinutes;
        final boolean staleFallback;
        Snapshot(CurrencyRepository r) {
            factors = new LinkedHashMap<>(r.factors);
            sources = new LinkedHashMap<>(r.sources);
            apiCache = new LinkedHashMap<>(r.apiCache);
            apiSources = new LinkedHashMap<>(r.apiSources);
            pinned = new LinkedHashMap<>(r.pinned);
            baseCode = r.baseCode; lastUpdate = r.lastUpdate;
            intervalMinutes = r.intervalMinutes; staleFallback = r.staleFallback;
        }
    }
    public synchronized void beginEdit() { if (editSnapshot == null) editSnapshot = new Snapshot(this); }
    public synchronized void commitEdit() { if (editSnapshot != null) { editSnapshot = null; save(); } }
    public synchronized void cancelEdit() {
        if (editSnapshot == null) return;
        factors.clear(); factors.putAll(editSnapshot.factors);
        sources.clear(); sources.putAll(editSnapshot.sources);
        apiCache.clear(); apiCache.putAll(editSnapshot.apiCache);
        apiSources.clear(); apiSources.putAll(editSnapshot.apiSources);
        pinned.clear(); pinned.putAll(editSnapshot.pinned);
        baseCode = editSnapshot.baseCode; lastUpdate = editSnapshot.lastUpdate;
        intervalMinutes = editSnapshot.intervalMinutes; staleFallback = editSnapshot.staleFallback;
        editSnapshot = null;
    }
    public synchronized void save(){if(editSnapshot != null) return; try{JSONArray a=new JSONArray();for(Map.Entry<String,BigDecimal> e:factors.entrySet()){JSONObject o=new JSONObject();o.put("code",e.getKey());o.put("factor",e.getValue().toPlainString());o.put("source",sources.containsKey(e.getKey())?sources.get(e.getKey()):0);o.put("pinned",pinned.containsKey(e.getKey())&&pinned.get(e.getKey()));a.put(o);}JSONObject cache=new JSONObject(); for(Map.Entry<String,BigDecimal> e:apiCache.entrySet()){JSONObject v=new JSONObject();v.put("factor",e.getValue().toPlainString());v.put("source",apiSources.get(e.getKey()));cache.put(e.getKey(),v);}
            prefs.edit().putString("currencies",a.toString()).putString("base",baseCode).putLong("lastUpdate",lastUpdate).putLong("interval",intervalMinutes).putString("apiCache",cache.toString()).apply();}catch(Exception ignored){}}
    public synchronized List<CurrencyItem> getCurrencies(){List<CurrencyItem> r=new ArrayList<>();for(String c:CurrencyOrder.baseFirst(new ArrayList<>(factors.keySet()),baseCode))r.add(CurrencyCatalog.find(c));return r;}
    public synchronized String getBaseCode(){return baseCode;} public synchronized void setBaseCode(String c){if(!factors.containsKey(c))throw new IllegalArgumentException();baseCode=c;save();}
    public synchronized BigDecimal getFactor(String c){return factors.get(c);} public synchronized BigDecimal getRate(String c){return ru.saradmin.currency.core.ConversionEngine.rate(factors.get(baseCode),factors.get(c));}
    public synchronized int getSource(String c){return sources.containsKey(c)?Math.max(0,sources.get(c)):0;} public synchronized boolean isPinned(String c){return pinned.containsKey(c)&&pinned.get(c);} public synchronized void setPinned(String c,boolean value){if(!factors.containsKey(c)||c.equals(baseCode))return;pinned.put(c,value);save();} public synchronized long getLastUpdate(){return lastUpdate;} public synchronized long getIntervalMinutes(){return intervalMinutes;} public synchronized void setIntervalMinutes(long m){if(m!=15&&m!=30&&m!=60&&m!=360&&m!=720&&m!=1440)throw new IllegalArgumentException();intervalMinutes=m;save();}
    public synchronized boolean shouldRefresh(){return lastUpdate==0||System.currentTimeMillis()-lastUpdate>=intervalMinutes*60000L;}
    public synchronized void markFallback(){} public synchronized void markRefreshFailed(){}
    public synchronized BigDecimal getCachedApiFactor(String c){return apiCache.get(c);} public synchronized int getCachedApiSource(String c){return apiSources.containsKey(c)?apiSources.get(c):0;}
    private void persistSnapshotState(Snapshot s) {
        try {
            JSONArray a = new JSONArray();
            for (Map.Entry<String,BigDecimal> e : s.factors.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("code", e.getKey());
                o.put("factor", e.getValue().toPlainString());
                o.put("source", s.sources.containsKey(e.getKey()) ? s.sources.get(e.getKey()) : 0);
                o.put("pinned", s.pinned.containsKey(e.getKey()) && s.pinned.get(e.getKey()));
                a.put(o);
            }
            JSONObject cache = new JSONObject();
            for (Map.Entry<String,BigDecimal> e : s.apiCache.entrySet()) {
                JSONObject v = new JSONObject();
                v.put("factor", e.getValue().toPlainString());
                v.put("source", s.apiSources.containsKey(e.getKey()) ? s.apiSources.get(e.getKey()) : 0);
                cache.put(e.getKey(), v);
            }
            prefs.edit().putString("currencies", a.toString())
                    .putString("base", s.baseCode)
                    .putLong("lastUpdate", s.lastUpdate)
                    .putLong("interval", s.intervalMinutes)
                    .putString("apiCache", cache.toString()).apply();
        } catch (Exception ignored) { }
    }

    public synchronized void applyApiRates(Map<String,RateProvider.Rate> rates) {
        long updatedAt = System.currentTimeMillis();
        for (Map.Entry<String,RateProvider.Rate> e : rates.entrySet()) {
            apiCache.put(e.getKey(), e.getValue().value);
            apiSources.put(e.getKey(), e.getValue().source);
        }
        for (String c : factors.keySet()) {
            RateProvider.Rate r = rates.get(c);
            if (r != null && !isPinned(c)) {
                factors.put(c, r.value);
                sources.put(c, r.source);
            }
        }
        lastUpdate = updatedAt;
        staleFallback = false;

        // A network refresh is independent of the settings dialog's Save/Cancel
        // transaction. Update its rollback baseline too, so Cancel can discard
        // manual edits without reverting freshly downloaded API rates.
        if (editSnapshot != null) {
            for (Map.Entry<String,RateProvider.Rate> e : rates.entrySet()) {
                String c = e.getKey();
                RateProvider.Rate r = e.getValue();
                editSnapshot.apiCache.put(c, r.value);
                editSnapshot.apiSources.put(c, r.source);
                if (editSnapshot.factors.containsKey(c)
                        && !(editSnapshot.pinned.containsKey(c) && editSnapshot.pinned.get(c))) {
                    editSnapshot.factors.put(c, r.value);
                    editSnapshot.sources.put(c, r.source);
                }
            }
            editSnapshot.lastUpdate = updatedAt;
            persistSnapshotState(editSnapshot);
        }
        save();
    }
    public synchronized void setRateFromBase(String target,BigDecimal rate){if(!factors.containsKey(target)||target.equals(baseCode)||rate==null||rate.signum()<=0)throw new IllegalArgumentException();BigDecimal current=getRate(target);if(current.compareTo(rate)!=0){factors.put(target,ru.saradmin.currency.core.ConversionEngine.factorFromRate(factors.get(baseCode),rate));sources.put(target,0);}save();}
    public synchronized boolean addCurrency(String code,BigDecimal rate){if(rate==null||rate.signum()<=0)return false;return addCurrencyFactor(code,ru.saradmin.currency.core.ConversionEngine.factorFromRate(factors.get(baseCode),rate),0);}
    public synchronized boolean addCurrencyFactor(String code,BigDecimal factor,int source){if(factors.size()>=MAX_CURRENCIES||factors.containsKey(code)||CurrencyCatalog.find(code)==null||factor==null||factor.signum()<=0)return false;factors.put(code,factor);sources.put(code,source);pinned.put(code,false);save();return true;}
    public synchronized void removeCurrency(String code){if(factors.size()<=2||code.equals(baseCode))throw new IllegalArgumentException("Keep at least two currencies and choose another base first");factors.remove(code);sources.remove(code);pinned.remove(code);save();}
    /** Reorders the non-base currencies and persists their display order. */
    public synchronized void reorderCurrencies(List<String> orderedCodes) {
        if (orderedCodes == null || orderedCodes.size() != factors.size() - 1)
            throw new IllegalArgumentException("Invalid currency order");
        java.util.HashSet<String> unique = new java.util.HashSet<>(orderedCodes);
        java.util.HashSet<String> expected = new java.util.HashSet<>(factors.keySet());
        expected.remove(baseCode);
        if (unique.size() != orderedCodes.size() || unique.contains(baseCode) || !expected.equals(unique))
            throw new IllegalArgumentException("Invalid currency order");
        LinkedHashMap<String,BigDecimal> reorderedFactors = new LinkedHashMap<>();
        LinkedHashMap<String,Integer> reorderedSources = new LinkedHashMap<>();
        reorderedFactors.put(baseCode, factors.get(baseCode));
        reorderedSources.put(baseCode, sources.get(baseCode));
        for (String code : orderedCodes) {
            reorderedFactors.put(code, factors.get(code));
            reorderedSources.put(code, sources.get(code));
        }
        factors.clear(); factors.putAll(reorderedFactors);
        sources.clear(); sources.putAll(reorderedSources);
        save();
    }
    public synchronized int getMaxCurrencies(){return MAX_CURRENCIES;}
}
