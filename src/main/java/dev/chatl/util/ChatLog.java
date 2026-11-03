package dev.chatl.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Hassas hesap verisi KAYDEDILMEZ. Son 100 satir bellekte tutulur (P paneli okur). */
public final class ChatLog {
    private static final Logger LOG = Logger.getLogger("ChatL");
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Deque<String> BUF = new ArrayDeque<>();

    private ChatLog() { }

    private static synchronized void add(String level, String msg) {
        BUF.addLast(LocalTime.now().format(F) + " [" + level + "] " + msg);
        while (BUF.size() > 100) BUF.removeFirst();
    }

    public static void info(String m) { add("INFO", m); LOG.log(Level.INFO, m); }
    public static void warn(String m) { add("WARN", m); LOG.log(Level.WARNING, m); }
    public static void error(String m) { add("ERROR", m); LOG.log(Level.SEVERE, m); }

    public static synchronized List<String> recent(int n) {
        List<String> all = new ArrayList<>(BUF);
        return all.subList(Math.max(0, all.size() - n), all.size());
    }
}
