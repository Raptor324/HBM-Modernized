package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineStorageDrumBlockEntity;
import com.hbm_m.inventory.menu.MachineStorageDrumMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Simple GUI for the Storage Drum - no dedicated original texture found, uses a plain background
 * with two colored fluid-level bars (liquid/gas waste tanks) instead.
 */
public class GUIMachineStorageDrum extends AbstractContainerScreen<MachineStorageDrumMenu> {

    private final MachineStorageDrumBlockEntity drum;

    public GUIMachineStorageDrum(MachineStorageDrumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.drum = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF0303030);

        int liquidPct = drum.getLiquidTank().getMaxFill() > 0
                ? drum.getLiquidTank().getFill() * 40 / drum.getLiquidTank().getMaxFill() : 0;
        int gasPct = drum.getGasTank().getMaxFill() > 0
                ? drum.getGasTank().getFill() * 40 / drum.getGasTank().getMaxFill() : 0;

        guiGraphics.fill(leftPos + 152, topPos + 17 + (40 - liquidPct), lexRight(), topPos + 57, 0xFF544400);
        guiGraphics.fill(leftPos + 160, topPos + 17 + (40 - gasPct), leftPos + 168, topPos + 57, 0xFFB8B8B8);
    }

    private int lexRight() { return leftPos + 160; }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
