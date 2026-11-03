package dev.chatl.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;

public final class SettingIO {
    private SettingIO() { }

    public static JsonObject save(List<? extends Setting> list) {
        JsonObject o = new JsonObject();
        for (Setting s : list) {
            if (!s.persistent()) continue;
            JsonElement e = s.toJson();
            if (e != null) o.add(s.name, e);
        }
        return o;
    }

    public static void load(List<? extends Setting> list, JsonObject o) {
        if (o == null) return;
        for (Setting s : list) {
            if (!s.persistent() || !o.has(s.name)) continue;
            try { s.fromJson(o.get(s.name)); } catch (RuntimeException ignored) { /* bozuk deger: varsayilan kalir */ }
        }
    }
}
