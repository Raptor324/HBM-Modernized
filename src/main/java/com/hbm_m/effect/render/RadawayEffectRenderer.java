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

    /** Левый верх 18×18 иконки (как у ванильного {@code GuiGraphics#blit(..., 18, 18, sprite)}). */
    public static void renderIcon(GuiGraphics gfx, int x, int y, int blitOffset, float alpha) {
        gfx.setColor(1f, 1f, 1f, alpha);
        gfx.blit(POTIONS_SHEET,
                x, y, blitOffset,
                U, V, ICON_SIZE, ICON_SIZE,
                TEX_W, TEX_H);
        gfx.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Инвентарь (Forge): x — уже позиция иконки, y — верх строки фона (32px).
     * Ваниль: {@code blit(x, y + 7, …, 18, 18, sprite)}.
     */
    public static void renderInventory(GuiGraphics gfx, int x, int y, int blitOffset) {
        renderIcon(gfx, x, y + 7, blitOffset, 1f);
    }

    /**
     * HUD (Forge): x, y — левый верх фона 24×24.
     * Ваниль: {@code blit(x + 3, y + 3, …, 18, 18, sprite)}.
     */
    public static void renderHud(GuiGraphics gfx, int x, int y, int blitOffset, float alpha) {
        renderIcon(gfx, x + 3, y + 3, blitOffset, alpha);
    }
}