package com.hbm_m.effect.render;

import com.hbm_m.lib.RefStrings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Рендерер иконки {@link com.hbm_m.effect.TaintEffect} из {@code textures/gui/potions.png}.
 * Координаты как в 1.7.10: {@code setIconIndex(0, 0)} → U=0, V=198.
 */
public final class TaintEffectRenderer {

    //? if fabric && < 1.21.1 {
    /*public static final ResourceLocation POTIONS_SHEET =
            new ResourceLocation(RefStrings.MODID, "textures/gui/potions.png");
    *///?} else {
    public static final ResourceLocation POTIONS_SHEET =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/potions.png");
    //?}

    public static final int U = 0;
    public static final int V = 198;
    public static final int ICON_SIZE = 18;
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    private TaintEffectRenderer() {}

    public static void renderIcon(GuiGraphics gfx, int x, int y, int blitOffset, float alpha) {
        gfx.setColor(1f, 1f, 1f, alpha);
        gfx.blit(POTIONS_SHEET,
                x, y, blitOffset,
                U, V, ICON_SIZE, ICON_SIZE,
                TEX_W, TEX_H);
        gfx.setColor(1f, 1f, 1f, 1f);
    }

    public static void renderInventory(GuiGraphics gfx, int x, int y, int blitOffset) {
        renderIcon(gfx, x, y + 7, blitOffset, 1f);
    }

    public static void renderHud(GuiGraphics gfx, int x, int y, int blitOffset, float alpha) {
        renderIcon(gfx, x + 3, y + 3, blitOffset, alpha);
    }
}
