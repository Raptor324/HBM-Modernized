package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineDieselGeneratorBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineDieselGeneratorMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur (nur der statische Rahmen, keine geratenen Icon-
 * Koordinaten daraus) - Tank-/Energieanzeigen als eigene Fuellrechteck-Overlays. Der An/Aus-Knopf
 * des Originals entfaellt (siehe Klassenkommentar in {@link MachineDieselGeneratorBlockEntity}).
 */
public class GUIMachineDieselGenerator extends AbstractContainerScreen<MachineDieselGeneratorMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/GUIDiesel.png");

    private final MachineDieselGeneratorBlockEntity blockEntity;

    public GUIMachineDieselGenerator(MachineDieselGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        var tank = blockEntity.getTank();
        int fuelH = tank.getMaxFill() > 0 ? tank.getFill() * 52 / tank.getMaxFill() : 0;
        if (fuelH > 0) guiGraphics.fill(x + 35, y + 69 + (52 - fuelH), x + 51, y + 121, 0xFF804000);

        long max = blockEntity.getMaxEnergyStored();
        long energy = blockEntity.getEnergyStored();
        int energyH = max > 0 ? (int) (energy * 52L / max) : 0;
        if (energyH > 0) guiGraphics.fill(x + 141, y + 69 + (52 - energyH), x + 157, y + 121, 0xFFFF3020);

        if (blockEntity.isActive()) {
            guiGraphics.fill(x + 89, y + 42, x + 97, y + 50, 0xFF30FF30);
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
