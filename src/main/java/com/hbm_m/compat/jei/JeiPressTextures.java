package com.hbm_m.compat.jei;

import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * NEI-Press-Texturkoordinaten, 1:1 zum Original-Handler {@code PressRecipeHandler}:
 *
 * <ul>
 *   <li>Die Textur {@code gui_nei_press.png} ist eine byte-identische Kopie der
 *       1.7.10-Original-Assets; NEIs Standard-{@code drawBackground} schneidet daraus
 *       {@code (5,11,166,65)} und zeichnet es bei {@code (0,0)} — Slot-Rahmen sind im
 *       Backpanel eingebacken ({@code (47,5)}, {@code (47,41)}, {@code (110,23)} auf
 *       Screen-Ebene), NEI zeichnet keine eigenen Slot-Drawables.</li>
 *   <li>NEI {@code drawExtras}: {@code drawProgressBar(47, 24, 0, 86, 18, 18, 20, 1)} —
 *       Fortschritts-Sprite aus der Textur bei {@code (0,86)}, 20 Ticks nach unten.</li>
 * </ul>
 */
public final class JeiPressTextures {

    public static final ResourceLocation GUI_NEI_PRESS =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_press.png");

    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    public static final int RECIPE_WIDTH = 166;
    public static final int RECIPE_HEIGHT = 65;

    /** NEI-Standard-{@code drawBackground}: {@code drawTexturedModalRect(0, 0, 5, 11, 166, 65)}. */
    public static final int BACKGROUND_U = 5;
    public static final int BACKGROUND_V = 11;

    public static final int PROGRESS_U = 0;
    public static final int PROGRESS_V = 86;
    public static final int PROGRESS_W = 18;
    public static final int PROGRESS_H = 18;

    /** NEI {@code drawExtras} position — {@code drawProgressBar(47, 24, 0, 86, 18, 18, 20, 1)}. */
    public static final int PROGRESS_X = 47;
    public static final int PROGRESS_Y = 24;

    private JeiPressTextures() {
    }

    public static IDrawable createRecipeBackground(IGuiHelper guiHelper) {
        return guiHelper.createDrawable(GUI_NEI_PRESS, BACKGROUND_U, BACKGROUND_V, RECIPE_WIDTH, RECIPE_HEIGHT);
    }
}
