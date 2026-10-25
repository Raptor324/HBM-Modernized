package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.albion.PADetectorBlockEntity;
import com.hbm_m.inventory.menu.PADetectorMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUIPADetector}: Lampen fuer Kuehlung (43, 18) und Energie (43, 43). */
public class GUIPADetector extends GUIPABase<PADetectorMenu> {

    public GUIPADetector(PADetectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_detector.png", 8, 134, 136);
    }

    @Override
    protected void renderIndicators(GuiGraphics g, int mouseX, int mouseY) {
        if (heatOk()) g.blit(texture, leftPos + 43, topPos + 18, 176, 8, 8, 8);
        if (be().getEnergyStored() >= PADetectorBlockEntity.getUsage()) g.blit(texture, leftPos + 43, topPos + 43, 176, 8, 8, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        drawTitle(g, -8, 5, 0xffffff);
        super.renderLabels(g, mouseX, mouseY);
    }
}
