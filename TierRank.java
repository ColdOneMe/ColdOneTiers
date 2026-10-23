package com.coldonetiers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Разбор строк вида HT3 / LT4 и их сравнение. */
public final class TierRank {
    private static final Pattern TIER = Pattern.compile("^(HT|LT)([1-5])$");

    private TierRank() {}

    public static boolean isValid(String tier) {
        return tier != null && TIER.matcher(tier).matches();
    }

    /** Чем меньше число, тем тир выше (HT1 = 2, LT1 = 3, HT2 = 4 ...). Невалидный тир = Integer.MAX_VALUE. */
    public static int rank(String tier) {
        if (tier == null) return Integer.MAX_VALUE;
        Matcher m = TIER.matcher(tier);
        if (!m.matches()) return Integer.MAX_VALUE;
        int n = Integer.parseInt(m.group(2));
        return n * 2 + (m.group(1).equals("HT") ? 0 : 1);
    }

    /** Цвета, как на сайте. */
    public static String color(String tier) {
        switch (tier) {
            case "HT1": case "LT1": return "#ffd76a";
            case "HT2": case "LT2": return "#c7d1d8";
            case "HT3": return "#b5691f";
            case "LT3": return "#cc7f42";
            case "HT4": case "LT4": return "#2f8bff";
            case "HT5": case "LT5": return "#a78bfa";
            default: return "#2e7da6";
        }
    }
}
