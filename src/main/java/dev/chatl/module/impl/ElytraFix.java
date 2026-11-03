package dev.chatl.module.impl;

import dev.chatl.ChatLClient;
import dev.chatl.event.EventBus;
import dev.chatl.event.Events;
import dev.chatl.hud.HudRenderable;
import dev.chatl.hud.HudRenderer;
import dev.chatl.module.ChatModule;
import dev.chatl.setting.Setting;
import dev.chatl.util.ChatLog;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Elytra durum takibi + gecersiz durum loglama. Oyuncu hareketine MUDAHALE ETMEZ. */
public final class ElytraFix extends ChatModule implements HudRenderable {
    public final Setting.Bool hud = add(new Setting.Bool("HUD'da ucus durumu", true));
    public final Setting.Bool warnLow = add(new Setting.Bool("Dusuk dayaniklilik uyarisi", true));
    public final Setting.Num lowThreshold = add(new Setting.Num("Uyari esigi (kalan)", 1, 50, 1, 10));

    private volatile boolean equipped, flying, lowDurability;
    private volatile long changedTick = -1000;
    private volatile double speed;
    private volatile int remaining;

    public ElytraFix() {
        super("ElytraFix", "Elytra ucus durumunu, gecersiz durumlari ve dayanikliligi izler", Category.MOVEMENT);
    }

    public boolean isFlying() { return flying; }
    public boolean isEquipped() { return equipped; }

    @Override public void tick() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        ItemStack chest = p.getEquippedStack(EquipmentSlot.CHEST);
        boolean eq = chest.isOf(Items.ELYTRA);
        boolean fly = p.isFallFlying();
        remaining = eq ? chest.getMaxDamage() - chest.getDamage() : 0;
        speed = p.getVelocity().length() * 20.0;
        lowDurability = warnLow.get() && eq && remaining <= lowThreshold.get();

        if (fly != flying || eq != equipped) {
            // gecis: eski veriyi temizle, durumu yayinla
            if (fly && !eq) ChatLog.warn("ElytraFix: gecersiz durum - ucus aktif ama Elytra takili degil");
            flying = fly;
            equipped = eq;
            changedTick = ChatLClient.ticks;
            EventBus.post(new Events.FlightStateChanged(fly, eq));
        }
    }

    @Override protected void onDisable() {
        flying = false; equipped = false; speed = 0; remaining = 0; lowDurability = false; changedTick = -1000;
    }

    @Override public boolean hudVisible() { return hud.get(); }

    @Override public int renderHud(DrawContext g, TextRenderer tr, int x, int y) {
        boolean flash = ChatLClient.ticks - changedTick < 40;
        String state = flying ? "UCUSTA" : equipped ? "Hazir (yerde/havada)" : "Elytra takili degil";
        HudRenderer.text(g, tr, "ElytraFix", x, y, 0xFFFFFF);
        HudRenderer.text(g, tr, (flash ? "> " : "") + state, x, y + 11, flash ? 0x55FF88 : 0x8A8F9C);
        int h = 22;
        if (flying) {
            HudRenderer.text(g, tr, String.format(Locale.ROOT, "Hiz %.1f b/s", speed), x, y + h, 0x8A8F9C);
            h += 10;
        }
        if (lowDurability) {
            HudRenderer.text(g, tr, "Elytra dayanikliligi dusuk (" + remaining + ")", x, y + h, 0xFF5566);
            h += 10;
        }
        return h;
    }
}
