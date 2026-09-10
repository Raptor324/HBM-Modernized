package com.hbm_m.effect.render;

import com.hbm_m.lib.RefStrings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Zeichnet ein Effektsymbol aus {@code textures/gui/potions.png}.
 *
 * <p>Im Original legt {@code setIconIndex(x, y)} die Kachel fest; das Blatt beginnt bei V=198.
 * Aus (x, y) wird hier also U = x * 18 und V = 198 + y * 18 - siehe {@code HbmPotion.init()}.</p>
 */
public final class PotionSheetRenderer {

    //? if fabric && < 1.21.1 {
    /*public static final ResourceLocation POTIONS_SHEET =
            new ResourceLocation(RefStrings.MODID, "textures/gui/potions.png");
    *///?} else {
    public static final ResourceLocation POTIONS_SHEET =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/potions.png");
    //?}

    public static final int ICON_SIZE = 18;
    /** Erste Zeile des Modblatts in potions.png. */
    public static final int SHEET_ORIGIN_V = 198;
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    private PotionSheetRenderer() {}

    public static int u(int iconX) {
        return iconX * ICON_SIZE;
    }

    public static int v(int iconY) {
        return SHEET_ORIGIN_V + iconY * ICON_SIZE;
    }

    public static void renderIcon(GuiGraphics gfx, int u, int v, int x, int y, int blitOffset, float alpha) {
        gfx.setColor(1f, 1f, 1f, alpha);
        gfx.blit(POTIONS_SHEET, x, y, blitOffset, u, v, ICON_SIZE, ICON_SIZE, TEX_W, TEX_H);
        gfx.setColor(1f, 1f, 1f, 1f);
    }

    public static void renderInventory(GuiGraphics gfx, int u, int v, int x, int y, int blitOffset) {
        renderIcon(gfx, u, v, x, y + 7, blitOffset, 1f);
    }

    public static void renderHud(GuiGraphics gfx, int u, int v, int x, int y, int blitOffset, float alpha) {
        renderIcon(gfx, u, v, x + 3, y + 3, blitOffset, alpha);
    }
}
