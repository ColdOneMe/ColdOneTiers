package com.coldonetiers;

import java.util.Locale;

/** Режимы тир-листа (в том же порядке, что и значки) и их символы в шрифте coldonetiers:tiers. */
public enum TierMode {
    SWORD("sword", "Sword", ''),
    AXE("axe", "Axe", ''),
    MACE("mace", "Mace", ''),
    UHC("uhc", "UHC", ''),
    DIAMOND_POT("diamondpot", "Diamond Pot", ''),
    NETHER_POT("netherop", "Nether Pot", ''),
    SMP("smp", "SMP", ''),
    VANILLA("vanilla", "Vanilla", ''),
    CART("cart", "Cart", ''),
    SPEAR_MACE("spearmace", "Spear Mace", '');

    public static final char FIRST_GLYPH = '';
    public static final char LAST_GLYPH = '';

    /** Ключ в данных сайта (p.tiers[id]). */
    public final String id;
    public final String label;
    public final char glyph;

    TierMode(String id, String label, char glyph) {
        this.id = id;
        this.label = label;
        this.glyph = glyph;
    }

    public static TierMode byId(String id) {
        if (id == null) return null;
        String low = id.toLowerCase(Locale.ROOT);
        for (TierMode m : values()) {
            if (m.id.equals(low)) return m;
        }
        return null;
    }
}
