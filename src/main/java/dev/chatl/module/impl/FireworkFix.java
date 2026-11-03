package dev.chatl.module.impl;

import dev.chatl.event.EventBus;
import dev.chatl.event.Events;
import dev.chatl.hud.HudRenderable;
import dev.chatl.hud.HudRenderer;
import dev.chatl.module.ChatModule;
import dev.chatl.setting.Setting;
import dev.chatl.util.ChatLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Hotbar'daki havai fisek durumunu izler; esya kullanim kurallarina MUDAHALE ETMEZ. */
public final class FireworkFix extends ChatModule implements HudRenderable {
    public final Setting.Bool hud = add(new Setting.Bool("HUD'da fisek durumu", true));
    public final Setting.Bool warnSlot = add(new Setting.Bool("Yanlis slot uyarisi", true));

    private Runnable unsubFlight, unsubToggle;
    private volatile Boolean sharedFlight;      // ElytraFix'ten gelen ucus durumu; null = olay yok
    private volatile int selectedSlot, rocketSlot = -1, rocketTotal, activeRockets;
    private volatile boolean holding, flyingNow;
    private volatile String warning;

    public FireworkFix() {
        super("FireworkFix", "Hotbar'daki havai fisegi ve ucus sirasindaki kullanimi izler", Category.MOVEMENT);
    }

    public Boolean sharedFlight() { return sharedFlight; }

    @Override protected void onEnable() {
        unsubFlight = EventBus.subscribe(Events.FlightStateChanged.class, e -> sharedFlight = e.flying());
        unsubToggle = EventBus.subscribe(Events.ModuleToggled.class, e -> {
            if (e.module().name.equals("ElytraFix") && !e.enabled()) sharedFlight = null; // ElytraFix kapandi -> kendi okumasina don
        });
    }

    @Override protected void onDisable() {
        if (unsubFlight != null) unsubFlight.run();
        if (unsubToggle != null) unsubToggle.run();
        unsubFlight = unsubToggle = null;
        sharedFlight = null; warning = null; rocketSlot = -1; rocketTotal = 0; activeRockets = 0; holding = false; flyingNow = false;
    }

    @Override public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        PlayerInventory inv = p.getInventory();
        selectedSlot = inv.selectedSlot;
        int slot = -1, total = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(Items.FIREWORK_ROCKET)) {
                total += s.getCount();
                if (slot < 0 || i == selectedSlot) slot = i;
            }
        }
        rocketSlot = slot;
        rocketTotal = total;
        holding = p.getMainHandStack().isOf(Items.FIREWORK_ROCKET) || p.getOffHandStack().isOf(Items.FIREWORK_ROCKET);
        Boolean sf = sharedFlight;
        boolean flying = sf != null ? sf : p.isFallFlying();
        flyingNow = flying;
        activeRockets = flying
                ? mc.world.getEntitiesByClass(FireworkRocketEntity.class, p.getBoundingBox().expand(8), e -> true).size()
                : 0;
        String w = null;
        if (warnSlot.get() && flying) {
            if (total == 0) w = "Hotbar'da havai fisek yok";
            else if (!holding) w = "Secili slot fisek degil (fisek: slot " + (slot + 1) + ")";
        }
        if (w != null && !w.equals(warning)) ChatLog.warn("FireworkFix: " + w);
        warning = w;
    }

    @Override public boolean hudVisible() { return hud.get(); }

    @Override public int renderHud(DrawContext g, TextRenderer tr, int x, int y) {
        HudRenderer.text(g, tr, "FireworkFix", x, y, 0xFFFFFF);
        String s = rocketTotal == 0 ? "Hotbar'da fisek yok"
                : "Fisek x" + rocketTotal + (holding ? "  (elde)" : "  (slot " + (rocketSlot + 1) + ")")
                  + (activeRockets > 0 ? "  aktif: " + activeRockets : "");
        HudRenderer.text(g, tr, s, x, y + 11, holding ? 0x55FF88 : 0x8A8F9C);
        int h = 22;
        String w = warning;
        if (w != null) { HudRenderer.text(g, tr, "! " + w, x, y + h, 0xFF5566); h += 10; }
        return h;
    }
}
