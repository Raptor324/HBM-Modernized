package com.hbm_m.compat.jei;

import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * NEI-style anvil JEI texture coordinates (1:1 with 1.7.10 {@code gui_nei_anvil.png}).
 */
public final class JeiAnvilTextures {

    public static final ResourceLocation GUI_NEI_ANVIL =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_anvil.png");

    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    public static final int RECIPE_WIDTH = 166;
    public static final int RECIPE_HEIGHT = 65;

    public static final int SLOT_U = 5;
    public static final int SLOT_V = 87;
    public static final int SLOT_SIZE = 18;

    private JeiAnvilTextures() {
    }

    public static IDrawable createRecipeBackground(IGuiHelper guiHelper) {
        // NEI TemplateRecipeHandler.drawBackground (1.7.10-1.0.3.74):
        // drawTexturedModalRect(0, 0, 5, 11, 166, 65) — окно панели в текстуре лежит в (5,11).
        return guiHelper.createDrawable(GUI_NEI_ANVIL, 5, 11, RECIPE_WIDTH, RECIPE_HEIGHT);
    }

    public static IDrawable createItemSlotBackground(IGuiHelper guiHelper) {
        return guiHelper.createDrawable(GUI_NEI_ANVIL, SLOT_U, SLOT_V, SLOT_SIZE, SLOT_SIZE);
    }
}
