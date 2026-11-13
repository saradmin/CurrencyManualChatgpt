package ru.saradmin.currency.model;

public final class CurrencyItem {
    public final String code;
    public final String name;
    public final String countryCode;
    public final String flagEmoji;

    public CurrencyItem(String code, String name, String countryCode, String flagEmoji) {
        this.code = code;
        this.name = name;
        this.countryCode = countryCode;
        this.flagEmoji = flagEmoji;
    }
}
