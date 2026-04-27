package com.hbm_m.interfaces;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;


/**
 * Crosshair HUD when looking at a block
 */
public interface ILookOverlay {

    void printHook(GuiGraphics guiGraphics, Level level, BlockPos pos);

    static void printGeneric(GuiGraphics guiGraphics, Component title, int titleCol, int bgCol, List<Component> text) {
        Minecraft mc = Minecraft.getInstance();
        Options options = mc.options;
        if (!options.getCameraType().isFirstPerson()) {
            return;
        }
        if (options.hideGui) {
            return;
        }
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }

        int pX = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int pZ = mc.getWindow().getGuiScaledHeight() / 2;
        Font font = mc.font;

        guiGraphics.drawString(font, title.getString(), pX + 1, pZ - 9, bgCol, false);
        guiGraphics.drawString(font, title.getString(), pX, pZ - 10, titleCol, false);

        for (Component line : text) {
            guiGraphics.drawString(font, line, pX, pZ, 0xFFFFFF, false);
            pZ += 10;
        }
    }
}
