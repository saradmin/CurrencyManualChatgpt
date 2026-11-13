package ru.saradmin.currency.data;

import ru.saradmin.currency.model.CurrencyItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CurrencyCatalog {
    private CurrencyCatalog() { }
    public static final List<CurrencyItem> ALL = Arrays.asList(
        new CurrencyItem("USD", "US Dollar", "us", "🇺🇸"),
        new CurrencyItem("EUR", "Euro", "eu", "🇪🇺"),
        new CurrencyItem("KZT", "Kazakhstani Tenge", "kz", "🇰🇿"),
        new CurrencyItem("RUB", "Russian Ruble", "ru", "🇷🇺"),
        new CurrencyItem("EGP", "Egyptian Pound", "eg", "🇪🇬"),
        new CurrencyItem("GBP", "British Pound", "gb", "🇬🇧"),
        new CurrencyItem("TRY", "Turkish Lira", "tr", "🇹🇷"),
        new CurrencyItem("AED", "UAE Dirham", "ae", "🇦🇪"),
        new CurrencyItem("CNY", "Chinese Yuan", "cn", "🇨🇳"),
        new CurrencyItem("JPY", "Japanese Yen", "jp", "🇯🇵"),
        new CurrencyItem("CHF", "Swiss Franc", "ch", "🇨🇭"),
        new CurrencyItem("CAD", "Canadian Dollar", "ca", "🇨🇦"),
        new CurrencyItem("AUD", "Australian Dollar", "au", "🇦🇺"),
        new CurrencyItem("INR", "Indian Rupee", "in", "🇮🇳"),
        new CurrencyItem("PLN", "Polish Zloty", "pl", "🇵🇱"),
        new CurrencyItem("CZK", "Czech Koruna", "cz", "🇨🇿"),
        new CurrencyItem("SEK", "Swedish Krona", "se", "🇸🇪"),
        new CurrencyItem("NOK", "Norwegian Krone", "no", "🇳🇴"),
        new CurrencyItem("DKK", "Danish Krone", "dk", "🇩🇰"),
        new CurrencyItem("GEL", "Georgian Lari", "ge", "🇬🇪"),
        new CurrencyItem("AMD", "Armenian Dram", "am", "🇦🇲"),
        new CurrencyItem("AZN", "Azerbaijani Manat", "az", "🇦🇿"),
        new CurrencyItem("UZS", "Uzbekistani Som", "uz", "🇺🇿"),
        new CurrencyItem("UAH", "Ukrainian Hryvnia", "ua", "🇺🇦"),
        new CurrencyItem("ILS", "Israeli New Shekel", "il", "🇮🇱"),
        new CurrencyItem("THB", "Thai Baht", "th", "🇹🇭"),
        new CurrencyItem("VND", "Vietnamese Dong", "vn", "🇻🇳"),
        new CurrencyItem("BRL", "Brazilian Real", "br", "🇧🇷"),
        new CurrencyItem("MXN", "Mexican Peso", "mx", "🇲🇽"),
        new CurrencyItem("SGD", "Singapore Dollar", "sg", "🇸🇬"),
        new CurrencyItem("NZD", "New Zealand Dollar", "nz", "🇳🇿")
    );
    public static CurrencyItem find(String code) {
        for (CurrencyItem item : ALL) if (item.code.equals(code)) return item;
        return null;
    }
}
