package com.hbm_m.compat.jei;

import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared NEI-style JEI texture coordinates (1:1 with 1.7.10 {@code gui_nei.png}, 256x256 atlas).
 */
public final class JeiNeiTextures {

    public static final ResourceLocation GUI_NEI =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei.png");

    /** {@code TemplateRecipeHandler} recipe panel size. */
    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    public static final int RECIPE_WIDTH = 166;
    public static final int RECIPE_HEIGHT = 65;

    public static final int SLOT_U = 5;
    public static final int SLOT_V = 87;
    public static final int SLOT_SIZE = 18;

    public static final int MACHINE_U = 59;
    public static final int MACHINE_V = 87;
    public static final int MACHINE_W = 18;
    public static final int MACHINE_H = 36;

    public static final int MACHINE_TEMPLATE_U = 77;
    public static final int MACHINE_TEMPLATE_V = 87;
    public static final int MACHINE_TEMPLATE_H = 50;

    private JeiNeiTextures() {
    }

    public static IDrawable createRecipeBackground(IGuiHelper guiHelper) {
        return guiHelper.createDrawable(GUI_NEI, 0, 0, RECIPE_WIDTH, RECIPE_HEIGHT);
    }

    public static IDrawable createItemSlotBackground(IGuiHelper guiHelper) {
        return guiHelper.createDrawable(GUI_NEI, SLOT_U, SLOT_V, SLOT_SIZE, SLOT_SIZE);
    }
}
