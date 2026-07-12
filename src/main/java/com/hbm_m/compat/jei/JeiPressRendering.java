package com.hbm_m.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws NEI-style press overlays on top of {@link JeiPressTextures#GUI_NEI_PRESS}.
 */
public final class JeiPressRendering {

    private JeiPressRendering() {
    }

    public static void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(JeiPressTextures.GUI_NEI_PRESS, x, y, u, v, width, height,
                JeiPressTextures.TEXTURE_WIDTH, JeiPressTextures.TEXTURE_HEIGHT);
    }

    public static void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        blit(graphics, x - 1, y - 1, JeiPressTextures.SLOT_U, JeiPressTextures.SLOT_V,
                JeiPressTextures.SLOT_SIZE, JeiPressTextures.SLOT_SIZE);
    }

    /**
     * Port of {@code PressRecipeHandler.drawExtras}: vertical progress bar at (47, 24).
     */
    public static void drawProgressBar(GuiGraphics graphics) {
        int tick = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.tickCount
                : 0;
        int cycle = 20;
        int height = (tick % cycle) * JeiPressTextures.PROGRESS_H / cycle;

        if (height <= 0) {
            return;
        }

        blit(graphics,
                47,
                24 + JeiPressTextures.PROGRESS_H - height,
                JeiPressTextures.PROGRESS_U,
                JeiPressTextures.PROGRESS_V + JeiPressTextures.PROGRESS_H - height,
                JeiPressTextures.PROGRESS_W,
                height);
    }
}
