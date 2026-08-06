package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity;
import com.hbm_m.inventory.menu.MachineElectrolyserMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI des Elektrolyseurs - beide Modi (Fluid + Metall) in einem Fenster, Fortschritts-/
 *  Fuellstandsanzeigen als Fuellrechtecke (siehe {@code GUIMachineElectricFurnace}). */
public class GUIMachineElectrolyser extends AbstractContainerScreen<MachineElectrolyserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_electrolyser.png");

    private final MachineElectrolyserBlockEntity blockEntity;

    public GUIMachineElectrolyser(MachineElectrolyserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        int fluidProgress = blockEntity.getProgressFluidScaled(16);
        if (fluidProgress > 0) {
            guiGraphics.fill(x + 8, y + 34 - fluidProgress, x + 24, y + 34, 0xFF3080FF);
        }

        int metalProgress = blockEntity.getProgressMetalScaled(30);
        if (metalProgress > 0) {
            guiGraphics.fill(x + 116, y + 36, x + 116 + metalProgress, y + 44, 0xFFC0C0C0);
        }

        if (blockEntity.getEnergyStored() > 0) {
            guiGraphics.fill(x + 8, y + 71, x + 20, y + 87, 0xFFFFD030);
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
