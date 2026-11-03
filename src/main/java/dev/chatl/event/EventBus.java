package dev.chatl.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Tek ortak olay sistemi. subscribe() geri donen Runnable ile aboneligi temizler. */
public final class EventBus {
    private static final Map<Class<?>, List<Consumer<Object>>> LISTENERS = new ConcurrentHashMap<>();

    private EventBus() { }

    public static <T> Runnable subscribe(Class<T> type, Consumer<T> handler) {
        Consumer<Object> wrapper = o -> handler.accept(type.cast(o));
        LISTENERS.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(wrapper);
        return () -> {
            List<Consumer<Object>> l = LISTENERS.get(type);
            if (l != null) l.remove(wrapper);
        };
    }

    public static void post(Object event) {
        List<Consumer<Object>> l = LISTENERS.get(event.getClass());
        if (l == null) return;
        for (Consumer<Object> c : l) {
            try { c.accept(event); } catch (RuntimeException ex) { dev.chatl.util.ChatLog.error("Olay isleyici hatasi: " + ex); }
        }
    }

    public static int listenerCount(Class<?> type) {
        List<Consumer<Object>> l = LISTENERS.get(type);
        return l == null ? 0 : l.size();
    }
}
