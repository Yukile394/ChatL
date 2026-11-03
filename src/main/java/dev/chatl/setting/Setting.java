package dev.chatl.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public abstract class Setting {
    public final String name;

    protected Setting(String name) { this.name = name; }

    public boolean persistent() { return true; }
    public JsonElement toJson() { return null; }
    public void fromJson(JsonElement e) { }
    public void reset() { }

    public static class Bool extends Setting {
        private final boolean def;
        private boolean value;
        public Bool(String name, boolean def) { super(name); this.def = def; this.value = def; }
        public boolean get() { return value; }
        public void set(boolean v) { value = v; }
        public void toggle() { set(!get()); }
        @Override public JsonElement toJson() { return new JsonPrimitive(get()); }
        @Override public void fromJson(JsonElement e) {
            if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) set(e.getAsBoolean());
        }
        @Override public void reset() { set(def); }
    }

    public static final class Num extends Setting {
        private final double min, max, step, def;
        private double value;
        public Num(String name, double min, double max, double step, double def) {
            super(name); this.min = min; this.max = max; this.step = step; this.def = def; set(def);
        }
        public double get() { return value; }
        public void set(double v) {
            if (Double.isNaN(v) || Double.isInfinite(v)) return;
            v = Math.max(min, Math.min(max, v));
            if (step > 0) v = Math.round((v - min) / step) * step + min;
            value = Math.max(min, Math.min(max, Math.round(v * 10000.0) / 10000.0));
        }
        public double fraction() { return max == min ? 0 : (value - min) / (max - min); }
        public void setFraction(double f) { set(min + (max - min) * Math.max(0, Math.min(1, f))); }
        public String display() {
            return step >= 1 ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.2f", value);
        }
        @Override public JsonElement toJson() { return new JsonPrimitive(value); }
        @Override public void fromJson(JsonElement e) {
            if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) set(e.getAsDouble());
        }
        @Override public void reset() { set(def); }
    }

    public static final class Mode extends Setting {
        private final String[] options;
        private final int def;
        private int index;
        public Mode(String name, int def, String... options) {
            super(name); this.options = options; this.def = def; this.index = def;
        }
        public String get() { return options[index]; }
        public int index() { return index; }
        public void cycle(int dir) { index = Math.floorMod(index + dir, options.length); }
        @Override public JsonElement toJson() { return new JsonPrimitive(get()); }
        @Override public void fromJson(JsonElement e) {
            if (e == null || !e.isJsonPrimitive()) return;
            String s = e.getAsString();
            for (int i = 0; i < options.length; i++) if (options[i].equals(s)) { index = i; return; }
        }
        @Override public void reset() { index = def; }
    }

    /** Tus atamasi satiri (kalici degil; ChatModule kendi tusunu kaydeder). */
    public static final class Key extends Setting {
        private final IntSupplier getter;
        private final IntConsumer setter;
        public Key(String name, IntSupplier getter, IntConsumer setter) { super(name); this.getter = getter; this.setter = setter; }
        public int get() { return getter.getAsInt(); }
        public void set(int k) { setter.accept(k); }
        @Override public boolean persistent() { return false; }
    }

    public static final class Action extends Setting {
        private final Runnable action;
        public Action(String name, Runnable action) { super(name); this.action = action; }
        public void run() { action.run(); }
        @Override public boolean persistent() { return false; }
    }
}
