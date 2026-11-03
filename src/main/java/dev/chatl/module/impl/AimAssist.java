package dev.chatl.module.impl;

import dev.chatl.hud.HudRenderable;
import dev.chatl.hud.HudRenderer;
import dev.chatl.model.TargetInfo;
import dev.chatl.module.ChatModule;
import dev.chatl.service.TargetService;
import dev.chatl.setting.Setting;
import java.util.Locale;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Antrenman gostergesi: hedef mesafesi ve aci farki. Kamerayi DEGISTIRMEZ. */
public final class AimAssist extends ChatModule implements HudRenderable {
    private static final int[] COLORS = {0x5B6CFF, 0x55FF88, 0xFF5566, 0xFFFFFF};

    public final Setting.Bool hud = add(new Setting.Bool("HUD'da hedef bilgisi", true));
    public final Setting.Num range = add(new Setting.Num("Menzil", 3, 12, 0.5, 6));
    public final Setting.Num fov = add(new Setting.Num("Gorus acisi", 10, 180, 5, 90));
    public final Setting.Num size = add(new Setting.Num("Gosterge boyutu", 4, 16, 1, 8));
    public final Setting.Mode color = add(new Setting.Mode("Gosterge rengi", 0, "Mavi", "Yesil", "Kirmizi", "Beyaz"));

    private volatile TargetInfo info;

    public AimAssist() {
        super("AimAssist", "Antrenman: hedef mesafesi ve gorus acisi gostergesi (yalnizca bilgi)", Category.COMBAT);
    }

    @Override public void tick() {
        TargetInfo t = TargetService.get();
        info = (t != null && Math.abs(t.yawDelta()) <= fov.get() / 2.0) ? t : null;
    }

    @Override protected void onDisable() { info = null; }

    @Override public boolean hudVisible() { return hud.get(); }

    @Override public int renderHud(DrawContext g, TextRenderer tr, int x, int y) {
        int sz = (int) size.get();
        int col = COLORS[color.index()];
        TargetInfo t = info;
        HudRenderer.text(g, tr, "AimAssist", x, y, 0xFFFFFF);
        g.fill(x, y + 12, x + sz, y + 12 + sz, 0xFF000000 | (t != null ? col : 0x444444));
        String s = t == null ? "Hedef yok"
                : String.format(Locale.ROOT, "%s  %.1fm  yaw %+.1f  pitch %+.1f", t.name(), t.distance(), t.yawDelta(), t.pitchDelta());
        HudRenderer.text(g, tr, s, x + sz + 4, y + 12 + Math.max(0, (sz - 8) / 2), t != null ? col : 0x8A8F9C);
        return 12 + Math.max(sz, 9);
    }
}
