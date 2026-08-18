package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCompressorBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineCompressorMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI des Kompressors - Fortschritts-/Fuellstands-/Energieanzeigen als Fuellrechtecke (siehe
 *  {@code GUIMachineElectricFurnace}). */
public class GUIMachineCompressor extends AbstractContainerScreen<MachineCompressorMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_compressor.png");

    private final MachineCompressorBlockEntity blockEntity;

    public GUIMachineCompressor(MachineCompressorMenu menu, Inventory inventory, Component title) {
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

        int progress = blockEntity.getProgressScaled(24);
        if (progress > 0) {
            guiGraphics.fill(x + 76, y + 40, x + 76 + progress, y + 48, 0xFFC0C0C0);
        }

        var tanks = blockEntity.getTanks();
        int inFill = tanks[0].getMaxFill() > 0 ? tanks[0].getFill() * 52 / tanks[0].getMaxFill() : 0;
        if (inFill > 0) guiGraphics.fill(x + 17, y + 88 - inFill, x + 33, y + 88, 0xFF3080FF);

        int outFill = tanks[1].getMaxFill() > 0 ? tanks[1].getFill() * 52 / tanks[1].getMaxFill() : 0;
        if (outFill > 0) guiGraphics.fill(x + 134, y + 88 - outFill, x + 150, y + 88, 0xFFFFA030);

        if (blockEntity.getEnergyStored() > 0) {
            guiGraphics.fill(x + 152, y + 20, x + 164, y + 52, 0xFF3080FF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
