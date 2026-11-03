package dev.chatl.gui;

import dev.chatl.ChatLClient;
import dev.chatl.module.ChatModule;
import dev.chatl.module.ModuleManager;
import dev.chatl.service.CompatChecks;
import dev.chatl.setting.Setting;
import dev.chatl.util.ChatLog;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** P tusu paneli: baglanti, surumler, uyumluluk testleri, modul durumlari ve gunluk. O menusunden bagimsizdir. */
public final class CompatScreen extends Screen {
    private static final int TXT = 0xE8EAF0, MUTED = 0x8A8F9C, GOOD = 0x55FF88, BAD = 0xFF5566;
    private List<CompatChecks.Result> results = List.of();
    private int ticks, px, py, pw, ph;

    public CompatScreen() { super(Text.literal("ChatL Uyumluluk")); }

    @Override protected void init() { results = CompatChecks.runAll(); }
    @Override public boolean shouldPause() { return false; }

    @Override public void tick() { if (++ticks % 20 == 0) results = CompatChecks.runAll(); }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (ChatLClient.OPEN_COMPAT.matchesKey(key, scan)) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean mouseClicked(double mx, double my, int b) {
        if (mx >= px + pw - 90 && mx < px + pw - 10 && my >= py + 8 && my < py + 24) { results = CompatChecks.runAll(); return true; }
        return super.mouseClicked(mx, my, b);
    }

    private void line(DrawContext g, String s, int x, int y, int maxW, int rgb) {
        g.drawText(textRenderer, textRenderer.trimToWidth(s, maxW), x, y, 0xFF000000 | rgb, false);
    }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        pw = Math.min(560, width - 16);
        ph = Math.min(320, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        g.fill(0, 0, width, height, 0x50000000);
        g.fill(px, py, px + pw, py + ph, 0xF216181D);
        g.drawBorder(px, py, pw, ph, 0xFF2A2D35);
        g.drawText(textRenderer, Text.literal("ChatL  -  Baglanti ve Uyumluluk").formatted(Formatting.BOLD), px + 12, py + 12, 0xFFE8EAF0, false);
        g.fill(px + pw - 90, py + 8, px + pw - 10, py + 24, 0xFF2A2D35);
        line(g, "Testleri yenile", px + pw - 86, py + 12, 76, TXT);

        int colW = (pw - 36) / 2, lx = px + 12, rx = px + 24 + colW, y = py + 36;

        // sol sutun
        line(g, "BAGLANTI", lx, y, colW, MUTED); y += 12;
        ServerInfo si = mc.getCurrentServerEntry();
        String addr = si != null ? si.address : mc.isInSingleplayer() ? "Tek oyunculu (yerel)" : "-";
        var nh = mc.getNetworkHandler();
        line(g, "Sunucu: " + addr, lx, y, colW, TXT); y += 11;
        line(g, "Durum: " + (nh != null ? "bagli" : "bagli degil"), lx, y, colW, nh != null ? GOOD : BAD); y += 11;
        if (nh != null && mc.player != null) {
            PlayerListEntry e = nh.getPlayerListEntry(mc.player.getUuid());
            if (e != null) { line(g, "Gecikme: " + e.getLatency() + " ms", lx, y, colW, TXT); y += 11; }
        }
        y += 6;
        line(g, "SURUMLER", lx, y, colW, MUTED); y += 12;
        line(g, "Minecraft: " + SharedConstants.getGameVersion().getName(), lx, y, colW, TXT); y += 11;
        line(g, "Fabric Loader: " + ClickGuiScreen.modVersion("fabricloader"), lx, y, colW, TXT); y += 11;
        line(g, "Fabric API: " + ClickGuiScreen.modVersion("fabric-api"), lx, y, colW, TXT); y += 11;
        line(g, "ChatL: " + ClickGuiScreen.modVersion("chatl"), lx, y, colW, TXT); y += 17;
        line(g, "MODUL DURUMU", lx, y, colW, MUTED); y += 12;
        for (ChatModule m : ModuleManager.all()) {
            StringBuilder sb = new StringBuilder(m.name).append(m.isEnabled() ? "  ACIK" : "  kapali");
            if (m.isEnabled()) {
                sb.append("  ");
                for (Setting s : m.settings()) {
                    if (s instanceof Setting.Num n) sb.append(n.display()).append(' ');
                    else if (s instanceof Setting.Mode md) sb.append(md.get()).append(' ');
                }
            }
            line(g, sb.toString().trim(), lx, y, colW, m.isEnabled() ? GOOD : MUTED); y += 11;
        }

        // sag sutun
        y = py + 36;
        line(g, "UYUMLULUK TESTLERI", rx, y, colW, MUTED); y += 12;
        for (CompatChecks.Result r : results) {
            line(g, (r.ok() ? "[OK] " : "[!!] ") + r.name(), rx, y, colW, r.ok() ? GOOD : BAD); y += 10;
            line(g, "     " + r.detail(), rx, y, colW, MUTED); y += 12;
        }
        y += 6;
        line(g, "GUNLUK", rx, y, colW, MUTED); y += 12;
        List<String> logs = ChatLog.recent(Math.max(3, (py + ph - 26 - y) / 10));
        if (logs.isEmpty()) line(g, "Kayit yok.", rx, y, colW, MUTED);
        for (String s : logs) {
            line(g, s, rx, y, colW, s.contains("[ERROR]") ? BAD : s.contains("[WARN]") ? 0xFFAA33 : MUTED); y += 10;
        }
        line(g, "Bu istemci sunucu korumalarini gizlice asmaz; yalnizca izinli ortamlar icindir.", px + 12, py + ph - 14, pw - 24, MUTED);
    }
}
