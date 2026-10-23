package com.coldonetiers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Настройки мода (файл config/coldonetiers.json). */
public final class TierConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** "highest" (лучший тир игрока) или id режима: sword, axe, mace, uhc, diamondpot, netherop, smp, vanilla, cart, spearmace. */
    public String mode = "highest";
    public boolean showNameTag = true;
    public boolean showTab = true;
    /** Показывать текст HT3/LT3 рядом со значком. */
    public boolean showTierText = true;
    /** Ссылка на сайт тир-листа (страница с PLAYERS) или на JSON с игроками. Пусто = только встроенный список. */
    public String dataUrl = "";
    public int refreshMinutes = 15;

    private transient Path file;
    private transient int version = 0;

    public int version() {
        return version;
    }

    public void touch() {
        version++;
    }

    public static TierConfig load(Path file) {
        TierConfig cfg = null;
        try {
            if (Files.isRegularFile(file)) cfg = GSON.fromJson(Files.readString(file), TierConfig.class);
        } catch (Exception ignored) {
        }
        if (cfg == null) cfg = new TierConfig();
        if (cfg.mode == null) cfg.mode = "highest";
        if (cfg.dataUrl == null) cfg.dataUrl = "";
        if (cfg.refreshMinutes < 1) cfg.refreshMinutes = 15;
        cfg.file = file;
        cfg.save();
        return cfg;
    }

    public void save() {
        touch();
        if (file == null) return;
        try {
            Files.writeString(file, GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }
}
