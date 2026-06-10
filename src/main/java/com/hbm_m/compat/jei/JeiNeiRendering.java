package com.hbm_m.compat.jei;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws NEI-style slot frames on top of the shared {@link JeiNeiTextures#GUI_NEI} background.
 */
public final class JeiNeiRendering {

    private JeiNeiRendering() {
    }

    public static void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(JeiNeiTextures.GUI_NEI, x, y, u, v, width, height,
                JeiNeiTextures.TEXTURE_WIDTH, JeiNeiTextures.TEXTURE_HEIGHT);
    }

    public static void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        blit(graphics, x - 1, y - 1, JeiNeiTextures.SLOT_U, JeiNeiTextures.SLOT_V,
                JeiNeiTextures.SLOT_SIZE, JeiNeiTextures.SLOT_SIZE);
    }

    public static void drawGenericSlotFrames(GuiGraphics graphics, int inputCount, int outputCount,
            int inputXOffset, int outputXOffset) {
        for (int[] pos : JeiNeiLayout.getGenericInputSlotPositions(inputCount)) {
            drawSlotFrame(graphics, pos[0] + inputXOffset, pos[1]);
        }
        for (int[] pos : JeiNeiLayout.getGenericOutputSlotPositions(outputCount)) {
            drawSlotFrame(graphics, pos[0] + outputXOffset, pos[1]);
        }
    }

    public static void drawUniversalSlotFrames(GuiGraphics graphics, int inputCount, int outputCount) {
        for (int[] pos : JeiNeiLayout.getUniversalInputCoords(inputCount)) {
            drawSlotFrame(graphics, pos[0], pos[1]);
        }
        for (int[] pos : JeiNeiLayout.getUniversalOutputCoords(outputCount)) {
            drawSlotFrame(graphics, pos[0], pos[1]);
        }
    }

    public static void drawMachineSlot(GuiGraphics graphics, int machineXOffset, boolean hasTemplate) {
        if (hasTemplate) {
            blit(graphics, 74 + machineXOffset, 7,
                    JeiNeiTextures.MACHINE_TEMPLATE_U, JeiNeiTextures.MACHINE_TEMPLATE_V,
                    JeiNeiTextures.MACHINE_W, JeiNeiTextures.MACHINE_TEMPLATE_H);
        } else {
            blit(graphics, 74 + machineXOffset, 14,
                    JeiNeiTextures.MACHINE_U, JeiNeiTextures.MACHINE_V,
                    JeiNeiTextures.MACHINE_W, JeiNeiTextures.MACHINE_H);
        }
    }

    public static String formatShortNumber(long value) {
        double res;
        String suffix;

        if (Math.abs(value) >= 1_000_000_000_000_000_000L) {
            res = value / 1_000_000_000_000_000_000.0;
            suffix = "E";
        } else if (Math.abs(value) >= 1_000_000_000_000_000L) {
            res = value / 1_000_000_000_000_000.0;
            suffix = "P";
        } else if (Math.abs(value) >= 1_000_000_000_000L) {
            res = value / 1_000_000_000_000.0;
            suffix = "T";
        } else if (Math.abs(value) >= 1_000_000_000L) {
            res = value / 1_000_000_000.0;
            suffix = "G";
        } else if (Math.abs(value) >= 1_000_000L) {
            res = value / 1_000_000.0;
            suffix = "M";
        } else if (Math.abs(value) >= 1_000L) {
            res = value / 1_000.0;
            suffix = "k";
        } else {
            return Long.toString(value);
        }

        if (res <= -100.0) {
            res = Math.round(res * 10.0) / 10.0;
        } else {
            res = Math.round(res * 100.0) / 100.0;
        }

        return res + suffix;
    }

    public static void drawGenericRecipeExtras(GuiGraphics graphics, int duration, long powerPerTick) {
        String durationText = formatShortNumber(duration) + " ticks";
        String consumptionText = formatShortNumber(powerPerTick) + "HE/t";
        int side = 164;
        int color = 0x404040;

        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                durationText, side - net.minecraft.client.Minecraft.getInstance().font.width(durationText), 45, color, false);
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                consumptionText, side - net.minecraft.client.Minecraft.getInstance().font.width(consumptionText), 57, color, false);
    }
}
