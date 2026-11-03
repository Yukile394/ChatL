package dev.chatl.config;

import com.google.gson.JsonObject;
import dev.chatl.setting.Setting;
import dev.chatl.setting.SettingIO;
import java.util.List;

public final class GuiPrefs {
    private GuiPrefs() { }

    public static final Setting.Num hue = new Setting.Num("Vurgu rengi (ton)", 0, 360, 1, 235);
    public static final Setting.Num opacity = new Setting.Num("Panel saydamligi", 0.5, 1.0, 0.02, 0.96);
    public static final Setting.Num panelWidth = new Setting.Num("Panel genisligi", 440, 720, 10, 560);
    public static final Setting.Bool animation = new Setting.Bool("Animasyonlar", true);

    public static final Setting.Bool hudEnabled = new Setting.Bool("HUD goster", true);
    public static final Setting.Num hudScale = new Setting.Num("HUD olcegi", 0.5, 2.0, 0.1, 1.0);
    public static final Setting.Num hudX = new Setting.Num("HUD X", 0, 400, 2, 6);
    public static final Setting.Num hudY = new Setting.Num("HUD Y", 0, 300, 2, 6);

    public static List<Setting> guiSettings() { return List.of(hue, opacity, panelWidth, animation); }
    public static List<Setting> hudSettings() { return List.of(hudEnabled, hudScale, hudX, hudY); }

    public static void resetGui() { guiSettings().forEach(Setting::reset); }

    public static void load(JsonObject root) {
        if (root == null) return;
        if (root.has("gui") && root.get("gui").isJsonObject()) SettingIO.load(guiSettings(), root.getAsJsonObject("gui"));
        if (root.has("hud") && root.get("hud").isJsonObject()) SettingIO.load(hudSettings(), root.getAsJsonObject("hud"));
    }

    public static JsonObject save() {
        JsonObject o = new JsonObject();
        o.add("gui", SettingIO.save(guiSettings()));
        o.add("hud", SettingIO.save(hudSettings()));
        return o;
    }
}
