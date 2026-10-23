package com.coldonetiers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Игроки сайта и их тиры. Только игроки из этой базы получают значок. */
public final class TierDatabase {
    /** Игрок сайта: имя и лучший тир в каждом режиме (modeId -> "HT3"). */
    public record Entry(String name, Map<String, String> best) {}

    /** Выбранный для показа тир. */
    public record Pick(TierMode mode, String tier) {}

    private static final Pattern PLAYERS_DECL = Pattern.compile("\\b(?:const|let|var)\\s+PLAYERS\\s*=\\s*");
    private static final int MAX_BYTES = 12 * 1024 * 1024;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private volatile Map<String, Entry> players = Map.of();
    private volatile int version = 0;

    public int version() {
        return version;
    }

    public int size() {
        return players.size();
    }

    public void set(Map<String, Entry> map) {
        this.players = map;
        this.version++;
    }

    // ---------------------------------------------------------------- поиск

    /** Ищет в строке (ник, возможно с префиксом команды) игрока сайта. */
    public Entry findIn(String text) {
        Map<String, Entry> map = players;
        if (map.isEmpty() || text == null) return null;
        int n = text.length();
        int i = 0;
        while (i < n) {
            if (isNameChar(text.charAt(i))) {
                int st = i;
                while (i < n && isNameChar(text.charAt(i))) i++;
                if (i - st >= 3) {
                    Entry e = map.get(text.substring(st, i).toLowerCase(Locale.ROOT));
                    if (e != null) return e;
                }
            } else {
                i++;
            }
        }
        return null;
    }

    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    /** modeId == "highest" -> лучший тир среди всех режимов; иначе тир указанного режима (или null). */
    public static Pick pick(Entry e, String modeId) {
        if (e == null) return null;
        TierMode single = TierMode.byId(modeId);
        if (single != null) {
            String t = e.best().get(single.id);
            return t == null ? null : new Pick(single, t);
        }
        Pick best = null;
        int bestRank = Integer.MAX_VALUE;
        for (TierMode m : TierMode.values()) {
            String t = e.best().get(m.id);
            int r = TierRank.rank(t);
            if (r < bestRank) {
                bestRank = r;
                best = new Pick(m, t);
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- разбор

    /** Принимает JSON ([...] или {"players":[...]}) либо HTML страницы сайта с массивом PLAYERS. */
    public static Map<String, Entry> parse(String body) throws IOException {
        JsonElement root;
        int p = 0;
        while (p < body.length() && (Character.isWhitespace(body.charAt(p)) || body.charAt(p) == '﻿')) p++;
        char first = p < body.length() ? body.charAt(p) : ' ';
        if (first == '[' || first == '{') {
            try {
                root = JsonParser.parseString(body);
            } catch (RuntimeException ex) {
                root = RelaxedJson.parse(body, p);
            }
        } else {
            Matcher m = PLAYERS_DECL.matcher(body);
            if (!m.find()) throw new IOException("В ответе не найден список игроков (PLAYERS)");
            root = RelaxedJson.parse(body, m.end());
        }
        JsonArray arr;
        if (root.isJsonArray()) arr = root.getAsJsonArray();
        else if (root.isJsonObject() && root.getAsJsonObject().has("players")) arr = root.getAsJsonObject().getAsJsonArray("players");
        else throw new IOException("Неизвестный формат данных");

        Map<String, Entry> out = new HashMap<>();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            if (!o.has("name") || !o.get("name").isJsonPrimitive()) continue;
            String name = o.get("name").getAsString().trim();
            if (name.isEmpty()) continue;
            Map<String, String> best = new LinkedHashMap<>();
            if (o.has("tiers") && o.get("tiers").isJsonObject()) {
                for (Map.Entry<String, JsonElement> te : o.getAsJsonObject("tiers").entrySet()) {
                    String t = bestOf(te.getValue());
                    if (t != null) best.put(te.getKey().toLowerCase(Locale.ROOT), t);
                }
            }
            out.put(name.toLowerCase(Locale.ROOT), new Entry(name, best));
        }
        return out;
    }

    private static String bestOf(JsonElement v) {
        String best = null;
        if (v.isJsonArray()) {
            for (JsonElement x : v.getAsJsonArray()) {
                if (x.isJsonPrimitive()) best = better(best, x.getAsString());
            }
        } else if (v.isJsonPrimitive()) {
            best = better(null, v.getAsString());
        }
        return best;
    }

    private static String better(String cur, String cand) {
        if (!TierRank.isValid(cand)) return cur;
        if (cur == null || TierRank.rank(cand) < TierRank.rank(cur)) return cand;
        return cur;
    }

    // ---------------------------------------------------------------- загрузка

    public void loadBundled() {
        try (InputStream in = TierDatabase.class.getResourceAsStream("/assets/coldonetiers/players.json")) {
            if (in != null) set(parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
        }
    }

    public void loadCache(Path file) {
        try {
            if (Files.isRegularFile(file)) set(parse(Files.readString(file)));
        } catch (Exception ignored) {
        }
    }

    /** Скачивает данные (JSON или страницу сайта) и применяет их. Кидает исключение при ошибке. */
    public int fetch(String url, Path cacheFile) throws IOException, InterruptedException {
        URI uri = URI.create(url.trim());
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
            throw new IOException("Ссылка должна начинаться с http:// или https://");
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(25))
                .header("User-Agent", "ColdOneTiers-Mod")
                .GET()
                .build();
        HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() / 100 != 2) throw new IOException("Сервер ответил " + resp.statusCode());
        if (resp.body().length > MAX_BYTES) throw new IOException("Ответ слишком большой");
        Map<String, Entry> parsed = parse(new String(resp.body(), StandardCharsets.UTF_8));
        if (parsed.isEmpty()) throw new IOException("Список игроков пуст");
        set(parsed);
        try {
            Files.writeString(cacheFile, toJson(parsed));
        } catch (IOException ignored) {
        }
        return parsed.size();
    }

    private static String toJson(Map<String, Entry> map) {
        JsonArray arr = new JsonArray();
        for (Entry e : map.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", e.name());
            JsonObject tiers = new JsonObject();
            for (Map.Entry<String, String> t : e.best().entrySet()) {
                JsonArray one = new JsonArray();
                one.add(t.getValue());
                tiers.add(t.getKey(), one);
            }
            o.add("tiers", tiers);
            arr.add(o);
        }
        return GSON.toJson(arr);
    }
}
