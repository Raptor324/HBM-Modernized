package com.hbm_m.interfaces;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RenderGuiEvent;


/**
 * Crosshair HUD when looking at a block
 */
public interface ILookOverlay {

    void printHook(RenderGuiEvent.Pre event, Level level, BlockPos pos);

    static void printGeneric(RenderGuiEvent.Pre event, Component title, int titleCol, int bgCol, List<Component> text) {
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
        var graphics = event.getGuiGraphics();

        graphics.drawString(font, title.getString(), pX + 1, pZ - 9, bgCol, false);
        graphics.drawString(font, title.getString(), pX, pZ - 10, titleCol, false);

        for (Component line : text) {
            graphics.drawString(font, line, pX, pZ, 0xFFFFFF, false);
            pZ += 10;
        }
    }
}
