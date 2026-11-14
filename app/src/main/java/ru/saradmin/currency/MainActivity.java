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
import android.view.DragEvent;
import android.content.ClipData;
import android.os.Build;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
    private boolean settingsEditActive = false;
    private boolean rateRefreshInProgress = false;
    private int pendingPairRefreshes = 0;
    private boolean initialRefreshStarted = false;
    private boolean settingsTemporarilyHidden = false;
    private final java.util.LinkedHashMap<String, EditText> settingsRateInputs = new java.util.LinkedHashMap<>();
    private final java.util.LinkedHashMap<String, String> settingsOriginalRates = new java.util.LinkedHashMap<>();
    private final java.util.LinkedHashMap<String, CheckBox> settingsPinInputs = new java.util.LinkedHashMap<>();
    private final java.util.LinkedHashMap<String, TextView> settingsSourceBadges = new java.util.LinkedHashMap<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        repository = new CurrencyRepository(this);
        buildMainScreen();
    }

    @Override protected void onResume() { super.onResume(); if (!initialRefreshStarted) { initialRefreshStarted = true; refreshRatesIfNeeded(); } }

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
        TextView title = label("Currency Converter", 22, true);
        android.text.SpannableString brandTitle = new android.text.SpannableString("Currency Converter");
        brandTitle.setSpan(new android.text.style.ForegroundColorSpan(0xff2589E8), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        brandTitle.setSpan(new android.text.style.ForegroundColorSpan(0xffE34B55), 9, 10, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(brandTitle);
        heading.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button settings = smallButton("⚙", false); heading.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(42)));
        settings.setOnClickListener(v -> showSettings()); root.addView(heading);

        LinearLayout inputCard = column(); inputCard.setBackground(rounded(CARD, BORDER)); padding(inputCard, 14, 10, 14, 10);
        LinearLayout inputColumn = column();
        TextView caption = label("AMOUNT", 11, true); caption.setTextColor(MUTED); inputColumn.addView(caption);
        amountInput = new EditText(this); amountInput.setSingleLine(true); amountInput.setTextSize(30); amountInput.setTextColor(DARK);
        amountInput.setHint("Enter amount"); amountInput.setHintTextColor(0xffaab2ac); amountInput.setSelectAllOnFocus(false);
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

        statusHint = label("Rates are saved on this device", 11, false); statusHint.setTextColor(MUTED); statusHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(28)); root.addView(statusHint, hp);

        resultsScroll = new ScrollView(this); resultsScroll.setFillViewport(false); resultsScroll.setClipToPadding(false);
        resultsContainer = column(); resultsScroll.addView(resultsContainer);
        LinearLayout.LayoutParams rsp = new LinearLayout.LayoutParams(-1, 0, 1); rsp.topMargin = dp(2); rsp.bottomMargin = dp(8); root.addView(resultsScroll, rsp);

        root.addView(buildNumericKeyboard(), new LinearLayout.LayoutParams(-1, dp(162)));
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                fitTextToWidth(amountInput, 30, 17);
                refreshResults();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        amountInput.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitTextToWidth(amountInput, 30, 17));
        updateBaseLabel(); refreshResults();
        amountInput.requestFocus();
    }

    private LinearLayout buildNumericKeyboard() {
        // Compact keypad: the bottom row contains only decimal point, zero and backspace.
        LinearLayout keyboard = column();
        keyboard.setBackground(rounded(CARD, BORDER));
        padding(keyboard, 5, 5, 5, 5);
        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {".", "0", "⌫"}
        };
        for (String[] line : keys) {
            LinearLayout r = row();
            for (String key : line) {
                Button b = keyButton(key);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(34), 1);
                p.setMargins(dp(3), dp(2), dp(3), dp(2));
                r.addView(b, p);
                b.setOnClickListener(v -> onKey(key));
            }
            keyboard.addView(r, new LinearLayout.LayoutParams(-1, dp(38)));
        }
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
        String current = amountInput.getText().toString().replace(" ", "");
        if ("⌫".equals(key)) {
            if (!current.isEmpty()) current = current.substring(0, current.length() - 1);
        } else if (".".equals(key) || ",".equals(key)) {
            if (current.contains(".") || current.contains(",")) return;
            current = current.isEmpty() ? "0." : current + ".";
        } else {
            if (current.length() >= 15) return;
            current += key;
        }
        current = CurrencyDisplayFormat.formatInput(current);
        amountInput.setText(current); amountInput.setSelection(current.length()); amountInput.requestFocus();
    }

    private BigDecimal parseAmount() {
        String value = amountInput == null ? "" : amountInput.getText().toString().trim().replace(" ", "").replace(',', '.');
        if (value.isEmpty() || ".".equals(value)) return BigDecimal.ZERO;
        try { return new BigDecimal(value); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
    private String format(BigDecimal value) {
        return CurrencyDisplayFormat.format(value);
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
            LinearLayout details = column();
            LinearLayout codeRow = row();
            TextView code = label(item.code, 15, true);
            code.setSingleLine(true);
            codeRow.addView(code);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(19), dp(19));
            badgeParams.leftMargin = dp(5);
            codeRow.addView(sourceBadge(repository.getSource(item.code)), badgeParams);
            details.addView(codeRow);
            BigDecimal rate = repository.getRate(item.code);
            TextView rateText = label("1 " + base + " = " + CurrencyDisplayFormat.formatRate(rate), 9, false);
            rateText.setTextColor(MUTED);
            rateText.setSingleLine(true);
            rateText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            details.addView(rateText);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(dp(76), -2);
            detailsParams.leftMargin = dp(8);
            card.addView(details, detailsParams);
            BigDecimal result = amount.multiply(repository.getRate(item.code), new java.math.MathContext(16, java.math.RoundingMode.HALF_UP));
            TextView value = label(amountInput.getText().length() == 0 ? "" : format(result), 19, true);
            value.setSingleLine(true);
            value.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            value.setIncludeFontPadding(false);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0, -1, 1);
            valueParams.leftMargin = dp(3);
            card.addView(value, valueParams);
            value.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitTextToWidth(value, 19, 10));
            value.post(() -> fitTextToWidth(value, 19, 10));
            // A dedicated visible handle makes the drag gesture discoverable.
            TextView dragHandle = label("≡", 25, true);
            dragHandle.setTextColor(MUTED);
            dragHandle.setGravity(Gravity.CENTER);
            dragHandle.setContentDescription("Drag to reorder " + item.code);
            card.addView(dragHandle, new LinearLayout.LayoutParams(dp(30), dp(40)));
            card.setContentDescription(item.code + ". Use the reorder handle to move this currency.");
            dragHandle.setOnLongClickListener(v -> {
                ClipData data = ClipData.newPlainText("currency-code", item.code);
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(card);
                if (Build.VERSION.SDK_INT >= 24) card.startDragAndDrop(data, shadow, item.code, 0);
                else card.startDrag(data, shadow, item.code, 0);
                return true;
            });
            card.setOnDragListener((target, event) -> {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return true;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        card.setBackground(rounded(0xff394555, GREEN));
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                    case DragEvent.ACTION_DRAG_ENDED:
                        card.setBackground(rounded(CARD, BORDER));
                        return true;
                    case DragEvent.ACTION_DROP:
                        card.setBackground(rounded(CARD, BORDER));
                        Object local = event.getLocalState();
                        String draggedCode = local == null ? null : local.toString();
                        if (draggedCode == null && event.getClipData() != null && event.getClipData().getItemCount() > 0)
                            draggedCode = event.getClipData().getItemAt(0).getText().toString();
                        if (draggedCode != null && !draggedCode.equals(item.code)) {
                            List<String> order = new ArrayList<>();
                            for (CurrencyItem currency : repository.getCurrencies())
                                if (!currency.code.equals(repository.getBaseCode())) order.add(currency.code);
                            int from = order.indexOf(draggedCode);
                            int to = order.indexOf(item.code);
                            if (from >= 0 && to >= 0) {
                                repository.reorderCurrencies(ru.saradmin.currency.data.CurrencyOrder.move(order, from, to));
                                refreshResults();
                            }
                        }
                        return true;
                    default:
                        return true;
                }
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.bottomMargin = dp(7); resultsContainer.addView(card, cp);
        }
    }

    /** Shrinks a single-line number to the available width instead of pushing sibling views out of the card. */
    private void fitTextToWidth(TextView view, float maxSp, float minSp) {
        if (view == null || view.getWidth() <= 0) return;
        float available = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
        float size = maxSp;
        view.setTextSize(size);
        while (size > minSp && view.getPaint().measureText(view.getText().toString()) > available) {
            size -= 0.5f;
            view.setTextSize(size);
        }
        view.setEllipsize(view.getPaint().measureText(view.getText().toString()) > available
                ? android.text.TextUtils.TruncateAt.END : null);
    }

    private TextView sourceBadge(int source) {
        TextView badge = new TextView(this);
        badge.setGravity(Gravity.CENTER);
        badge.setTextSize(10);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setTextColor(Color.WHITE);
        String text = RateSourceLabel.forSource(source);
        badge.setText(text);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(source == 1 || source == 2 ? 0xff20b85a : 0xff2589e8);
        badge.setBackground(shape);
        badge.setContentDescription(source == 1 ? "Frankfurter rate" : source == 2 ? "ExchangeRate-API rate" : "Manual or cached rate");
        return badge;
    }

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
        new AlertDialog.Builder(this).setTitle("Base currency").setItems(labels, (dialog, which) -> {
            repository.setBaseCode(currencies.get(which).code);
            updateBaseLabel(); refreshResults();
        }).setNegativeButton("Cancel", null).show();
    }

    private void addLegendSource(LinearLayout parent, int source, String caption) {
        LinearLayout item = row();
        item.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(-2, -2);
        itemParams.rightMargin = dp(14);
        parent.addView(item, itemParams);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(18), dp(18));
        item.addView(sourceBadge(source), badgeParams);
        TextView text = label(caption, 11, false);
        text.setTextColor(MUTED);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(-2, -2);
        textParams.leftMargin = dp(5);
        item.addView(text, textParams);
    }

    private void showSettings() {
        settingsTemporarilyHidden = false;
        settingsRateInputs.clear();
        settingsOriginalRates.clear();
        settingsPinInputs.clear();
        settingsSourceBadges.clear();
        if (!settingsEditActive) {
            repository.beginEdit();
            settingsEditActive = true;
        }
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = column(); padding(body, 18, 16, 18, 18); scroll.addView(body);
        TextView title = label("Currencies & Rates", 22, true); body.addView(title);
        TextView description = label("Rates are edited here as a draft. Update rates fills non-fixed pairs; only Save stores changes.", 13, false);
        description.setTextColor(MUTED); LinearLayout.LayoutParams descP = new LinearLayout.LayoutParams(-1, -2); descP.topMargin = dp(8); body.addView(description, descP);

        LinearLayout sourceBlock = column();
        LinearLayout.LayoutParams sourceBlockP = new LinearLayout.LayoutParams(-1, -2); sourceBlockP.topMargin = dp(10); body.addView(sourceBlock, sourceBlockP);
        TextView sourcesLabel = label("Rate sources", 11, true); sourcesLabel.setTextColor(MUTED); sourceBlock.addView(sourcesLabel);
        LinearLayout sourceRow = row(); LinearLayout.LayoutParams srp = new LinearLayout.LayoutParams(-1, -2); srp.topMargin = dp(5); sourceBlock.addView(sourceRow, srp);
        addLegendSource(sourceRow, 1, "Frankfurter");
        addLegendSource(sourceRow, 2, "ExchangeRate-API");
        LinearLayout fallbackRow = row(); LinearLayout.LayoutParams frp = new LinearLayout.LayoutParams(-1, -2); frp.topMargin = dp(4); sourceBlock.addView(fallbackRow, frp);
        addLegendSource(fallbackRow, 0, "Manual / fallback");

        TextView autoUpdateNote = label("Rates refresh on app open, here by button, or when Keep is unchecked.", 12, false);
        autoUpdateNote.setTextColor(MUTED);
        LinearLayout.LayoutParams autoNoteP = new LinearLayout.LayoutParams(-1, -2); autoNoteP.topMargin = dp(10); body.addView(autoUpdateNote, autoNoteP);
        Button updateNow = smallButton("Update rates now", true); LinearLayout.LayoutParams unp = new LinearLayout.LayoutParams(-1, dp(42)); unp.topMargin = dp(6); body.addView(updateNow, unp);
        updateNow.setOnClickListener(v -> {
            if (rateRefreshInProgress || pendingPairRefreshes > 0 || !applySettingsRateInputs()) return;
            applySettingsPinInputs();
            updateNow.setEnabled(false);
            updateSettingsRates(updateNow);
        });

        for (CurrencyItem item : repository.getCurrencies()) {
            boolean isBase = item.code.equals(repository.getBaseCode());
            LinearLayout line = row(); line.setGravity(Gravity.CENTER_VERTICAL); padding(line, 0, 10, 0, 10);

            LinearLayout currency = column(); currency.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams currencyP = new LinearLayout.LayoutParams(dp(54), -2);
            line.addView(currency, currencyP);
            TextView flag = label(item.flagEmoji, 22, false); flag.setGravity(Gravity.CENTER); currency.addView(flag);
            TextView code = label(item.code + (isBase ? " · base" : ""), 10, true); code.setGravity(Gravity.CENTER); code.setTextColor(DARK); currency.addView(code);

            if (!isBase) {
                LinearLayout.LayoutParams badgeP = new LinearLayout.LayoutParams(dp(20), dp(20)); badgeP.leftMargin = dp(5); badgeP.rightMargin = dp(7);
                TextView pairSourceBadge = sourceBadge(repository.getSource(item.code));
                settingsSourceBadges.put(item.code, pairSourceBadge);
                line.addView(pairSourceBadge, badgeP);
                LinearLayout rateColumn = column();
                LinearLayout.LayoutParams rateColumnP = new LinearLayout.LayoutParams(0, -2, 1); rateColumnP.leftMargin = dp(2); line.addView(rateColumn, rateColumnP);
                TextView prefix = label("1 " + repository.getBaseCode() + " =", 10, false); prefix.setTextColor(MUTED); rateColumn.addView(prefix);
                EditText rate = new EditText(this); rate.setSingleLine(true); rate.setTextSize(15); rate.setTextColor(DARK); rate.setPadding(0, 0, 0, 0);
                rate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(MUTED));
                rate.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
                rate.setHint("Rate"); rate.setSelectAllOnFocus(true); rate.setText(CurrencyDisplayFormat.formatEditable(repository.getRate(item.code)));
                settingsOriginalRates.put(item.code, normalizeRateText(rate.getText().toString()));
                rateColumn.addView(rate, new LinearLayout.LayoutParams(-1, dp(34)));
                CheckBox keepRate = new CheckBox(this);
                keepRate.setText("Keep this rate");
                keepRate.setTextSize(10);
                keepRate.setTextColor(MUTED);
                keepRate.setButtonTintList(new android.content.res.ColorStateList(
                        new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                        new int[]{0xff2589e8, MUTED}));
                keepRate.setPadding(0, 0, 0, 0);
                keepRate.setChecked(repository.isPinned(item.code));
                rateColumn.addView(keepRate, new LinearLayout.LayoutParams(-1, dp(28)));
                // Keep the checkbox as part of the same Save/Cancel draft as the rate.
                // Do not persist it immediately from the widget listener.
                settingsPinInputs.put(item.code, keepRate);
                settingsRateInputs.put(item.code, rate);
                final boolean[] previousKeepState = {keepRate.isChecked()};
                final boolean[] revertingKeep = {false};
                keepRate.setOnCheckedChangeListener((button, checked) -> {
                    if (revertingKeep[0]) { previousKeepState[0] = checked; return; }
                    boolean wasChecked = previousKeepState[0];
                    previousKeepState[0] = checked;
                    if (!wasChecked && checked) {
                        if (!applySettingsRateInputs() || repository.getSource(item.code) != 0) {
                            revertingKeep[0] = true; keepRate.setChecked(false); revertingKeep[0] = false;
                            Toast.makeText(this, "Keep is available only for a manually edited rate", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        repository.setPinned(item.code, true);
                    }
                    if (wasChecked && !checked) {
                        if (!applySettingsRateInputs()) {
                            revertingKeep[0] = true; keepRate.setChecked(true); revertingKeep[0] = false;
                            return;
                        }
                        repository.setPinned(item.code, false); // draft only while the editor is open
                        final String pairBase = repository.getBaseCode();
                        final String pairTarget = item.code;
                        keepRate.setEnabled(false);
                        pendingPairRefreshes++;
                        if (settingsDialog != null && settingsDialog.isShowing())
                            settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        network.execute(() -> {
                            RateProvider.Rate fetched = new RateProvider().fetchPair(pairBase, pairTarget);
                            runOnUiThread(() -> {
                                if (isFinishing() || isDestroyed()) return;
                                pendingPairRefreshes = Math.max(0, pendingPairRefreshes - 1);
                                keepRate.setEnabled(true);
                                if (settingsDialog != null && settingsDialog.isShowing() && pendingPairRefreshes == 0 && !rateRefreshInProgress)
                                    settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                if (!settingsEditActive || settingsDialog == null || !settingsDialog.isShowing()) return;
                                if (fetched == null) {
                                    Toast.makeText(this, "API unavailable. The last manual rate is kept.", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                repository.applyApiPairToDraft(pairBase, pairTarget, fetched);
                                rate.setText(CurrencyDisplayFormat.formatEditable(fetched.value));
                                pairSourceBadge.setText(RateSourceLabel.forSource(fetched.source));
                                GradientDrawable badgeShape = new GradientDrawable(); badgeShape.setShape(GradientDrawable.OVAL);
                                badgeShape.setColor(0xff20b85a); pairSourceBadge.setBackground(badgeShape);
                                pairSourceBadge.setContentDescription(fetched.source == 1 ? "Frankfurter rate" : "ExchangeRate-API rate");
                                settingsOriginalRates.put(item.code, normalizeRateText(rate.getText().toString()));
                                Toast.makeText(this, fetched.source == 1 ? "Frankfurter rate loaded" : "ExchangeRate-API rate loaded", Toast.LENGTH_SHORT).show();
                            });
                        });
                    }
                });

                TextView remove = label("×", 27, false); remove.setTextColor(0xffE34B55); remove.setGravity(Gravity.CENTER);
                GradientDrawable removeBg = new GradientDrawable(); removeBg.setShape(GradientDrawable.OVAL); removeBg.setColor(0x22E34B55); remove.setBackground(removeBg);
                LinearLayout.LayoutParams removeP = new LinearLayout.LayoutParams(dp(34), dp(34)); removeP.leftMargin = dp(5); line.addView(remove, removeP);
                remove.setContentDescription("Remove " + item.code);
                remove.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("Remove currency " + item.code + "?").setNegativeButton("Cancel", null).setPositiveButton("Remove", (d,w) -> {
                    repository.removeCurrency(item.code); settingsTemporarilyHidden = true; settingsDialog.dismiss(); showSettings();
                }).show());
            }
            body.addView(line);
            View divider = new View(this); divider.setBackgroundColor(0xff777777); body.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }

        Button add = smallButton("＋  Add currency", true); LinearLayout.LayoutParams addP = new LinearLayout.LayoutParams(-1, dp(48)); addP.topMargin = dp(14); body.addView(add, addP);
        add.setEnabled(repository.getCurrencies().size() < repository.getMaxCurrencies()); add.setOnClickListener(v -> showAddCurrency());
        TextView count = label("Currencies added: " + repository.getCurrencies().size() + " of " + repository.getMaxCurrencies(), 12, false); count.setTextColor(MUTED); count.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams countP = new LinearLayout.LayoutParams(-1, -2); countP.topMargin = dp(10); body.addView(count, countP);
        TextView version = label("Currency Converter  v" + getAppVersionName(), 11, false);
        version.setTextColor(MUTED); version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams versionP = new LinearLayout.LayoutParams(-1, -2); versionP.topMargin = dp(18); body.addView(version, versionP);
        version.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("About").setMessage("Currency Converter\nVersion: " + getAppVersionName() + "\nPackage: " + getPackageName()).setPositiveButton("Close", null).show());

        settingsDialog = new AlertDialog.Builder(this).setView(scroll)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (d,w) -> {
                    repository.cancelEdit();
                    settingsEditActive = false;
                    updateBaseLabel();
                    refreshResults();
                })
                .create();
        settingsDialog.setOnShowListener(d -> {
            settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(pendingPairRefreshes == 0 && !rateRefreshInProgress);
            settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Commit rate fields and Keep checkboxes together from the visible draft.
            // No widget writes directly to persistent storage while this dialog is open.
            if (!applySettingsRateInputs()) return;
            applySettingsPinInputs();
            repository.commitEdit();
            settingsEditActive = false;
            updateBaseLabel();
            refreshResults();
            settingsDialog.dismiss();
        });
        });
        settingsDialog.setOnCancelListener(d -> {
            repository.cancelEdit();
            settingsEditActive = false;
            updateBaseLabel();
            refreshResults();
        });
        settingsDialog.setOnDismissListener(d -> {
            if (settingsEditActive && !settingsTemporarilyHidden) {
                repository.cancelEdit();
                settingsEditActive = false;
                updateBaseLabel();
                refreshResults();
            }
        });
        settingsDialog.show();
    }

    private String getAppVersionName() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception e) { return "unknown"; }
    }


    private boolean applySettingsRateInputs() {
        for (Map.Entry<String, EditText> entry : settingsRateInputs.entrySet()) {
            if (!saveRate(entry.getKey(), entry.getValue().getText().toString(), false)) {
                entry.getValue().requestFocus();
                Toast.makeText(this, "Rate must be a number greater than zero", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void applySettingsPinInputs() {
        for (Map.Entry<String, CheckBox> entry : settingsPinInputs.entrySet()) {
            repository.setPinned(entry.getKey(), entry.getValue().isChecked());
        }
    }

    private String normalizeRateText(String text) {
        return text == null ? "" : text.trim().replace("\u00a0", "").replace(" ", "").replace(',', '.');
    }

    private boolean saveRate(String code, String text, boolean showError) {
        String normalized = normalizeRateText(text);
        // The displayed rate is rounded for readability. Do not write it back unless
        // the user actually edited this field, otherwise API precision/source is lost.
        if (normalized.equals(settingsOriginalRates.get(code))) return true;
        try {
            BigDecimal rate = new BigDecimal(normalized);
            if (rate.signum() <= 0) throw new NumberFormatException();
            repository.setRateFromBase(code, rate);
            settingsOriginalRates.put(code, normalized);
            return true;
        } catch (Exception e) {
            if (showError) Toast.makeText(this, "Rate must be a number greater than zero", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void showAddCurrency() {
        if (repository.getCurrencies().size() >= repository.getMaxCurrencies()) return;
        LinearLayout content = column(); padding(content, 18, 8, 18, 8);
        EditText search = new EditText(this); search.setSingleLine(true); search.setHint("Search currencies by code or name"); content.addView(search);
        ListView list = new ListView(this); content.addView(list, new LinearLayout.LayoutParams(-1, dp(300)));
        ArrayList<CurrencyItem> available = new ArrayList<>();
        for (CurrencyItem item : CurrencyCatalog.ALL) {
            boolean exists = false; for (CurrencyItem current : repository.getCurrencies()) if (current.code.equals(item.code)) exists = true;
            if (!exists) available.add(item);
        }
        ArrayList<CurrencyItem> shown = new ArrayList<>(available);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayCurrencies(shown)); list.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Add currency").setView(content).setNegativeButton("Cancel", null).create();
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
        // Adding a currency is deliberately offline-first: never use cached factors
        // or label an old value as a fresh API quote. All its pairs start at zero.
        if (repository.addCurrencyOffline(item.code)) {
            downloadFlagOnce(item);
            Toast.makeText(this, "Currency added with zero rates. Update rates when online.", Toast.LENGTH_LONG).show();
            if (settingsDialog != null && settingsDialog.isShowing()) {
                // Rebuild the settings screen only after the previous AlertDialog has
                // completed dismissal. Opening the replacement immediately can leave
                // the old window owning focus and the new draft view out of sync.
                AlertDialog previousDialog = settingsDialog;
                settingsTemporarilyHidden = true;
                previousDialog.setOnDismissListener(d -> {
                    settingsDialog = null;
                    showSettings();
                });
                previousDialog.dismiss();
            } else {
                showSettings();
            }
        } else {
            Toast.makeText(this, "Unable to add currency", Toast.LENGTH_LONG).show();
        }
    }

    /** Refreshes only non-fixed pairs into the active settings draft. Save is the only commit. */
    private void updateSettingsRates(Button updateButton) {
        if (!settingsEditActive || rateRefreshInProgress) return;
        rateRefreshInProgress = true;
        if (settingsDialog != null && settingsDialog.isShowing())
            settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        network.execute(() -> {
            RateProvider.Result result = new RateProvider().fetch(repository.getCurrencyCodes());
            runOnUiThread(() -> {
                rateRefreshInProgress = false;
                if (isFinishing() || isDestroyed()) return;
                if (!settingsEditActive || settingsDialog == null || !settingsDialog.isShowing()) return;
                if (result.anyApiSucceeded && !result.rates.isEmpty()) {
                    repository.applyApiRates(result.rates);
                    for (Map.Entry<String, EditText> entry : settingsRateInputs.entrySet()) {
                        String target = entry.getKey();
                        if (repository.isPinned(target)) continue;
                        BigDecimal value = repository.getRate(target);
                        if (value == null || value.signum() <= 0) continue;
                        EditText field = entry.getValue();
                        String text = CurrencyDisplayFormat.formatEditable(value);
                        field.setText(text);
                        settingsOriginalRates.put(target, normalizeRateText(text));
                    }
                    // Refresh source badges after applying the draft rates.
                    refreshSettingsSourceBadges();
                    Toast.makeText(this, "Rates updated in settings. Press Save to apply.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Rate update failed. Current settings values are unchanged.", Toast.LENGTH_LONG).show();
                }
                if (updateButton != null) updateButton.setEnabled(true);
                if (settingsDialog != null && settingsDialog.isShowing() && pendingPairRefreshes == 0)
                    settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            });
        });
    }

    private void refreshSettingsSourceBadges() {
        for (Map.Entry<String, TextView> entry : settingsSourceBadges.entrySet()) {
            int source = repository.getSource(entry.getKey());
            TextView badge = entry.getValue();
            badge.setText(RateSourceLabel.forSource(source));
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(source == 0 ? 0xff2589e8 : 0xff20b85a);
            badge.setBackground(shape);
            badge.setContentDescription(source == 0 ? "Manual rate" : (source == 1 ? "Frankfurter rate" : "ExchangeRate-API rate"));
        }
    }

    private void startManualRateRefresh() {
        if (rateRefreshInProgress) return;
        rateRefreshInProgress = true;
        if (statusHint != null) statusHint.setText("Updating rates…");
        network.execute(() -> {
            RateProvider.Result result = new RateProvider().fetch(repository.getCurrencyCodes());
            runOnUiThread(() -> {
                rateRefreshInProgress = false;
                if (isFinishing() || isDestroyed()) return;
                if (result.anyApiSucceeded && !result.rates.isEmpty()) {
                    repository.applyApiRates(result.rates); updateStatus(true); refreshResults();
                    Toast.makeText(this, "Rates updated", Toast.LENGTH_SHORT).show();
                } else {
                    updateStatus(false); Toast.makeText(this, "Rate update failed. Saved rates are kept.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void refreshRatesIfNeeded() {
        if (settingsEditActive || rateRefreshInProgress) return;
        if (statusHint != null) statusHint.setText("Updating rates…");
        network.execute(() -> { RateProvider.Result result=new RateProvider().fetch(repository.getCurrencyCodes()); runOnUiThread(() -> {
            if (result.anyApiSucceeded && !result.rates.isEmpty()) { repository.applyApiRates(result.rates); updateStatus(true); refreshResults(); }
            else { repository.markRefreshFailed(); updateStatus(false); }
        }); });
    }
    private void updateStatus(boolean success) {
        if(statusHint==null)return;
        if(success) statusHint.setText("Rates updated · "+new java.text.SimpleDateFormat("MMM d, HH:mm",Locale.US).format(new java.util.Date(repository.getLastUpdate())));
        else if(repository.getLastUpdate()>0) statusHint.setText("Saved rates · last updated " + new java.text.SimpleDateFormat("MMM d, HH:mm",Locale.US).format(new java.util.Date(repository.getLastUpdate())) + " · API unavailable");
        else statusHint.setText("No API data · check your connection or enter rates manually");
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
