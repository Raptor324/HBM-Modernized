package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachinePUREXBlockEntity;
import com.hbm_m.inventory.menu.MachinePUREXMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur (nur der statische Rahmen, keine geratenen Icon-
 * Koordinaten daraus) - Fortschritt/Tanks/Energie als eigene Fuellrechteck-Overlays. Die
 * durchsuchbare Rezeptauswahl-Liste des Originals entfaellt (siehe Klassenkommentar in
 * {@link MachinePUREXBlockEntity}).
 */
public class GUIMachinePUREX extends AbstractContainerScreen<MachinePUREXMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_purex.png");

    private final MachinePUREXBlockEntity blockEntity;

    public GUIMachinePUREX(MachinePUREXMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 220;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (blockEntity.isActive()) {
            int w = blockEntity.getProgressScaled(24);
            guiGraphics.fill(x + 45, y + 40, x + 45 + w, y + 48, 0xFFC0C0C0);
        }

        for (int i = 0; i < 3; i++) {
            var tank = blockEntity.getInputTank(i);
            int h = tank.getMaxFill() > 0 ? tank.getFill() * 40 / tank.getMaxFill() : 0;
            if (h > 0) guiGraphics.fill(x + 10 + i * 12, y + 20 + (40 - h), x + 20 + i * 12, y + 60, 0xFF3080FF);
        }

        var outTank = blockEntity.getOutputTank();
        int outH = outTank.getMaxFill() > 0 ? outTank.getFill() * 40 / outTank.getMaxFill() : 0;
        if (outH > 0) guiGraphics.fill(x + 152, y + 20 + (40 - outH), x + 164, y + 60, 0xFF30FF80);

        if (blockEntity.getEnergyStored() > 0) {
            guiGraphics.fill(x + 17, y + 71, x + 29, y + 105, 0xFFFFA030);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
