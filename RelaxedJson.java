package com.coldonetiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;

/**
 * Мини-парсер литералов JavaScript ({ name:'x', tiers:{ sword:['HT3'] }, /* комментарии *&#47; }).
 * Нужен, чтобы читать массив PLAYERS прямо из HTML страницы сайта.
 */
final class RelaxedJson {
    private final String s;
    private int i;

    private RelaxedJson(String s, int start) {
        this.s = s;
        this.i = start;
    }

    static JsonElement parse(String text, int start) throws IOException {
        RelaxedJson p = new RelaxedJson(text, start);
        return p.value();
    }

    private IOException err(String msg) {
        return new IOException("Ошибка разбора данных (позиция " + i + "): " + msg);
    }

    private void skip() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c) || c == '﻿') {
                i++;
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/') {
                while (i < s.length() && s.charAt(i) != '\n') i++;
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else {
                break;
            }
        }
    }

    private JsonElement value() throws IOException {
        skip();
        if (i >= s.length()) throw err("неожиданный конец данных");
        char c = s.charAt(i);
        if (c == '{') return object();
        if (c == '[') return array();
        if (c == '\'' || c == '"' || c == '`') return new JsonPrimitive(string());
        if (c == '-' || (c >= '0' && c <= '9')) return number();
        String id = ident();
        switch (id) {
            case "true": return new JsonPrimitive(true);
            case "false": return new JsonPrimitive(false);
            case "null": case "undefined": return JsonNull.INSTANCE;
            default: throw err("неподдерживаемое значение '" + id + "'");
        }
    }

    private JsonObject object() throws IOException {
        JsonObject o = new JsonObject();
        i++; // {
        while (true) {
            skip();
            if (i >= s.length()) throw err("не закрыт объект");
            if (s.charAt(i) == '}') { i++; return o; }
            char c = s.charAt(i);
            String key;
            if (c == '\'' || c == '"' || c == '`') key = string();
            else key = ident();
            skip();
            if (i >= s.length() || s.charAt(i) != ':') throw err("ожидалось ':' после ключа " + key);
            i++;
            o.add(key, value());
            skip();
            if (i < s.length() && s.charAt(i) == ',') i++;
        }
    }

    private JsonArray array() throws IOException {
        JsonArray a = new JsonArray();
        i++; // [
        while (true) {
            skip();
            if (i >= s.length()) throw err("не закрыт массив");
            if (s.charAt(i) == ']') { i++; return a; }
            a.add(value());
            skip();
            if (i < s.length() && s.charAt(i) == ',') i++;
        }
    }

    private String ident() throws IOException {
        int st = i;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '$') i++;
            else break;
        }
        if (st == i) throw err("неожиданный символ '" + s.charAt(i) + "'");
        return s.substring(st, i);
    }

    private JsonElement number() throws IOException {
        int st = i;
        if (s.charAt(i) == '-') i++;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || ".eE+-".indexOf(s.charAt(i)) >= 0)) i++;
        try {
            return new JsonPrimitive(Double.parseDouble(s.substring(st, i)));
        } catch (NumberFormatException e) {
            throw err("плохое число");
        }
    }

    private String string() throws IOException {
        char q = s.charAt(i++);
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (i >= s.length()) throw err("не закрыта строка");
            char c = s.charAt(i++);
            if (c == q) return sb.toString();
            if (q == '`' && c == '$' && i < s.length() && s.charAt(i) == '{') throw err("шаблонные выражения не поддерживаются");
            if (c != '\\') { sb.append(c); continue; }
            if (i >= s.length()) throw err("обрыв экранирования");
            char e = s.charAt(i++);
            switch (e) {
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'r': sb.append('\r'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case '0': sb.append('\0'); break;
                case 'u':
                    if (i + 4 > s.length()) throw err("плохой \\u");
                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                    break;
                case '\n': break; // продолжение строки
                default: sb.append(e);
            }
        }
    }
}
