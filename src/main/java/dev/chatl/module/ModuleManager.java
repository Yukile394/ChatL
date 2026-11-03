package dev.chatl.module;

import com.google.gson.JsonObject;
import dev.chatl.ChatLClient;
import dev.chatl.module.impl.AimAssist;
import dev.chatl.module.impl.ElytraFix;
import dev.chatl.module.impl.FireworkFix;
import dev.chatl.module.impl.TriggerBot;
import dev.chatl.module.impl.Velocity;
import dev.chatl.service.TargetService;
import dev.chatl.util.ChatLog;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

/** Yalnizca bu bes modul vardir. */
public final class ModuleManager {
    public static final AimAssist AIM = new AimAssist();
    public static final TriggerBot TRIGGER = new TriggerBot();
    public static final Velocity VELOCITY = new Velocity();
    public static final ElytraFix ELYTRA = new ElytraFix();
    public static final FireworkFix FIREWORK = new FireworkFix();
    private static final List<ChatModule> ALL = List.of(AIM, TRIGGER, VELOCITY, ELYTRA, FIREWORK);

    private ModuleManager() { }

    public static List<ChatModule> all() { return ALL; }

    public static void loadFrom(JsonObject root) {
        for (ChatModule m : ALL) {
            if (root != null && root.has(m.name) && root.get(m.name).isJsonObject()) m.fromJson(root.getAsJsonObject(m.name));
        }
    }

    public static JsonObject saveTo() {
        JsonObject o = new JsonObject();
        for (ChatModule m : ALL) o.add(m.name, m.toJson());
        return o;
    }

    public static void tick(MinecraftClient mc, boolean allowKeys) {
        // Ortak hedef verisi: tick basina tek kez hesaplanir (AimAssist + TriggerBot paylasir).
        if (AIM.isEnabled() || TRIGGER.isEnabled()) {
            double range = Math.max(AIM.isEnabled() ? AIM.range.get() : 0, TRIGGER.isEnabled() ? TRIGGER.range.get() : 0);
            try { TargetService.update(mc, range); } catch (RuntimeException ex) { ChatLog.error("Hedef servisi hatasi: " + ex); TargetService.clear(); }
        } else {
            TargetService.clear();
        }

        for (ChatModule m : ALL) {
            if (allowKeys && m.key() >= 0) {
                boolean down = InputUtil.isKeyPressed(mc.getWindow().getHandle(), m.key());
                if (down && !m.keyDown) m.toggle();
                m.keyDown = down;
            }
            if (!m.isEnabled()) continue;
            try {
                m.tick();
            } catch (RuntimeException ex) {
                ChatLog.error(m.name + " tick hatasi, modul guvenli sekilde kapatildi: " + ex);
                m.setEnabled(false);
                ChatLClient.notice(m.name + " hata nedeniyle kapatildi");
            }
        }
    }
}
