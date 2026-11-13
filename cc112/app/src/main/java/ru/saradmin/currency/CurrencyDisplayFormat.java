package ru.saradmin.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Consistent English number formatting with spaces as thousands separators. */
public final class CurrencyDisplayFormat {
    private CurrencyDisplayFormat() {}

    public static String format(BigDecimal value) {
        return formatWithPattern(value, "#,##0.00");
    }

    /** Formats an exchange rate to four decimals, never displaying a positive rate as zero. */
    public static String formatRate(BigDecimal value) {
        BigDecimal rounded = value.setScale(4, RoundingMode.HALF_UP);
        if (value.signum() > 0 && rounded.signum() == 0) {
            rounded = new BigDecimal("0.0001");
        }
        return formatWithPattern(rounded, "#,##0.0000");
    }

    private static String formatWithPattern(BigDecimal value, String pattern) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat(pattern, symbols);
        format.setGroupingUsed(true);
        format.setGroupingSize(3);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    /** Groups the integer part without changing the entered fractional digits. */
    public static String formatEditable(BigDecimal value) {
        return groupInteger(value.stripTrailingZeros().toPlainString());
    }

    /** Groups an amount being typed, preserving a trailing decimal point. */
    public static String formatInput(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String value = raw.replace(',', '.').replace(" ", "");
        int dot = value.indexOf('.');
        String integer = dot >= 0 ? value.substring(0, dot) : value;
        String fraction = dot >= 0 ? value.substring(dot) : "";
        if (integer.isEmpty()) integer = "0";
        return groupInteger(integer) + fraction;
    }

    private static String groupInteger(String number) {
        boolean negative = number.startsWith("-");
        String digits = negative ? number.substring(1) : number;
        int dot = digits.indexOf('.');
        String integer = dot >= 0 ? digits.substring(0, dot) : digits;
        String fraction = dot >= 0 ? digits.substring(dot) : "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < integer.length(); i++) {
            if (i > 0 && (integer.length() - i) % 3 == 0) result.append(' ');
            result.append(integer.charAt(i));
        }
        return (negative ? "-" : "") + result + fraction;
    }
}
