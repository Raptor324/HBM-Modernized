package com.hbm_m.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
/**
 * Draws NEI-style shredder overlays on top of {@link JeiShredderTextures#GUI_NEI_SHREDDER}.
 */
@OnlyIn(Dist.CLIENT)
public final class JeiShredderRendering {

    private JeiShredderRendering() {
    }

    public static void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(JeiShredderTextures.GUI_NEI_SHREDDER, x, y, u, v, width, height,
                JeiShredderTextures.TEXTURE_WIDTH, JeiShredderTextures.TEXTURE_HEIGHT);
    }

    /** NEI tick counter (phase is arbitrary in 1.7.10 too — {@code cycleticks} starts at a random value). */
    private static int ticks() {
        return Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.tickCount
                : 0;
    }

    /**
     * Port of {@code ShredderRecipeHandler.drawExtras}: arrow
     * {@code drawProgressBar(80, 23, 100, 118, 24, 16, 48, 0)} — grows left-to-right.
     */
    public static void drawProgressArrow(GuiGraphics graphics) {
        int var = (ticks() % JeiShredderTextures.ARROW_TICKS) * JeiShredderTextures.ARROW_W
                / JeiShredderTextures.ARROW_TICKS;

        if (var <= 0) {
            return;
        }

        blit(graphics, 80, 23,
                JeiShredderTextures.ARROW_U,
                JeiShredderTextures.ARROW_V,
                var, JeiShredderTextures.ARROW_H);
    }

    /**
     * Port of {@code ShredderRecipeHandler.drawExtras}: power bar
     * {@code drawProgressBar(3, 6, 36, 86, 16, 52, 480, 7)} — direction 7 = inverted up:
     * anchored at the bottom, drains downward as the cycle advances.
     */
    public static void drawPowerBar(GuiGraphics graphics) {
        int fillTicks = JeiShredderTextures.POWER_TICKS - (ticks() % JeiShredderTextures.POWER_TICKS);
        int var = fillTicks * JeiShredderTextures.POWER_H / JeiShredderTextures.POWER_TICKS;

        if (var <= 0) {
            return;
        }

        blit(graphics, 3, 6 + JeiShredderTextures.POWER_H - var,
                JeiShredderTextures.POWER_U,
                JeiShredderTextures.POWER_V + JeiShredderTextures.POWER_H - var,
                JeiShredderTextures.POWER_W, var);
    }
}
