package dev.chatl.gui;

import dev.chatl.ChatLClient;
import dev.chatl.config.ConfigManager;
import dev.chatl.config.GuiPrefs;
import dev.chatl.module.ChatModule;
import dev.chatl.module.ModuleManager;
import dev.chatl.service.Search;
import dev.chatl.setting.Setting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/** O tusu menusu. Sol tik: ac/kapat, SAG TIK: modulun ayarlari. */
public final class ClickGuiScreen extends Screen {
    private static final int BG = 0x16181D, CARD = 0x1D2027, CARD_HOVER = 0x262A33, OFF = 0x2C2F38,
            TXT = 0xE8EAF0, MUTED = 0x8A8F9C, BAR = 0x111317, GOOD = 0x55FF88, BAD = 0xFF5566;

    private enum Tab {
        MODULES("Modules"), CONFIG("Config"), FRIENDS("Friends"), HUD("HUD");
        final String label;
        Tab(String l) { label = l; }
    }

    private record FEntry(String name, boolean friend) { }

    private Tab tab = Tab.MODULES;
    private ChatModule.Category filter;
    private String search = "";
    private boolean searchFocus, friendFocus, profileOpen;
    private ChatModule selected;
    private Setting.Key bindListening;
    private Setting dragging;
    private double scroll;
    private long openAt;
    private String friendInput = "", friendMsg = "", hoverDesc;
    private Set<String> online = new HashSet<>();
    private final List<Setting> rows = new ArrayList<>();

    private int px, py, pw, ph, cy, cb, rowsX, rowsW;
    private float ga = 1f;

    public ClickGuiScreen() { super(Text.literal("ChatL")); }

    @Override protected void init() { openAt = Util.getMeasuringTimeMs(); }
    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { ConfigManager.save(); }

    // ---------------------------------------------------------------- layout / helpers
    private void layout() {
        float t = GuiPrefs.animation.get() ? MathHelper.clamp((Util.getMeasuringTimeMs() - openAt) / 180f, 0f, 1f) : 1f;
        float e = 1f - (1f - t) * (1f - t) * (1f - t);
        ga = Math.max(0.1f, e);
        pw = (int) Math.min(GuiPrefs.panelWidth.get(), width - 16);
        ph = Math.min(330, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2 + (int) ((1f - e) * 12f);
        cy = py + 82;
        cb = py + ph - 34;
    }

    private int c(int rgb) { return ((int) (ga * 255) << 24) | rgb; }
    private int c(int rgb, double a) { return ((int) (ga * a * 255) << 24) | rgb; }
    private int accent() { return MathHelper.hsvToRgb((float) (GuiPrefs.hue.get() / 360.0), 0.62f, 0.95f); }
    private void text(DrawContext g, String s, int x, int y, int rgb) { g.drawText(textRenderer, s, x, y, c(rgb), false); }
    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    private void toggle(DrawContext g, int x, int y, boolean on) {
        g.fill(x, y, x + 22, y + 10, c(on ? accent() : OFF));
        int kx = on ? x + 13 : x + 1;
        g.fill(kx, y + 1, kx + 8, y + 9, c(0xFFFFFF));
    }

    private static String keyName(int code) {
        if (code < 0) return "Yok";
        try { return InputUtil.fromKeyCode(code, -1).getLocalizedText().getString(); } catch (RuntimeException e) { return "?"; }
    }

    private int[] tabBounds(int i) {
        int x = px + 10;
        Tab[] ts = Tab.values();
        for (int k = 0; k < i; k++) x += textRenderer.getWidth(ts[k].label) + 18;
        return new int[]{x, textRenderer.getWidth(ts[i].label) + 16};
    }

    private ChatModule.Category[] cats() { return new ChatModule.Category[]{null, ChatModule.Category.COMBAT, ChatModule.Category.MOVEMENT}; }

    private int[] catBounds(int i) {
        int x = px + 10;
        for (int k = 0; k < i; k++) x += textRenderer.getWidth(catLabel(cats()[k])) + 18;
        return new int[]{x, textRenderer.getWidth(catLabel(cats()[i])) + 16};
    }

    private static String catLabel(ChatModule.Category c) { return c == null ? "All" : c.label; }

    private int viewTop() { return cy + (tab == Tab.MODULES && selected != null ? 26 : 0); }
    private int rowsTop() { return viewTop() - (int) scroll; }
    private int sliderX() { return rowsX + rowsW - 8 - 44 - 100; }
    private int cols() { return pw >= 500 ? 4 : 3; }

    private List<ChatModule> visible() {
        List<ChatModule> l = new ArrayList<>();
        for (ChatModule m : ModuleManager.all()) {
            if (filter != null && m.category != filter) continue;
            if (!Search.matches(m, search)) continue;
            l.add(m);
        }
        return l;
    }

    private int[] cardRect(int i) {
        int cols = cols(), gap = 6;
        int cw = (pw - 20 - gap * (cols - 1)) / cols, ch = 56;
        return new int[]{px + 10 + (i % cols) * (cw + gap), cy + (i / cols) * (ch + gap) - (int) scroll, cw, ch};
    }

    private void buildRows() {
        rows.clear();
        rowsX = px + 10;
        rowsW = pw - 20;
        switch (tab) {
            case MODULES -> {
                if (selected != null) {
                    final ChatModule m = selected;
                    rows.add(new Setting.Bool("Aktif", false) {
                        @Override public boolean get() { return m.isEnabled(); }
                        @Override public void set(boolean v) { m.setEnabled(v); }
                        @Override public boolean persistent() { return false; }
                    });
                    rows.add(new Setting.Key("Tus atamasi", m::key, m::setKey));
                    rows.addAll(m.settings());
                }
            }
            case CONFIG -> {
                rows.addAll(GuiPrefs.guiSettings());
                rows.add(new Setting.Action("Varsayilana sifirla", GuiPrefs::resetGui));
            }
            case HUD -> rows.addAll(GuiPrefs.hudSettings());
            default -> { }
        }
    }

    private Set<String> onlineNames() {
        Set<String> s = new HashSet<>();
        var nh = MinecraftClient.getInstance().getNetworkHandler();
        if (nh != null) for (PlayerListEntry e : nh.getPlayerList()) s.add(e.getProfile().getName().toLowerCase(Locale.ROOT));
        return s;
    }

    private List<FEntry> friendEntries() {
        List<FEntry> l = new ArrayList<>();
        String q = search.trim().toLowerCase(Locale.ROOT);
        for (String f : ConfigManager.friends().list()) if (q.isEmpty() || f.toLowerCase(Locale.ROOT).contains(q)) l.add(new FEntry(f, true));
        if (!q.isEmpty()) {
            for (String n : online) {
                if (n.contains(q) && !ConfigManager.friends().contains(n)) l.add(new FEntry(n, false));
            }
        }
        return l;
    }

    private double maxScroll() {
        buildRows();
        int content;
        switch (tab) {
            case MODULES -> content = selected != null ? rows.size() * 24 : ((visible().size() + cols() - 1) / cols()) * 62;
            case FRIENDS -> content = 40 + friendEntries().size() * 22;
            default -> content = rows.size() * 24;
        }
        return Math.max(0, content - (cb - viewTop()));
    }

    private void addFriend(String name) {
        String err = ConfigManager.friends().add(name);
        friendMsg = err != null ? err : "Eklendi: " + name;
        if (err == null) friendInput = "";
    }

    // ---------------------------------------------------------------- render
    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        layout();
        hoverDesc = null;
        online = onlineNames();
        g.fill(0, 0, width, height, (int) (ga * 0x50) << 24);
        g.fill(px, py, px + pw, py + ph, c(BG, GuiPrefs.opacity.get()));
        g.drawBorder(px, py, pw, ph, c(0x2A2D35));

        g.drawText(textRenderer, Text.literal("ChatL").formatted(Formatting.BOLD), px + 12, py + 11, c(TXT), false);
        text(g, "5 modul", px + pw - 12 - textRenderer.getWidth("5 modul"), py + 11, MUTED);

        // tabs
        for (int i = 0; i < Tab.values().length; i++) {
            int[] b = tabBounds(i);
            boolean act = Tab.values()[i] == tab;
            if (act) g.fill(b[0], py + 32, b[0] + b[1], py + 52, c(0x2A2D35));
            text(g, Tab.values()[i].label, b[0] + 8, py + 38, act ? TXT : MUTED);
        }
        // search
        int sx = px + pw - 140, sy = py + 34;
        g.fill(sx, sy, sx + 130, sy + 16, c(0x0F1115));
        if (searchFocus) g.drawBorder(sx, sy, 130, 16, c(accent()));
        String sdisp = search.isEmpty() && !searchFocus ? "Search" : search + (searchFocus && (Util.getMeasuringTimeMs() / 500) % 2 == 0 ? "_" : "");
        text(g, textRenderer.trimToWidth(sdisp, 120), sx + 5, sy + 4, search.isEmpty() && !searchFocus ? MUTED : TXT);

        // categories
        if (tab == Tab.MODULES && selected == null) {
            for (int i = 0; i < cats().length; i++) {
                int[] b = catBounds(i);
                boolean act = cats()[i] == filter;
                if (act) g.fill(b[0], py + 58, b[0] + b[1], py + 76, c(0x2A2D35));
                text(g, catLabel(cats()[i]), b[0] + 8, py + 63, act ? TXT : MUTED);
            }
        }

        buildRows();
        scroll = MathHelper.clamp(scroll, 0, maxScroll());
        g.enableScissor(px + 4, viewTop(), px + pw - 4, cb);
        switch (tab) {
            case MODULES -> { if (selected == null) drawModules(g, mx, my); else drawRows(g, mx, my); }
            case FRIENDS -> drawFriends(g, mx, my);
            default -> drawRows(g, mx, my);
        }
        g.disableScissor();

        if (tab == Tab.MODULES && selected != null) {
            boolean hb = in(mx, my, px + 10, cy, 60, 20);
            g.fill(px + 10, cy, px + 70, cy + 20, c(hb ? CARD_HOVER : CARD));
            text(g, "< Back", px + 18, cy + 6, TXT);
            text(g, selected.name + "  -  " + selected.description, px + 78, cy + 6, MUTED);
        }

        drawProfile(g, mx, my);
    }

    private void drawModules(DrawContext g, int mx, int my) {
        List<ChatModule> list = visible();
        if (list.isEmpty()) { text(g, "Sonuc bulunamadi: \"" + search + "\"", px + 14, cy + 10, MUTED); return; }
        for (int i = 0; i < list.size(); i++) {
            int[] r = cardRect(i);
            ChatModule m = list.get(i);
            if (r[1] + r[3] < cy || r[1] > cb) continue;
            boolean hv = in(mx, my, r[0], r[1], r[2], r[3]) && my >= cy && my < cb;
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], c(hv ? CARD_HOVER : CARD));
            if (hv) hoverDesc = m.description;
            text(g, "?", r[0] + 8, r[1] + 8, MUTED);
            toggle(g, r[0] + r[2] - 40, r[1] + 8, m.isEnabled());
            for (int d = 0; d < 3; d++) g.fill(r[0] + r[2] - 12, r[1] + 8 + d * 3, r[0] + r[2] - 10, r[1] + 10 + d * 3, c(MUTED));
            String hint = Search.matchedSetting(m, search);
            if (hint != null) text(g, "ayar: " + textRenderer.trimToWidth(hint, r[2] - 40), r[0] + 8, r[1] + r[3] - 26, MUTED);
            text(g, m.name, r[0] + 8, r[1] + r[3] - 14, m.isEnabled() ? TXT : MUTED);
        }
    }

    private void drawRows(DrawContext g, int mx, int my) {
        int y = rowsTop();
        for (Setting s : rows) {
            if (y + 22 > viewTop() && y < cb) {
                boolean hv = in(mx, my, rowsX, y, rowsW, 22) && my >= viewTop() && my < cb;
                g.fill(rowsX, y, rowsX + rowsW, y + 22, c(hv ? CARD_HOVER : CARD));
                int right = rowsX + rowsW - 8;
                if (s instanceof Setting.Action) {
                    String t = "[ " + s.name + " ]";
                    text(g, t, rowsX + 8, y + 7, accent());
                } else {
                    text(g, s.name, rowsX + 8, y + 7, TXT);
                }
                if (s instanceof Setting.Bool b) {
                    toggle(g, right - 22, y + 6, b.get());
                } else if (s instanceof Setting.Num n) {
                    int sx = sliderX();
                    int fill = (int) (100 * n.fraction());
                    g.fill(sx, y + 10, sx + 100, y + 12, c(OFF));
                    g.fill(sx, y + 10, sx + fill, y + 12, c(accent()));
                    g.fill(sx + fill - 2, y + 7, sx + fill + 2, y + 15, c(0xFFFFFF));
                    text(g, n.display(), sx + 108, y + 7, MUTED);
                } else if (s instanceof Setting.Mode m) {
                    String t = "< " + m.get() + " >";
                    text(g, t, right - textRenderer.getWidth(t), y + 7, accent());
                } else if (s instanceof Setting.Key k) {
                    String t = bindListening == k ? "bir tusa bas (ESC = temizle)" : "[" + keyName(k.get()) + "]";
                    text(g, t, right - textRenderer.getWidth(t), y + 7, bindListening == k ? GOOD : accent());
                }
            }
            y += 24;
        }
    }

    private void drawFriends(DrawContext g, int mx, int my) {
        int ix = px + 10, iw = pw - 20 - 56;
        g.fill(ix, cy, ix + iw, cy + 18, c(0x0F1115));
        if (friendFocus) g.drawBorder(ix, cy, iw, 18, c(accent()));
        boolean blink = friendFocus && (Util.getMeasuringTimeMs() / 500) % 2 == 0;
        text(g, friendInput.isEmpty() && !friendFocus ? "Oyuncu adi ekle" : friendInput + (blink ? "_" : ""), ix + 6, cy + 5, friendInput.isEmpty() && !friendFocus ? MUTED : TXT);
        int bx = px + pw - 10 - 50;
        g.fill(bx, cy, bx + 50, cy + 18, c(accent(), 0.85));
        text(g, "Ekle", bx + 13, cy + 5, 0xFFFFFF);
        if (!friendMsg.isEmpty()) text(g, friendMsg, ix, cy + 23, MUTED);

        List<FEntry> list = friendEntries();
        int y = cy + 40 - (int) scroll;
        if (list.isEmpty()) {
            text(g, search.isBlank() ? "Henuz arkadas yok. Yukaridan ekle veya Search ile oyuncu ara." : "Eslesen oyuncu yok: \"" + search + "\"", ix, cy + 44, MUTED);
            return;
        }
        for (FEntry e : list) {
            if (y + 20 > cy && y < cb) {
                g.fill(ix, y, px + pw - 10, y + 20, c(CARD));
                text(g, e.name(), ix + 8, y + 6, e.friend() ? TXT : MUTED);
                int rx = px + pw - 10;
                if (e.friend()) {
                    if (MinecraftClient.getInstance().getNetworkHandler() != null && online.contains(e.name().toLowerCase(Locale.ROOT)))
                        text(g, "cevrim ici", rx - 24 - textRenderer.getWidth("cevrim ici"), y + 6, GOOD);
                    text(g, "x", rx - 14, y + 6, BAD);
                } else {
                    text(g, "sunucuda  +", rx - 14 - textRenderer.getWidth("sunucuda  "), y + 6, accent());
                }
            }
            y += 22;
        }
    }

    private void drawProfile(DrawContext g, int mx, int my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int by = py + ph - 30;
        g.fill(px + 1, by, px + pw - 1, py + ph - 1, c(BAR));
        boolean hv = in(mx, my, px + 8, by + 2, 160, 26);
        if (hv) g.fill(px + 8, by + 2, px + 168, by + 28, c(CARD_HOVER));
        String name = mc.getSession().getUsername();
        try {
            PlayerSkinDrawer.draw(g, mc.getSkinProvider().getSkinTextures(mc.getGameProfile()), px + 12, by + 5, 20);
        } catch (RuntimeException e) {
            g.fill(px + 12, by + 5, px + 32, by + 25, c(0x6B4F2A)); // varsayilan avatar yerine duz blok; cokme yok
        }
        text(g, name, px + 38, by + 11, TXT);
        if (hoverDesc != null) {
            String d = textRenderer.trimToWidth(hoverDesc, pw - 220);
            text(g, d, px + pw - 10 - textRenderer.getWidth(d), by + 11, MUTED);
        }
        if (profileOpen) {
            int bx = px + 8, bh = 56, byy = by - bh - 4;
            g.fill(bx, byy, bx + 190, byy + bh, c(0x0F1115));
            g.drawBorder(bx, byy, 190, bh, c(0x2A2D35));
            text(g, "Hesap: " + name, bx + 8, byy + 8, TXT);
            text(g, "ChatL " + modVersion("chatl"), bx + 8, byy + 22, MUTED);
            text(g, "Minecraft " + SharedConstants.getGameVersion().getName(), bx + 8, byy + 36, MUTED);
        }
    }

    static String modVersion(String id) {
        return FabricLoader.getInstance().getModContainer(id).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    // ---------------------------------------------------------------- input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        buildRows();
        int by = py + ph - 30;
        if (in(mx, my, px + 8, by + 2, 160, 26)) { profileOpen = !profileOpen; return true; }
        profileOpen = false;

        for (int i = 0; i < Tab.values().length; i++) {
            int[] b = tabBounds(i);
            if (in(mx, my, b[0], py + 32, b[1], 20)) {
                tab = Tab.values()[i]; selected = null; scroll = 0; searchFocus = friendFocus = false; return true;
            }
        }
        if (in(mx, my, px + pw - 140, py + 34, 130, 16)) { searchFocus = true; friendFocus = false; return true; }
        searchFocus = false;

        if (tab == Tab.MODULES && selected == null) {
            for (int i = 0; i < cats().length; i++) {
                int[] b = catBounds(i);
                if (in(mx, my, b[0], py + 58, b[1], 18)) { filter = cats()[i]; scroll = 0; return true; }
            }
        }
        if (tab == Tab.MODULES && selected != null && in(mx, my, px + 10, cy, 60, 20)) { selected = null; scroll = 0; return true; }
        if (my < viewTop() || my >= cb) return super.mouseClicked(mx, my, button);

        switch (tab) {
            case MODULES -> {
                if (selected == null) {
                    List<ChatModule> list = visible();
                    for (int i = 0; i < list.size(); i++) {
                        int[] r = cardRect(i);
                        if (!in(mx, my, r[0], r[1], r[2], r[3])) continue;
                        ChatModule m = list.get(i);
                        if (button == 1 || mx >= r[0] + r[2] - 16) { selected = m; scroll = 0; }
                        else if (button == 0) m.toggle();
                        return true;
                    }
                } else if (rowClick(mx, my, button)) return true;
            }
            case FRIENDS -> { if (friendsClick(mx, my)) return true; }
            default -> { if (rowClick(mx, my, button)) return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean rowClick(double mx, double my, int button) {
        int y = rowsTop();
        for (Setting s : rows) {
            if (in(mx, my, rowsX, y, rowsW, 22)) {
                if (s instanceof Setting.Bool b) { b.set(!b.get()); }
                else if (s instanceof Setting.Num n) {
                    int sx = sliderX();
                    if (mx >= sx - 4 && mx <= sx + 104) { dragging = n; n.setFraction((mx - sx) / 100.0); }
                }
                else if (s instanceof Setting.Mode m) { m.cycle(button == 1 ? -1 : 1); }
                else if (s instanceof Setting.Key k) { bindListening = k; }
                else if (s instanceof Setting.Action a) { a.run(); }
                return true;
            }
            y += 24;
        }
        return false;
    }

    private boolean friendsClick(double mx, double my) {
        int ix = px + 10, iw = pw - 20 - 56, bx = px + pw - 10 - 50;
        if (in(mx, my, ix, cy, iw, 18)) { friendFocus = true; return true; }
        friendFocus = false;
        if (in(mx, my, bx, cy, 50, 18)) { addFriend(friendInput); return true; }
        int y = cy + 40 - (int) scroll;
        for (FEntry e : friendEntries()) {
            if (in(mx, my, ix, y, pw - 20, 20)) {
                if (e.friend() && mx >= px + pw - 10 - 22) { ConfigManager.friends().remove(e.name()); friendMsg = "Kaldirildi: " + e.name(); }
                else if (!e.friend() && mx >= px + pw - 10 - 80) { addFriend(e.name()); }
                return true;
            }
            y += 22;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging instanceof Setting.Num n) { n.setFraction((mx - sliderX()) / 100.0); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) { dragging = null; ConfigManager.save(); }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = MathHelper.clamp(scroll - v * 16, 0, maxScroll());
        return true;
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        if (!SharedConstants.isValidChar(ch)) return false;
        if (searchFocus) { if (search.length() < 24) search += ch; scroll = 0; return true; }
        if (friendFocus) {
            if (friendInput.length() < 16 && (Character.isLetterOrDigit(ch) || ch == '_') && ch < 128) friendInput += ch;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (bindListening != null) {
            bindListening.set(key == GLFW.GLFW_KEY_ESCAPE ? -1 : key);
            bindListening = null;
            ConfigManager.save();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (searchFocus || friendFocus) { searchFocus = friendFocus = false; return true; }
            if (selected != null) { selected = null; scroll = 0; return true; }
            close();
            return true;
        }
        if (searchFocus || friendFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (searchFocus && !search.isEmpty()) search = search.substring(0, search.length() - 1);
                if (friendFocus && !friendInput.isEmpty()) friendInput = friendInput.substring(0, friendInput.length() - 1);
            } else if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && friendFocus) {
                addFriend(friendInput);
            }
            return true; // yazarken O tusu menuyu kapatmasin
        }
        if (ChatLClient.OPEN_GUI.matchesKey(key, scan)) { close(); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_LEFT) {
            int n = Tab.values().length;
            tab = Tab.values()[Math.floorMod(tab.ordinal() + (key == GLFW.GLFW_KEY_RIGHT ? 1 : -1), n)];
            selected = null; scroll = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) { scroll = MathHelper.clamp(scroll + 16, 0, maxScroll()); return true; }
        if (key == GLFW.GLFW_KEY_UP) { scroll = MathHelper.clamp(scroll - 16, 0, maxScroll()); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
