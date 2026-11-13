package ru.saradmin.currency;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import ru.saradmin.currency.core.ConversionEngine;
import ru.saradmin.currency.data.CurrencyCatalog;
import ru.saradmin.currency.data.CurrencyRepository;
import ru.saradmin.currency.data.RateProvider;
import ru.saradmin.currency.model.CurrencyItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(32, 35, 41);
    private static final int GREEN = Color.rgb(37, 137, 232);
    private static final int DARK = Color.rgb(242, 244, 247);
    private static final int MUTED = Color.rgb(167, 175, 186);
    private static final int CARD = Color.rgb(43, 48, 56);
    private static final int BORDER = Color.rgb(62, 69, 80);
    private CurrencyRepository repository;
    private EditText amountInput;
    private LinearLayout resultsContainer;
    private ScrollView resultsScroll;
    private TextView baseFlag, baseName;
    private AlertDialog settingsDialog;
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private TextView statusHint;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        repository = new CurrencyRepository(this);
        buildMainScreen();
    }

    @Override protected void onResume() { super.onResume(); refreshRatesIfNeeded(false); }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable rounded(int color, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(16));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView label(String value, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(DARK);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private void padding(View v, int l, int t, int r, int b) { v.setPadding(dp(l), dp(t), dp(r), dp(b)); }

    private void buildMainScreen() {
        LinearLayout root = column(); root.setBackgroundColor(BG); padding(root, 16, 10, 16, 10);
        setContentView(root);

        LinearLayout heading = row();
        TextView title = label("Конвертер", 22, true);
        heading.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button settings = smallButton("⚙", false); heading.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(42)));
        settings.setOnClickListener(v -> showSettings()); root.addView(heading);

        LinearLayout inputCard = column(); inputCard.setBackground(rounded(CARD, BORDER)); padding(inputCard, 14, 10, 14, 10);
        LinearLayout inputColumn = column();
        TextView caption = label("СУММА", 11, true); caption.setTextColor(MUTED); inputColumn.addView(caption);
        amountInput = new EditText(this); amountInput.setSingleLine(true); amountInput.setTextSize(30); amountInput.setTextColor(DARK);
        amountInput.setHint("Введите сумму"); amountInput.setHintTextColor(0xffaab2ac); amountInput.setSelectAllOnFocus(false);
        amountInput.setInputType(android.text.InputType.TYPE_NULL); amountInput.setShowSoftInputOnFocus(false);
        amountInput.setPadding(0, dp(2), 0, 0); amountInput.setBackgroundColor(Color.TRANSPARENT); amountInput.setText("");
        inputColumn.addView(amountInput, new LinearLayout.LayoutParams(-1, dp(48)));
        inputCard.addView(inputColumn, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout basePicker = row(); basePicker.setBackground(rounded(0xff363d48, 0)); padding(basePicker, 10, 8, 8, 8);
        baseFlag = label("", 25, false); basePicker.addView(baseFlag);
        baseName = label("", 15, true); LinearLayout.LayoutParams bnp = new LinearLayout.LayoutParams(-2, -2); bnp.leftMargin = dp(7); basePicker.addView(baseName, bnp);
        TextView arrow = label("⌄", 18, true); LinearLayout.LayoutParams arp = new LinearLayout.LayoutParams(-2, -2); arp.leftMargin = dp(5); basePicker.addView(arrow, arp);
        basePicker.setOnClickListener(v -> chooseBase());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2); bp.topMargin = dp(8); inputCard.addView(basePicker, bp);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2); ip.topMargin = dp(8); root.addView(inputCard, ip);

        statusHint = label("Курсы сохранены на устройстве", 11, false); statusHint.setTextColor(MUTED); statusHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(28)); root.addView(statusHint, hp);

        resultsScroll = new ScrollView(this); resultsScroll.setFillViewport(false); resultsScroll.setClipToPadding(false);
        resultsContainer = column(); resultsScroll.addView(resultsContainer);
        LinearLayout.LayoutParams rsp = new LinearLayout.LayoutParams(-1, 0, 1); rsp.topMargin = dp(2); rsp.bottomMargin = dp(8); root.addView(resultsScroll, rsp);

        root.addView(buildNumericKeyboard(), new LinearLayout.LayoutParams(-1, dp(194)));
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshResults(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        updateBaseLabel(); refreshResults();
        amountInput.requestFocus();
    }

    private LinearLayout buildNumericKeyboard() {
        LinearLayout keyboard = column(); keyboard.setBackground(rounded(CARD, BORDER)); padding(keyboard, 5, 5, 5, 5);
        LinearLayout top = row(); TextView keyTitle = label("Цифровой ввод", 11, true); keyTitle.setTextColor(MUTED);
        top.addView(keyTitle, new LinearLayout.LayoutParams(0, dp(30), 1));
        Button clear = keyButton("✕"); LinearLayout.LayoutParams clearP = new LinearLayout.LayoutParams(dp(48), dp(30)); clearP.setMargins(dp(3), 0, dp(3), 0); top.addView(clear, clearP);
        clear.setOnClickListener(v -> { amountInput.setText(""); amountInput.setSelection(0); amountInput.requestFocus(); }); keyboard.addView(top);
        String[][] keys = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {".", "0", "⌫"}};
        for (String[] line : keys) { LinearLayout r = row(); for (String key : line) { Button b = keyButton(key); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(34), 1); p.setMargins(dp(3), dp(2), dp(3), dp(2)); r.addView(b,p); b.setOnClickListener(v -> onKey(key)); } keyboard.addView(r, new LinearLayout.LayoutParams(-1, dp(38))); }
        return keyboard;
    }

    private Button smallButton(String text, boolean light) {
        Button b = new Button(this); b.setText(text); b.setTextSize(16); b.setAllCaps(false); b.setPadding(0, 0, 0, 0);
        b.setTextColor(Color.WHITE); b.setBackground(rounded(light ? 0xff363d48 : GREEN, 0)); return b;
    }
    private Button keyButton(String text) {
        Button b = new Button(this); b.setText(text); b.setTextSize(20); b.setAllCaps(false); b.setPadding(0, 0, 0, 0); b.setMinHeight(0); b.setMinimumHeight(0); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setTextColor(DARK); b.setBackground(rounded(0xff363d48, BORDER)); return b;
    }
    private void onKey(String key) {
        String current = amountInput.getText().toString();
        if ("⌫".equals(key)) {
            if (!current.isEmpty()) current = current.substring(0, current.length() - 1);
        } else if (".".equals(key) || ",".equals(key)) {
            if (current.contains(".") || current.contains(",")) return;
            current = current.isEmpty() ? "0." : current + ".";
        } else {
            if (current.length() >= 15) return;
            current += key;
        }
        amountInput.setText(current); amountInput.setSelection(current.length()); amountInput.requestFocus();
    }

    private BigDecimal parseAmount() {
        String value = amountInput == null ? "" : amountInput.getText().toString().trim().replace(',', '.');
        if (value.isEmpty() || ".".equals(value)) return BigDecimal.ZERO;
        try { return new BigDecimal(value); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
    private String format(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        DecimalFormat f = new DecimalFormat("#,##0.####", symbols); f.setRoundingMode(RoundingMode.HALF_UP);
        return f.format(value.stripTrailingZeros());
    }
    private void refreshResults() {
        if (resultsContainer == null) return;
        resultsContainer.removeAllViews();
        BigDecimal amount = parseAmount();
        String base = repository.getBaseCode();
        for (CurrencyItem item : repository.getCurrencies()) {
            if (item.code.equals(base)) continue;
            LinearLayout card = row(); card.setBackground(rounded(CARD, BORDER)); padding(card, 12, 10, 12, 10);
            View flag = flagView(item); card.addView(flag, new LinearLayout.LayoutParams(dp(38), dp(28)));
            LinearLayout details = column(); TextView code = label(item.code + "   " + sourceMark(repository.getSource(item.code)), 16, true); details.addView(code);
            BigDecimal rate = repository.getRate(item.code);
            TextView rateText = label("1 " + base + " = " + format(rate) + " " + item.code, 10, false); rateText.setTextColor(MUTED); details.addView(rateText);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(0, -2, 1); dp.leftMargin = dp(10); card.addView(details, dp);
            BigDecimal result = ConversionEngine.convert(amount, repository.getFactor(base), repository.getFactor(item.code));
            TextView value = label(amountInput.getText().length() == 0 ? "···" : format(result), 19, true); value.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT); card.addView(value);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.bottomMargin = dp(7); resultsContainer.addView(card, cp);
        }
    }

    private String sourceMark(int source) { return source == 1 ? "🟢¹" : source == 2 ? "🟢²" : "🔵"; }

    private View flagView(CurrencyItem item) {
        File cached = new File(getFilesDir(), "flag-" + item.countryCode + ".svg");
        if (!cached.exists()) return label(item.flagEmoji, 24, false);
        WebView web = new WebView(this); web.setBackgroundColor(Color.TRANSPARENT); web.setVerticalScrollBarEnabled(false); web.setHorizontalScrollBarEnabled(false);
        WebSettings settings = web.getSettings(); settings.setJavaScriptEnabled(false); settings.setAllowFileAccess(true); settings.setAllowContentAccess(false);
        web.loadUrl("file://" + cached.getAbsolutePath()); return web;
    }
    private void updateBaseLabel() {
        CurrencyItem base = CurrencyCatalog.find(repository.getBaseCode());
        if (base != null) { baseFlag.setText(base.flagEmoji); baseName.setText(base.code); }
    }
    private void chooseBase() {
        List<CurrencyItem> currencies = repository.getCurrencies(); String[] labels = new String[currencies.size()];
        for (int i = 0; i < currencies.size(); i++) { CurrencyItem c = currencies.get(i); labels[i] = c.flagEmoji + "   " + c.code + " · " + c.name; }
        new AlertDialog.Builder(this).setTitle("Основная валюта").setItems(labels, (dialog, which) -> {
            repository.setBaseCode(currencies.get(which).code);
            updateBaseLabel(); refreshResults();
        }).setNegativeButton("Отмена", null).show();
    }

    private void showSettings() {
        ScrollView scroll = new ScrollView(this); LinearLayout body = column(); padding(body, 18, 16, 18, 18); scroll.addView(body);
        TextView title = label("Валюты и курсы", 22, true); body.addView(title);
        TextView description = label("Курс каждой валюты показан относительно текущей основной валюты. Изменения сохраняются на устройстве.", 13, false);
        description.setTextColor(MUTED); LinearLayout.LayoutParams descP = new LinearLayout.LayoutParams(-1, -2); descP.topMargin = dp(8); body.addView(description, descP);
        TextView apiInfo = label("Источник: ¹ Frankfurter · ² ExchangeRate-API · 🔵 ручной/резервный курс", 11, false); apiInfo.setTextColor(MUTED); LinearLayout.LayoutParams aip = new LinearLayout.LayoutParams(-1,-2); aip.topMargin=dp(6); body.addView(apiInfo,aip);
        Button interval = smallButton("Обновление: " + intervalLabel(repository.getIntervalMinutes()), true); LinearLayout.LayoutParams intp=new LinearLayout.LayoutParams(-1,dp(42)); intp.topMargin=dp(10); body.addView(interval,intp);
        interval.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Интервал обновления").setItems(new String[]{"15 минут","30 минут","1 час","6 часов","12 часов","24 часа"},(d,w)->{long[] vals={15,30,60,360,720,1440};repository.setIntervalMinutes(vals[w]);interval.setText("Обновление: "+intervalLabel(vals[w]));refreshRatesIfNeeded(true);}).show());
        Button updateNow=smallButton("Обновить курсы сейчас",true); LinearLayout.LayoutParams unp=new LinearLayout.LayoutParams(-1,dp(42));unp.topMargin=dp(6);body.addView(updateNow,unp);updateNow.setOnClickListener(v->refreshRatesIfNeeded(true));
        for (CurrencyItem item : repository.getCurrencies()) {
            LinearLayout line = row(); padding(line, 0, 10, 0, 10); line.addView(label(item.flagEmoji, 23, false));
            TextView name = label(item.code + (item.code.equals(repository.getBaseCode()) ? " · основная" : "   " + sourceMark(repository.getSource(item.code))), 15, true);
            LinearLayout.LayoutParams nameP = new LinearLayout.LayoutParams(0, -2, 1); nameP.leftMargin = dp(9); line.addView(name, nameP);
            if (!item.code.equals(repository.getBaseCode())) {
                TextView prefix = label("1 " + repository.getBaseCode() + " =", 12, false); line.addView(prefix);
                EditText rate = new EditText(this); rate.setSingleLine(true); rate.setTextSize(15); rate.setEms(5);
                rate.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
                rate.setHint("курс"); rate.setSelectAllOnFocus(true); rate.setText(repository.getRate(item.code).stripTrailingZeros().toPlainString());
                line.addView(rate);
                rate.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) saveRate(item.code, rate.getText().toString()); });
            }
            if (!item.code.equals(repository.getBaseCode())) { TextView remove=label("Удалить",11,true); remove.setTextColor(0xffE34B55); remove.setPadding(dp(8),0,0,0); line.addView(remove); remove.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("Удалить валюту "+item.code+"?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{repository.removeCurrency(item.code);settingsDialog.dismiss();showSettings();refreshResults();}).show()); }
            body.addView(line); View divider = new View(this); divider.setBackgroundColor(0xffe7ebe8); body.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }
        Button add = smallButton("＋  Добавить валюту", true); LinearLayout.LayoutParams addP = new LinearLayout.LayoutParams(-1, dp(48)); addP.topMargin = dp(14); body.addView(add, addP);
        add.setEnabled(repository.getCurrencies().size() < repository.getMaxCurrencies()); add.setOnClickListener(v -> { showAddCurrency(); });
        TextView count = label("Добавлено валют: " + repository.getCurrencies().size() + " из " + repository.getMaxCurrencies(), 12, false); count.setTextColor(MUTED); count.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams countP = new LinearLayout.LayoutParams(-1, -2); countP.topMargin = dp(10); body.addView(count, countP);
        TextView version = label("CurrencyManual  v" + getAppVersionName() + "  (" + getAppVersionCode() + ")", 11, false);
        version.setTextColor(MUTED); version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams versionP = new LinearLayout.LayoutParams(-1, -2); versionP.topMargin = dp(18); body.addView(version, versionP);
        version.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("О приложении")
                .setMessage("CurrencyManual\nВерсия: " + getAppVersionName() + "\nНомер сборки: " + getAppVersionCode() + "\nПакет: " + getPackageName())
                .setPositiveButton("Закрыть", null).show());
        settingsDialog = new AlertDialog.Builder(this).setView(scroll).setPositiveButton("Готово", (d, w) -> { repository.save(); updateBaseLabel(); refreshResults(); })
                .setNegativeButton("Закрыть", (d, w) -> { updateBaseLabel(); refreshResults(); }).create();
        settingsDialog.show();
    }

    private String getAppVersionName() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception e) { return "unknown"; }
    }

    private long getAppVersionCode() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (android.os.Build.VERSION.SDK_INT >= 28) return info.getLongVersionCode();
            return info.versionCode;
        } catch (Exception e) { return 0L; }
    }

    private void saveRate(String code, String text) {
        String normalized = text.trim().replace(',', '.');
        try {
            BigDecimal rate = new BigDecimal(normalized);
            if (rate.signum() <= 0) throw new NumberFormatException();
            repository.setRateFromBase(code, rate);
        } catch (Exception e) { Toast.makeText(this, "Курс должен быть числом больше нуля", Toast.LENGTH_SHORT).show(); }
    }

    private void showAddCurrency() {
        if (repository.getCurrencies().size() >= repository.getMaxCurrencies()) return;
        LinearLayout content = column(); padding(content, 18, 8, 18, 8);
        EditText search = new EditText(this); search.setSingleLine(true); search.setHint("Поиск валюты по коду или названию"); content.addView(search);
        ListView list = new ListView(this); content.addView(list, new LinearLayout.LayoutParams(-1, dp(300)));
        ArrayList<CurrencyItem> available = new ArrayList<>();
        for (CurrencyItem item : CurrencyCatalog.ALL) {
            boolean exists = false; for (CurrencyItem current : repository.getCurrencies()) if (current.code.equals(item.code)) exists = true;
            if (!exists) available.add(item);
        }
        ArrayList<CurrencyItem> shown = new ArrayList<>(available);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayCurrencies(shown)); list.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Добавить валюту").setView(content).setNegativeButton("Отмена", null).create();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                shown.clear(); String q = s.toString().toLowerCase(Locale.ROOT);
                for (CurrencyItem item : available) if (item.code.toLowerCase(Locale.ROOT).contains(q) || item.name.toLowerCase(Locale.ROOT).contains(q)) shown.add(item);
                adapter.clear(); adapter.addAll(displayCurrencies(shown)); adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable e) { }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            CurrencyItem selected = shown.get(position); dialog.dismiss(); askInitialRate(selected);
        });
        dialog.show();
    }
    private List<String> displayCurrencies(List<CurrencyItem> items) {
        List<String> labels = new ArrayList<>(); for (CurrencyItem item : items) labels.add(item.flagEmoji + "   " + item.code + " · " + item.name); return labels;
    }
    private void askInitialRate(CurrencyItem item) {
        BigDecimal apiFactor = repository.getCachedApiFactor(item.code);
        if (apiFactor != null) {
            if (repository.addCurrencyFactor(item.code, apiFactor, repository.getCachedApiSource(item.code))) { downloadFlagOnce(item); Toast.makeText(this,"Валюта добавлена по последнему курсу API",Toast.LENGTH_SHORT).show(); if(settingsDialog!=null&&settingsDialog.isShowing())settingsDialog.dismiss(); showSettings(); }
            return;
        }
        EditText input = new EditText(this); input.setSingleLine(true); input.setTextSize(18);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); input.setHint("Например, 90.5");
        new AlertDialog.Builder(this).setTitle(item.flagEmoji + "  " + item.code)
                .setMessage("Введите вручную: 1 " + repository.getBaseCode() + " = ? " + item.code)
                .setView(input).setNegativeButton("Отмена", null).setPositiveButton("Добавить", (d, w) -> {
                    try {
                        BigDecimal rate = new BigDecimal(input.getText().toString().trim().replace(',', '.'));
                        if (!repository.addCurrency(item.code, rate)) throw new NumberFormatException();
                        downloadFlagOnce(item);
                        Toast.makeText(this, "Валюта добавлена", Toast.LENGTH_SHORT).show();
                        if (settingsDialog != null && settingsDialog.isShowing()) settingsDialog.dismiss();
                        showSettings();
                    } catch (Exception e) { Toast.makeText(this, "Укажите корректный курс больше нуля", Toast.LENGTH_LONG).show(); }
                }).show();
    }

    private String intervalLabel(long m) { return m<60?m+" минут":(m%60==0?m/60+" ч":m+" мин"); }
    private void refreshRatesIfNeeded(boolean force) {
        if (!force && !repository.shouldRefresh()) { updateStatus(false); return; }
        if (statusHint != null) statusHint.setText("Обновляем курсы…");
        network.execute(() -> { RateProvider.Result result=new RateProvider().fetch(); runOnUiThread(() -> {
            if (result.anyApiSucceeded && !result.rates.isEmpty()) { repository.applyApiRates(result.rates); updateStatus(true); refreshResults(); }
            else { repository.markRefreshFailed(); updateStatus(false); }
        }); });
    }
    private void updateStatus(boolean success) {
        if(statusHint==null)return;
        if(success) statusHint.setText("Курсы обновлены · "+new java.text.SimpleDateFormat("dd.MM HH:mm",Locale.getDefault()).format(new java.util.Date(repository.getLastUpdate())));
        else if(repository.getLastUpdate()>0 && !repository.shouldRefresh()) statusHint.setText("Сохранённые курсы · последнее обновление " + new java.text.SimpleDateFormat("dd.MM HH:mm",Locale.getDefault()).format(new java.util.Date(repository.getLastUpdate())));
        else if(repository.getLastUpdate()>0) statusHint.setText("API недоступны · используем последние сохранённые курсы");
        else statusHint.setText("Нет данных API · проверьте интернет и задайте курсы вручную");
    }

    private void downloadFlagOnce(CurrencyItem item) {
        File target = new File(getFilesDir(), "flag-" + item.countryCode + ".svg");
        if (target.exists()) return;
        new Thread(() -> {
            HttpURLConnection connection = null;
            File temp = new File(getFilesDir(), "flag-" + item.countryCode + ".tmp");
            try {
                URL url = new URL("https://raw.githubusercontent.com/lipis/flag-icons/main/flags/4x3/" + item.countryCode + ".svg");
                connection = (HttpURLConnection) url.openConnection(); connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
                connection.setRequestMethod("GET"); connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return;
                try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[4096]; int n; while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                }
                if (temp.length() > 0) temp.renameTo(target);
                runOnUiThread(this::refreshResults);
            } catch (Exception ignored) { temp.delete(); }
            finally { if (connection != null) connection.disconnect(); }
        }).start();
    }

    @Override protected void onDestroy() { network.shutdownNow(); super.onDestroy(); }

    @Override protected void onStop() {
        // The entered amount is intentionally session-only and is cleared when the app is backgrounded.
        if (amountInput != null) amountInput.setText("");
        super.onStop();
    }
}
