package com.hbm_m.compat.jei;

import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * NEI-style press JEI texture coordinates (1:1 with 1.7.10 {@code gui_nei_press.png}).
 */
public final class JeiPressTextures {

    public static final ResourceLocation GUI_NEI_PRESS =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_press.png");

    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    public static final int RECIPE_WIDTH = 166;
    public static final int RECIPE_HEIGHT = 65;

    public static final int SLOT_U = 5;
    public static final int SLOT_V = 87;
    public static final int SLOT_SIZE = 18;

    public static final int PROGRESS_U = 150;
    public static final int PROGRESS_V = 72;
    public static final int PROGRESS_W = 18;
    public static final int PROGRESS_H = 18;

    private JeiPressTextures() {
    }

    public static IDrawable createRecipeBackground(IGuiHelper guiHelper) {
        return guiHelper.createDrawable(GUI_NEI_PRESS, 0, 0, RECIPE_WIDTH, RECIPE_HEIGHT);
    }

    public static IDrawable createItemSlotBackground(IGuiHelper guiHelper) {
        return guiHelper.createDrawable(GUI_NEI_PRESS, SLOT_U, SLOT_V, SLOT_SIZE, SLOT_SIZE);
    }
}
