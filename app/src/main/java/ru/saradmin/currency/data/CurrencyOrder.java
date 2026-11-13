package ru.saradmin.currency.data;

import java.util.ArrayList;
import java.util.List;

/** Pure ordering helper shared by drag-and-drop UI and unit tests. */
public final class CurrencyOrder {
    private CurrencyOrder() {}
    public static List<String> move(List<String> current, int from, int to) {
        if (current == null || from < 0 || to < 0 || from >= current.size() || to >= current.size())
            throw new IllegalArgumentException("Invalid move positions");
        List<String> result = new ArrayList<>(current);
        String item = result.remove(from);
        result.add(to, item);
        return result;
    }
}
