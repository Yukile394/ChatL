package dev.chatl.service;

import dev.chatl.module.ChatModule;
import dev.chatl.setting.Setting;
import java.util.Locale;

/** Yalnizca modul adi ve o modulun ayar adlari aranir; buyuk-kucuk harf duyarsiz. */
public final class Search {
    private Search() { }

    private static boolean has(String hay, String q) {
        return hay.toLowerCase(Locale.ROOT).contains(q.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean matches(ChatModule m, String q) {
        if (q == null || q.isBlank()) return true;
        if (has(m.name, q)) return true;
        return matchedSetting(m, q) != null;
    }

    /** Modul adi eslesmiyorsa, eslesen ilk ayarin adini dondurur. */
    public static String matchedSetting(ChatModule m, String q) {
        if (q == null || q.isBlank() || has(m.name, q)) return null;
        for (Setting s : m.settings()) if (has(s.name, q)) return s.name;
        return null;
    }
}
