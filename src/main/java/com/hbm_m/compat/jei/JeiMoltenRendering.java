package com.hbm_m.compat.jei;

//? if forge || neoforge {
import com.hbm_m.inventory.material.MaterialStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Расплавленный материал — не предмет и не жидкость, JEI-ингредиента для него нет.
 * Рисуем цветной квадрат из {@code MaterialType.color} и подпись с количеством,
 * как это делает сам тигель в своём GUI.
 */
public final class JeiMoltenRendering {

    private JeiMoltenRendering() {}

    public static final int SWATCH = 16;

    public static void drawSwatch(GuiGraphics graphics, int x, int y, MaterialStack stack) {
        int argb = 0xFF000000 | (stack.type.color & 0xFFFFFF);
        graphics.fill(x, y, x + SWATCH, y + SWATCH, argb);
        graphics.renderOutline(x - 1, y - 1, SWATCH + 2, SWATCH + 2, 0xFF373737);
    }

    /** Подпись «1000 mB Steel» рядом со свотчем. */
    public static void drawLabel(GuiGraphics graphics, int x, int y, MaterialStack stack) {
        String text = stack.amount + " mB " + stack.type.name;
        graphics.drawString(Minecraft.getInstance().font, text, x, y, 0x404040, false);
    }
}
//?}
