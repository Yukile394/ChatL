package dev.chatl.hud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public interface HudRenderable {
    boolean hudVisible();

    /** Cizilen blogun yuksekligini doner. */
    int renderHud(DrawContext ctx, TextRenderer tr, int x, int y);
}
