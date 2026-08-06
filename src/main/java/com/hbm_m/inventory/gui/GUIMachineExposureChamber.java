package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity;
import com.hbm_m.inventory.menu.MachineExposureChamberMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI der Belichtungskammer - Fortschritts-/Partikelvorrats-/Energieanzeigen als Fuellrechtecke
 *  (siehe {@code GUIMachineElectricFurnace}). */
public class GUIMachineExposureChamber extends AbstractContainerScreen<MachineExposureChamberMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_exposure_chamber.png");

    private final MachineExposureChamberBlockEntity blockEntity;

    public GUIMachineExposureChamber(MachineExposureChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        int progress = blockEntity.getProgressScaled(24);
        if (progress > 0) {
            guiGraphics.fill(x + 96, y + 40, x + 96 + progress, y + 48, 0xFFC0C0C0);
        }

        int particles = blockEntity.getParticlesScaled(16);
        if (particles > 0) {
            guiGraphics.fill(x + 26, y + 34 - particles, x + 36, y + 34, 0xFF30D0FF);
        }

        if (blockEntity.getEnergyStored() > 0) {
            guiGraphics.fill(x + 156, y + 46, x + 168, y + 78, 0xFF3080FF);
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
