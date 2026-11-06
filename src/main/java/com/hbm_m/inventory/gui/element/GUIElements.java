package com.hbm_m.inventory.gui.element;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.inventory.fluid.FluidType;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;

/**
 * 1:1 port of 1.7.10 {@code com.hbm.inventory.gui.element.GUIElements} tooltip border logic.
 * <p>
 * In 1.7.10, hovering over fluid tanks or machine recipes rendered a tooltip with vibrant, custom-colored
 * borders (e.g. orange-to-yellow gradient for recipes, and a fluid-tint-derived gradient for tanks)
 * instead of the standard purple/blue vanilla tooltip border.
 */
public class GUIElements {

    public static final int STANDARD_COLOR_BACKGROUND = 0xF0100010;
    public static final int STANDARD_COLOR_LINE0 = 0x505000FF;
    public static final int STANDARD_COLOR_LINE1 = (STANDARD_COLOR_LINE0 & 0xFEFEFE) >> 1 | STANDARD_COLOR_LINE0 & 0xFF000000;
    public static final int RECIPE_COLOR_LINE0 = 0xFFFF8000;
    public static final int RECIPE_COLOR_LINE1 = 0xFFFFFF00;

    public static final ThreadLocal<int[]> ACTIVE_TOOLTIP_BORDER = new ThreadLocal<>();

    public static void drawHoveringText(GuiGraphics guiGraphics, Font font, List<? extends FormattedText> lines, int mouseX, int mouseY) {
        drawHoveringText(guiGraphics, font, lines, mouseX, mouseY, STANDARD_COLOR_LINE0, STANDARD_COLOR_LINE1);
    }

    public static void drawHoveringTextRecipe(GuiGraphics guiGraphics, Font font, List<? extends FormattedText> lines, int mouseX, int mouseY) {
        drawHoveringText(guiGraphics, font, lines, mouseX, mouseY, RECIPE_COLOR_LINE0, RECIPE_COLOR_LINE1);
    }

    public static void drawHoveringTextFluid(GuiGraphics guiGraphics, Font font, List<? extends FormattedText> lines, int mouseX, int mouseY, FluidType type) {
        int[] colors = getFluidBorderColors(type);
        drawHoveringText(guiGraphics, font, lines, mouseX, mouseY, colors[0], colors[1]);
    }

    public static int[] getFluidBorderColors(FluidType type) {
        if (type == null) {
            return new int[] { STANDARD_COLOR_LINE0, STANDARD_COLOR_LINE1 };
        }
        int color0 = type.getColor();
        int r = (color0 >> 16) & 255;
        int g = (color0 >> 8) & 255;
        int b = color0 & 255;
        int add = (r + g + b) / 3 > 0x80 ? -0x40 : 0x40;
        int color1 = ((Mth.clamp(r + add, 0, 255)) << 16)
                | ((Mth.clamp(g + add, 0, 255)) << 8)
                | (Mth.clamp(b + add, 0, 255));
        color0 |= 0xff000000;
        color1 |= 0xff000000;
        return new int[] { color0, color1 };
    }

    public static void drawHoveringText(GuiGraphics guiGraphics, Font font, List<? extends FormattedText> lines, int mouseX, int mouseY, int colLine0, int colLine1) {
        if (lines == null || lines.isEmpty()) return;
        List<Component> compList = new ArrayList<>(lines.size());
        for (FormattedText line : lines) {
            if (line instanceof Component c) {
                compList.add(c);
            } else if (line != null) {
                compList.add(Component.literal(line.getString()));
            }
        }
        try {
            ACTIVE_TOOLTIP_BORDER.set(new int[] { colLine0, colLine1 });
            guiGraphics.renderComponentTooltip(font, compList, mouseX, mouseY);
        } finally {
            ACTIVE_TOOLTIP_BORDER.remove();
        }
    }
}
