package dev.chatl.module.impl;

import dev.chatl.event.EventBus;
import dev.chatl.event.Events;
import dev.chatl.hud.HudRenderable;
import dev.chatl.hud.HudRenderer;
import dev.chatl.model.KnockbackRecord;
import dev.chatl.module.ChatModule;
import dev.chatl.setting.Setting;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Knockback GOZLEM kaydi: hasar aninda hiz degerlerini listeler. Hareket/hasar kurallarini DEGISTIRMEZ. */
public final class Velocity extends ChatModule implements HudRenderable {
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm:ss");

    public final Setting.Bool hud = add(new Setting.Bool("HUD'da hareket durumu", true));
    public final Setting.Num history = add(new Setting.Num("Kayit sayisi", 3, 15, 1, 6));
    public final Setting.Bool localOnly = add(new Setting.Bool("Yalnizca yerel/test dunyasi", true));

    private final Deque<KnockbackRecord> records = new ArrayDeque<>();
    private int lastHurt;
    private volatile String motion = "-";
    private volatile double curH, curV;

    public Velocity() {
        super("Velocity", "Izinli test dunyasinda knockback olaylarini kaydeder (gozlem, iptal etmez)", Category.COMBAT);
    }

    @Override public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        Vec3d v = p.getVelocity();
        curH = Math.hypot(v.x, v.z);
        curV = v.y;
        motion = p.isFallFlying() ? "Ucus" : p.isOnGround() ? "Yerde" : "Havada";
        boolean allowed = !localOnly.get() || mc.isInSingleplayer();
        if (p.hurtTime > lastHurt && allowed) {
            KnockbackRecord r = new KnockbackRecord(LocalTime.now(), curH, curV);
            synchronized (records) {
                records.addFirst(r);
                while (records.size() > (int) history.get()) records.removeLast();
            }
            EventBus.post(new Events.KnockbackRecorded(r));
        }
        lastHurt = p.hurtTime;
    }

    @Override protected void onDisable() { synchronized (records) { records.clear(); } lastHurt = 0; motion = "-"; }

    public List<KnockbackRecord> recent() { synchronized (records) { return new ArrayList<>(records); } }

    @Override public boolean hudVisible() { return hud.get(); }

    @Override public int renderHud(DrawContext g, TextRenderer tr, int x, int y) {
        HudRenderer.text(g, tr, "Velocity (gozlem)", x, y, 0xFFFFFF);
        HudRenderer.text(g, tr, String.format(Locale.ROOT, "%s  yatay %.2f  dikey %+.2f", motion, curH, curV), x, y + 11, 0x8A8F9C);
        int h = 22;
        for (KnockbackRecord r : recent()) {
            HudRenderer.text(g, tr, String.format(Locale.ROOT, "%s  H %.2f  V %+.2f", r.time().format(TF), r.horizontal(), r.vertical()), x, y + h, 0x5B9CFF);
            h += 10;
        }
        if (h == 22 && localOnly.get() && !MinecraftClient.getInstance().isInSingleplayer()) {
            HudRenderer.text(g, tr, "Kayit pasif: yerel dunya degil", x, y + h, 0xFFAA33);
            h += 10;
        }
        return h;
    }
}
