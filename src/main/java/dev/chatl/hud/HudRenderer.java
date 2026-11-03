package dev.chatl.hud;

import dev.chatl.config.GuiPrefs;
import dev.chatl.module.ChatModule;
import dev.chatl.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class HudRenderer {
    private HudRenderer() { }

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden || !GuiPrefs.hudEnabled.get()) return;
            float s = (float) GuiPrefs.hudScale.get();
            ctx.getMatrices().push();
            ctx.getMatrices().scale(s, s, 1f);
            int x = (int) GuiPrefs.hudX.get();
            int y = (int) GuiPrefs.hudY.get();
            for (ChatModule m : ModuleManager.all()) {
                if (!m.isEnabled() || !(m instanceof HudRenderable h) || !h.hudVisible()) continue;
                y += h.renderHud(ctx, mc.textRenderer, x, y) + 6;
            }
            ctx.getMatrices().pop();
        });
    }

    public static void text(DrawContext g, TextRenderer tr, String s, int x, int y, int rgb) {
        g.drawTextWithShadow(tr, s, x, y, 0xFF000000 | rgb);
    }
}
