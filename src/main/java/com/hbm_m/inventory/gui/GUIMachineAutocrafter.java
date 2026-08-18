package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineAutocrafterBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineAutocrafterMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur (nur der statische Rahmen, keine geratenen Icon-
 * Koordinaten daraus) - Energieanzeige als eigenes Fuellrechteck-Overlay. Die Vorlage-/Filter-
 * Raster-Anzeige des Originals entfaellt (siehe Klassenkommentar in
 * {@link MachineAutocrafterBlockEntity}).
 */
public class GUIMachineAutocrafter extends AbstractContainerScreen<MachineAutocrafterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_autocrafter.png");

    private final MachineAutocrafterBlockEntity blockEntity;

    public GUIMachineAutocrafter(MachineAutocrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        long energy = blockEntity.getEnergyStored();
        long maxEnergy = blockEntity.getMaxEnergyStored();
        if (energy > 0 && maxEnergy > 0) {
            int h = (int) (energy * 34L / maxEnergy);
            guiGraphics.fill(x + 17, y + 71 - h, x + 29, y + 71, 0xFF3080FF);
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
