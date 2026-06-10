package com.hbm_m.compat.jei;

import com.hbm_m.recipe.AnvilRecipe.OverlayType;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws NEI-style anvil overlay frames on top of {@link JeiAnvilTextures#GUI_NEI_ANVIL}.
 */
public final class JeiAnvilRendering {

    private JeiAnvilRendering() {
    }

    public static void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(JeiAnvilTextures.GUI_NEI_ANVIL, x, y, u, v, width, height,
                JeiAnvilTextures.TEXTURE_WIDTH, JeiAnvilTextures.TEXTURE_HEIGHT);
    }

    public static void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        blit(graphics, x - 1, y - 1, JeiAnvilTextures.SLOT_U, JeiAnvilTextures.SLOT_V,
                JeiAnvilTextures.SLOT_SIZE, JeiAnvilTextures.SLOT_SIZE);
    }

    public static void drawOverlay(OverlayType shape, GuiGraphics graphics) {
        switch (shape) {
            case NONE -> {
                blit(graphics, 2, 5, 5, 87, 72, 54);
                blit(graphics, 92, 5, 5, 87, 72, 54);
                blit(graphics, 74, 14, 131, 96, 18, 36);
            }
            case SMITHING -> {
                blit(graphics, 47, 23, 113, 105, 18, 18);
                blit(graphics, 101, 23, 113, 105, 18, 18);
                blit(graphics, 74, 14, 149, 96, 18, 36);
            }
            case CONSTRUCTION -> {
                blit(graphics, 11, 5, 5, 87, 108, 54);
                blit(graphics, 137, 23, 113, 105, 18, 18);
                blit(graphics, 119, 14, 167, 96, 18, 36);
            }
            case RECYCLING -> {
                blit(graphics, 11, 23, 113, 105, 18, 18);
                blit(graphics, 47, 5, 5, 87, 108, 54);
                blit(graphics, 29, 14, 185, 96, 18, 36);
            }
        }
    }
}
