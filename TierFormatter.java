package com.coldonetiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.HashMap;
import java.util.Map;

/** Собирает "[значок] HT3 | Ник" и подставляет его в ник над головой и в табе. */
public final class TierFormatter {
    private static final String FONT = "coldonetiers:tiers";
    private static final Component NONE = Component.empty();

    // Кэши живут, пока не поменялись данные или настройки (иначе компонент бы собирался каждый кадр).
    private static final Map<String, Component> PREFIX_CACHE = new HashMap<>();
    private static final Map<String, Component> LOOKUP_CACHE = new HashMap<>();
    private static int cacheDataVersion = -1;
    private static int cacheConfigVersion = -1;

    private TierFormatter() {}

    public static Component decorate(Component original, boolean forTab) {
        TierConfig cfg = ColdOneTiersClient.config();
        if (cfg == null || original == null) return original;
        if (forTab ? !cfg.showTab : !cfg.showNameTag) return original;

        String text = original.getString();
        if (hasGlyph(text)) return original; // уже оформлено (защита от двойной вставки)

        validateCaches(cfg);
        String key = (forTab ? "t|" : "n|") + text;
        Component prefix = LOOKUP_CACHE.get(key);
        if (prefix == null) {
            prefix = resolvePrefix(text, cfg);
            if (LOOKUP_CACHE.size() > 1024) LOOKUP_CACHE.clear();
            LOOKUP_CACHE.put(key, prefix);
        }
        if (prefix == NONE) return original;
        return prefix.copy().append(original);
    }

    private static void validateCaches(TierConfig cfg) {
        int dv = ColdOneTiersClient.database().version();
        int cv = cfg.version();
        if (dv != cacheDataVersion || cv != cacheConfigVersion) {
            PREFIX_CACHE.clear();
            LOOKUP_CACHE.clear();
            cacheDataVersion = dv;
            cacheConfigVersion = cv;
        }
    }

    private static Component resolvePrefix(String text, TierConfig cfg) {
        TierDatabase.Entry entry = ColdOneTiersClient.database().findIn(text);
        TierDatabase.Pick pick = TierDatabase.pick(entry, cfg.mode);
        if (pick == null) return NONE;
        String key = pick.mode().id + "|" + pick.tier() + "|" + cfg.showTierText;
        Component c = PREFIX_CACHE.get(key);
        if (c == null) {
            c = build(pick, cfg.showTierText);
            PREFIX_CACHE.put(key, c);
        }
        return c;
    }

    private static boolean hasGlyph(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= TierMode.FIRST_GLYPH && c <= TierMode.LAST_GLYPH) return true;
        }
        return false;
    }

    private static Component build(TierDatabase.Pick pick, boolean showText) {
        JsonArray extra = new JsonArray();

        JsonObject icon = new JsonObject();
        icon.addProperty("text", String.valueOf(pick.mode().glyph));
        icon.addProperty("font", FONT);
        icon.addProperty("color", "white");
        extra.add(icon);

        if (showText) {
            JsonObject t = new JsonObject();
            t.addProperty("text", " " + pick.tier());
            t.addProperty("color", TierRank.color(pick.tier()));
            extra.add(t);
        }

        JsonObject sep = new JsonObject();
        sep.addProperty("text", showText ? " | " : " ");
        sep.addProperty("color", "dark_gray");
        extra.add(sep);

        JsonObject root = new JsonObject();
        root.addProperty("text", "");
        root.add("extra", extra);

        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, root)
                .result()
                .orElseGet(() -> Component.literal(pick.mode().glyph + " " + pick.tier() + " | "));
    }
}
