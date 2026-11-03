package dev.chatl.module;

import com.google.gson.JsonObject;
import dev.chatl.event.EventBus;
import dev.chatl.event.Events;
import dev.chatl.setting.Setting;
import dev.chatl.setting.SettingIO;
import dev.chatl.util.ChatLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ChatModule {
    public enum Category {
        COMBAT("Combat"), MOVEMENT("Movement");
        public final String label;
        Category(String label) { this.label = label; }
    }

    public final String name;
    public final String description;
    public final Category category;
    public boolean keyDown;
    private boolean enabled;
    private int key = -1;
    private final List<Setting> settings = new ArrayList<>();

    protected ChatModule(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
    }

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }

    public List<Setting> settings() { return Collections.unmodifiableList(settings); }
    public boolean isEnabled() { return enabled; }
    public int key() { return key; }
    public void setKey(int k) { key = k; }

    public void setEnabled(boolean v) {
        if (v == enabled) return;
        enabled = v;
        try {
            if (v) onEnable(); else onDisable();
        } catch (RuntimeException ex) {
            ChatLog.error(name + " durum degisiminde hata: " + ex);
            enabled = false;
        }
        EventBus.post(new Events.ModuleToggled(this, enabled));
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() { }
    protected void onDisable() { }
    /** Yalnizca modul aciksa ve oyuncu/dunya varsa cagrilir. */
    public void tick() { }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("key", key);
        o.add("settings", SettingIO.save(settings));
        return o;
    }

    public void fromJson(JsonObject o) {
        if (o == null) return;
        if (o.has("key") && o.get("key").isJsonPrimitive() && o.get("key").getAsJsonPrimitive().isNumber()) key = o.get("key").getAsInt();
        if (o.has("settings") && o.get("settings").isJsonObject()) SettingIO.load(settings, o.getAsJsonObject("settings"));
        if (o.has("enabled") && o.get("enabled").isJsonPrimitive() && o.get("enabled").getAsJsonPrimitive().isBoolean()) setEnabled(o.get("enabled").getAsBoolean());
    }
}
