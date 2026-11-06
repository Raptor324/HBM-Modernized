package com.hbm_m.compat.jei;

import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * NEI-style shredder JEI texture coordinates (1:1 with 1.7.10 {@code gui_nei_shredder.png}).
 */
public final class JeiShredderTextures {

    public static final ResourceLocation GUI_NEI_SHREDDER =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_shredder.png");

    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    public static final int RECIPE_WIDTH = 166;
    public static final int RECIPE_HEIGHT = 65;

    /** Power bar fill — {@code drawProgressBar(3, 6, 36, 86, 16, 52, 480, 7)} (texture shifted -5,-11). */
    public static final int POWER_U = 31;
    public static final int POWER_V = 75;
    public static final int POWER_W = 16;
    public static final int POWER_H = 52;
    public static final int POWER_TICKS = 480;

    /** Progress arrow — {@code drawProgressBar(80, 23, 100, 118, 24, 16, 48, 0)} (texture shifted -5,-11). */
    public static final int ARROW_U = 95;
    public static final int ARROW_V = 107;
    public static final int ARROW_W = 24;
    public static final int ARROW_H = 16;
    public static final int ARROW_TICKS = 48;

    private JeiShredderTextures() {
    }

    public static IDrawable createRecipeBackground(IGuiHelper guiHelper) {
        return guiHelper.createDrawable(GUI_NEI_SHREDDER, 0, 0, RECIPE_WIDTH, RECIPE_HEIGHT);
    }
}
