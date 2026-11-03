package dev.chatl.module.impl;

import dev.chatl.ChatLClient;
import dev.chatl.hud.HudRenderable;
import dev.chatl.hud.HudRenderer;
import dev.chatl.model.TargetInfo;
import dev.chatl.module.ChatModule;
import dev.chatl.service.TargetService;
import dev.chatl.setting.Setting;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Saldiri cooldown ve zamanlama gostergesi. Saldiri GONDERMEZ, hicbir giris uretmez. */
public final class TriggerBot extends ChatModule implements HudRenderable {
    public final Setting.Bool hud = add(new Setting.Bool("HUD'da zamanlama", true));
    public final Setting.Num range = add(new Setting.Num("Menzil", 3, 8, 0.5, 6));
    public final Setting.Num threshold = add(new Setting.Num("Hazir esigi", 0.5, 1.0, 0.05, 0.9));
    public final Setting.Bool testMode = add(new Setting.Bool("Test modu (simule cooldown)", false));

    private volatile float cooldown;
    private volatile boolean ready;
    private volatile TargetInfo target;

    public TriggerBot() {
        super("TriggerBot", "Saldiri cooldown cubugu ve zamanlama gostergesi (yalnizca bilgi)", Category.COMBAT);
    }

    @Override public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        float cd = testMode.get() ? (ChatLClient.ticks % 40) / 40f : mc.player.getAttackCooldownProgress(0.5f);
        if (Float.isNaN(cd)) throw new IllegalStateException("gecersiz cooldown verisi");
        cooldown = Math.max(0f, Math.min(1f, cd));
        ready = cooldown >= threshold.get();
        TargetInfo t = TargetService.get();
        target = (t != null && t.distance() <= range.get()) ? t : null;
    }

    @Override protected void onDisable() { cooldown = 0; ready = false; target = null; }

    @Override public boolean hudVisible() { return hud.get(); }

    @Override public int renderHud(DrawContext g, TextRenderer tr, int x, int y) {
        int w = 80;
        float cd = cooldown;
        HudRenderer.text(g, tr, "TriggerBot" + (testMode.get() ? " [TEST]" : ""), x, y, 0xFFFFFF);
        g.fill(x, y + 12, x + w, y + 18, 0xFF2C2F38);
        g.fill(x, y + 12, x + (int) (w * cd), y + 18, ready ? 0xFF55FF88 : 0xFF5B6CFF);
        HudRenderer.text(g, tr, String.format(Locale.ROOT, "%d%%  %s", Math.round(cd * 100), ready ? "HAZIR" : "bekle"), x + w + 6, y + 11, ready ? 0x55FF88 : 0x8A8F9C);
        TargetInfo t = target;
        HudRenderer.text(g, tr, t == null ? "Hedef yok" : t.name() + (t.onCrosshair() ? "  [nisangahta]" : ""), x, y + 22, 0x8A8F9C);
        return 32;
    }
}
