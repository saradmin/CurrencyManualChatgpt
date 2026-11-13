package ru.saradmin.currency.data;

import ru.saradmin.currency.model.CurrencyItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CurrencyCatalog {
    private CurrencyCatalog() { }
    public static final List<CurrencyItem> ALL = Arrays.asList(
        new CurrencyItem("USD", "Доллар США", "us", "🇺🇸"),
        new CurrencyItem("EUR", "Евро", "eu", "🇪🇺"),
        new CurrencyItem("KZT", "Казахстанский тенге", "kz", "🇰🇿"),
        new CurrencyItem("RUB", "Российский рубль", "ru", "🇷🇺"),
        new CurrencyItem("EGP", "Египетский фунт", "eg", "🇪🇬"),
        new CurrencyItem("GBP", "Фунт стерлингов", "gb", "🇬🇧"),
        new CurrencyItem("TRY", "Турецкая лира", "tr", "🇹🇷"),
        new CurrencyItem("AED", "Дирхам ОАЭ", "ae", "🇦🇪"),
        new CurrencyItem("CNY", "Китайский юань", "cn", "🇨🇳"),
        new CurrencyItem("JPY", "Японская иена", "jp", "🇯🇵"),
        new CurrencyItem("CHF", "Швейцарский франк", "ch", "🇨🇭"),
        new CurrencyItem("CAD", "Канадский доллар", "ca", "🇨🇦"),
        new CurrencyItem("AUD", "Австралийский доллар", "au", "🇦🇺"),
        new CurrencyItem("INR", "Индийская рупия", "in", "🇮🇳"),
        new CurrencyItem("PLN", "Польский злотый", "pl", "🇵🇱"),
        new CurrencyItem("CZK", "Чешская крона", "cz", "🇨🇿"),
        new CurrencyItem("SEK", "Шведская крона", "se", "🇸🇪"),
        new CurrencyItem("NOK", "Норвежская крона", "no", "🇳🇴"),
        new CurrencyItem("DKK", "Датская крона", "dk", "🇩🇰"),
        new CurrencyItem("GEL", "Грузинский лари", "ge", "🇬🇪"),
        new CurrencyItem("AMD", "Армянский драм", "am", "🇦🇲"),
        new CurrencyItem("AZN", "Азербайджанский манат", "az", "🇦🇿"),
        new CurrencyItem("UZS", "Узбекский сум", "uz", "🇺🇿"),
        new CurrencyItem("UAH", "Украинская гривна", "ua", "🇺🇦"),
        new CurrencyItem("ILS", "Новый израильский шекель", "il", "🇮🇱"),
        new CurrencyItem("THB", "Тайский бат", "th", "🇹🇭"),
        new CurrencyItem("VND", "Вьетнамский донг", "vn", "🇻🇳"),
        new CurrencyItem("BRL", "Бразильский реал", "br", "🇧🇷"),
        new CurrencyItem("MXN", "Мексиканское песо", "mx", "🇲🇽"),
        new CurrencyItem("SGD", "Сингапурский доллар", "sg", "🇸🇬"),
        new CurrencyItem("NZD", "Новозеландский доллар", "nz", "🇳🇿")
    );
    public static CurrencyItem find(String code) {
        for (CurrencyItem item : ALL) if (item.code.equals(code)) return item;
        return null;
    }
}
