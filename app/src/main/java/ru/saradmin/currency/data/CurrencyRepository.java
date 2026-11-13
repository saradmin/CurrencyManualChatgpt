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
    private final SharedPreferences prefs; private final LinkedHashMap<String,BigDecimal> factors=new LinkedHashMap<>(); private final LinkedHashMap<String,Integer> sources=new LinkedHashMap<>(); private final LinkedHashMap<String,BigDecimal> apiCache=new LinkedHashMap<>(); private final LinkedHashMap<String,Integer> apiSources=new LinkedHashMap<>(); private final LinkedHashMap<String,Boolean> pinned=new LinkedHashMap<>(); private final LinkedHashMap<String,BigDecimal> pairRates=new LinkedHashMap<>(); private final LinkedHashMap<String,Integer> pairSources=new LinkedHashMap<>(); private final LinkedHashMap<String,Boolean> pairPinned=new LinkedHashMap<>();
    private String baseCode; private long lastUpdate; private long intervalMinutes=15; private boolean staleFallback; private Snapshot editSnapshot;
    public CurrencyRepository(Context context){prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);load();}
    private void load(){String raw=prefs.getString("currencies",null); if(raw!=null)try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String c=o.getString("code");BigDecimal f=new BigDecimal(o.getString("factor"));if(CurrencyCatalog.find(c)!=null&&f.signum()>0){factors.put(c,f);sources.put(c,o.optInt("source",0));pinned.put(c,o.optBoolean("pinned",false));}}baseCode=prefs.getString("base",null);}catch(Exception e){factors.clear();sources.clear();}
        if(factors.isEmpty()){factors.put("USD",BigDecimal.ONE);factors.put("EUR",new BigDecimal("0.925"));factors.put("KZT",new BigDecimal("492"));factors.put("RUB",new BigDecimal("91.5"));for(String c:factors.keySet())sources.put(c,-1);baseCode="USD";}
        if(!factors.containsKey(baseCode))baseCode=factors.keySet().iterator().next();lastUpdate=prefs.getLong("lastUpdate",0);intervalMinutes=prefs.getLong("interval",15);
        try { JSONObject cache=new JSONObject(prefs.getString("apiCache","{}")); java.util.Iterator<String> keys=cache.keys(); while(keys.hasNext()){String c=keys.next();JSONObject v=cache.getJSONObject(c);BigDecimal f=new BigDecimal(v.getString("factor"));if(f.signum()>0){apiCache.put(c,f);apiSources.put(c,v.optInt("source",0));}} } catch(Exception ignored) { }
        try { JSONArray pairs = new JSONArray(prefs.getString("pairRates", "[]")); for (int i=0;i<pairs.length();i++) { JSONObject o=pairs.getJSONObject(i); String key=pairKey(o.getString("base"),o.getString("target")); pairRates.put(key,new BigDecimal(o.getString("rate"))); pairSources.put(key,o.optInt("source",0)); pairPinned.put(key,o.optBoolean("pinned",false)); } } catch(Exception ignored) { }
        // One-time migration from releases that stored a manual quote by changing
        // the target's global factor. Preserve the currently visible manual quote
        // as a pair override; future edits will no longer affect unrelated pairs.
        if (pairRates.isEmpty() && baseCode != null) {
            for (String c : factors.keySet()) {
                if (!c.equals(baseCode) && sources.containsKey(c) && sources.get(c) == 0) {
                    BigDecimal legacyRate = ru.saradmin.currency.core.ConversionEngine.rate(factors.get(baseCode), factors.get(c));
                    String key = pairKey(baseCode, c);
                    pairRates.put(key, legacyRate);
                    pairSources.put(key, 0);
                    pairPinned.put(key, pinned.containsKey(c) && pinned.get(c));
                }
            }
            if (!pairRates.isEmpty()) save();
        }
    }
    private static final class Snapshot {
        final LinkedHashMap<String,BigDecimal> factors;
        final LinkedHashMap<String,Integer> sources;
        final LinkedHashMap<String,BigDecimal> apiCache;
        final LinkedHashMap<String,Integer> apiSources;
        final LinkedHashMap<String,Boolean> pinned;
        final LinkedHashMap<String,BigDecimal> pairRates;
        final LinkedHashMap<String,Integer> pairSources;
        final LinkedHashMap<String,Boolean> pairPinned;
        final String baseCode;
        long lastUpdate; final long intervalMinutes;
        final boolean staleFallback;
        Snapshot(CurrencyRepository r) {
            factors = new LinkedHashMap<>(r.factors);
            sources = new LinkedHashMap<>(r.sources);
            apiCache = new LinkedHashMap<>(r.apiCache);
            apiSources = new LinkedHashMap<>(r.apiSources);
            pinned = new LinkedHashMap<>(r.pinned);
            pairRates = new LinkedHashMap<>(r.pairRates);
            pairSources = new LinkedHashMap<>(r.pairSources);
            pairPinned = new LinkedHashMap<>(r.pairPinned);
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
        pairRates.clear(); pairRates.putAll(editSnapshot.pairRates);
        pairSources.clear(); pairSources.putAll(editSnapshot.pairSources);
        pairPinned.clear(); pairPinned.putAll(editSnapshot.pairPinned);
        baseCode = editSnapshot.baseCode; lastUpdate = editSnapshot.lastUpdate;
        intervalMinutes = editSnapshot.intervalMinutes; staleFallback = editSnapshot.staleFallback;
        editSnapshot = null;
    }
    private static String pairKey(String base, String target) { return base + ">" + target; }
    private static JSONArray serializePairs(Map<String,BigDecimal> rates, Map<String,Integer> sources, Map<String,Boolean> pins) throws Exception {
        JSONArray a = new JSONArray();
        for (Map.Entry<String,BigDecimal> e : rates.entrySet()) {
            String[] parts=e.getKey().split(">",2); JSONObject o=new JSONObject();
            o.put("base",parts[0]); o.put("target",parts[1]); o.put("rate",e.getValue().toPlainString());
            o.put("source",sources.containsKey(e.getKey())?sources.get(e.getKey()):0);
            o.put("pinned",pins.containsKey(e.getKey())&&pins.get(e.getKey())); a.put(o);
        }
        return a;
    }
    private String matchingPairKey(String base, String target) {
        String direct=pairKey(base,target), reverse=pairKey(target,base);
        if (pairRates.containsKey(direct)) return direct;
        if (pairRates.containsKey(reverse)) return reverse;
        return null;
    }
    public synchronized void save(){if(editSnapshot != null) return; try{JSONArray a=new JSONArray();for(Map.Entry<String,BigDecimal> e:factors.entrySet()){JSONObject o=new JSONObject();o.put("code",e.getKey());o.put("factor",e.getValue().toPlainString());o.put("source",sources.containsKey(e.getKey())?sources.get(e.getKey()):0);o.put("pinned",pinned.containsKey(e.getKey())&&pinned.get(e.getKey()));a.put(o);}JSONObject cache=new JSONObject(); for(Map.Entry<String,BigDecimal> e:apiCache.entrySet()){JSONObject v=new JSONObject();v.put("factor",e.getValue().toPlainString());v.put("source",apiSources.get(e.getKey()));cache.put(e.getKey(),v);}
            JSONArray pairs = serializePairs(pairRates, pairSources, pairPinned); prefs.edit().putString("currencies",a.toString()).putString("base",baseCode).putLong("lastUpdate",lastUpdate).putLong("interval",intervalMinutes).putString("apiCache",cache.toString()).putString("pairRates",pairs.toString()).apply();}catch(Exception ignored){}}
    public synchronized List<CurrencyItem> getCurrencies(){List<CurrencyItem> r=new ArrayList<>();for(String c:CurrencyOrder.baseFirst(new ArrayList<>(factors.keySet()),baseCode))r.add(CurrencyCatalog.find(c));return r;}
    public synchronized String getBaseCode(){return baseCode;} public synchronized void setBaseCode(String c){if(!factors.containsKey(c))throw new IllegalArgumentException();baseCode=c;save();}
    public synchronized BigDecimal getFactor(String c){return factors.get(c);} public synchronized BigDecimal getRate(String c){String key=matchingPairKey(baseCode,c); if(key!=null){BigDecimal v=pairRates.get(key);return ru.saradmin.currency.core.PairRate.orient(v,key.equals(pairKey(baseCode,c)));} return ru.saradmin.currency.core.ConversionEngine.rate(factors.get(baseCode),factors.get(c));}
    public synchronized int getSource(String c){String key=matchingPairKey(baseCode,c); if(key!=null)return Math.max(0,pairSources.containsKey(key)?pairSources.get(key):0);return sources.containsKey(c)?Math.max(0,sources.get(c)):0;} public synchronized boolean isPinned(String c){String key=matchingPairKey(baseCode,c);return key!=null?(pairPinned.containsKey(key)&&pairPinned.get(key)):(pinned.containsKey(c)&&pinned.get(c));} public synchronized void setPinned(String c,boolean value){if(!factors.containsKey(c)||c.equals(baseCode))return;String key=matchingPairKey(baseCode,c);if(key==null)key=pairKey(baseCode,c);pairPinned.put(key,value);save();} public synchronized long getLastUpdate(){return lastUpdate;} public synchronized long getIntervalMinutes(){return intervalMinutes;} public synchronized void setIntervalMinutes(long m){if(m!=15&&m!=30&&m!=60&&m!=360&&m!=720&&m!=1440)throw new IllegalArgumentException();intervalMinutes=m;save();}
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
                    .putString("apiCache", cache.toString()).putString("pairRates", serializePairs(s.pairRates, s.pairSources, s.pairPinned).toString()).apply();
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
            if (r != null && !(pinned.containsKey(c) && pinned.get(c))) {
                factors.put(c, r.value);
                sources.put(c, r.source);
            }
        }
        java.util.Iterator<String> pairIt=pairRates.keySet().iterator();
        while(pairIt.hasNext()) { String key=pairIt.next(); String[] p=key.split(">",2); boolean refreshed=rates.containsKey(p[0])||rates.containsKey(p[1]); if(refreshed && !(pairPinned.containsKey(key)&&pairPinned.get(key))) { pairIt.remove(); pairSources.remove(key); pairPinned.remove(key); } }
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
            java.util.Iterator<String> snapPairIt=editSnapshot.pairRates.keySet().iterator();
            while(snapPairIt.hasNext()) { String key=snapPairIt.next(); String[] p=key.split(">",2); boolean refreshed=rates.containsKey(p[0])||rates.containsKey(p[1]); if(refreshed && !(editSnapshot.pairPinned.containsKey(key)&&editSnapshot.pairPinned.get(key))) { snapPairIt.remove(); editSnapshot.pairSources.remove(key); editSnapshot.pairPinned.remove(key); } }
            editSnapshot.lastUpdate = updatedAt;
            persistSnapshotState(editSnapshot);
        }
        save();
    }
    public synchronized void setRateFromBase(String target,BigDecimal rate){if(!factors.containsKey(target)||target.equals(baseCode)||rate==null||rate.signum()<=0)throw new IllegalArgumentException();String key=pairKey(baseCode,target);String reverse=pairKey(target,baseCode);pairRates.remove(reverse);pairSources.remove(reverse);pairPinned.remove(reverse);pairRates.put(key,rate);pairSources.put(key,0);pairPinned.putIfAbsent(key,false);save();}
    public synchronized boolean addCurrency(String code,BigDecimal rate){if(rate==null||rate.signum()<=0)return false;return addCurrencyFactor(code,ru.saradmin.currency.core.ConversionEngine.factorFromRate(factors.get(baseCode),rate),0);}
    public synchronized boolean addCurrencyFactor(String code,BigDecimal factor,int source){if(factors.size()>=MAX_CURRENCIES||factors.containsKey(code)||CurrencyCatalog.find(code)==null||factor==null||factor.signum()<=0)return false;factors.put(code,factor);sources.put(code,source);pinned.put(code,false);save();return true;}
    public synchronized void removeCurrency(String code){if(factors.size()<=2||code.equals(baseCode))throw new IllegalArgumentException("Keep at least two currencies and choose another base first");factors.remove(code);sources.remove(code);pinned.remove(code);String prefix=code+">", suffix=">"+code;java.util.Iterator<String> it=pairRates.keySet().iterator();while(it.hasNext()){String key=it.next();if(key.startsWith(prefix)||key.endsWith(suffix)){it.remove();pairSources.remove(key);pairPinned.remove(key);}}save();}
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
