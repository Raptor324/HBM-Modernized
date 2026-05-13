package com.hbm_m.effect.render;

import com.hbm_m.lib.RefStrings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Платформонезависимый рендерер иконки RadawayEffect.
 * Вся логика рисования сосредоточена здесь.
 * Forge вызывает через IClientMobEffectExtensions, Fabric — через Mixin.
 */
public final class RadawayEffectRenderer {

    //? if fabric && < 1.21.1 {
    /*public static final ResourceLocation POTIONS_SHEET =
            new ResourceLocation(RefStrings.MODID, "textures/gui/potions.png");
    *///?} else {
    public static final ResourceLocation POTIONS_SHEET =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/potions.png");
    //?}

    // Координаты иконки на спрайт-листе
    public static final int U         = 18;
    public static final int V         = 198;
    public static final int ICON_SIZE = 18;
    private static final int TEX_W   = 256;
    private static final int TEX_H   = 256;

    private RadawayEffectRenderer() {}

    /**
     * Рисует иконку в инвентаре.
     * x, y — координаты фона эффекта (24×24); центрируем иконку 18×18 внутри.
     */
    public static void renderInventory(GuiGraphics gfx, int x, int y, int blitOffset) {
        gfx.blit(POTIONS_SHEET,
                x + 3, y + 3, blitOffset,
                U, V, ICON_SIZE, ICON_SIZE,
                TEX_W, TEX_H);
    }

    /**
     * Рисует иконку на HUD.
     * x, y — координаты фона (22×22); центрируем иконку 18×18 внутри.
     */
    public static void renderHud(GuiGraphics gfx, int x, int y, int blitOffset, float alpha) {
        gfx.setColor(1f, 1f, 1f, alpha);
        gfx.blit(POTIONS_SHEET,
                x + 2, y + 2, blitOffset,
                U, V, ICON_SIZE, ICON_SIZE,
                TEX_W, TEX_H);
        gfx.setColor(1f, 1f, 1f, 1f);
    }
}