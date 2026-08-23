package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineCombustionEngineMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur (nur der statische Rahmen, keine geratenen Icon-
 * Koordinaten daraus) - Tank-/Energieanzeigen als eigene Fuellrechteck-Overlays. Zuend-Knopf und
 * Drossel-Schieberegler des Originals entfallen (siehe Klassenkommentar in
 * {@link MachineCombustionEngineBlockEntity}).
 */
public class GUIMachineCombustionEngine extends AbstractContainerScreen<MachineCombustionEngineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_combustion.png");

    private final MachineCombustionEngineBlockEntity blockEntity;

    public GUIMachineCombustionEngine(MachineCombustionEngineMenu menu, Inventory inventory, Component title) {
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

        if (blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            var tank = blockEntity.getTank();
            int fuelH = tank.getMaxFill() > 0 ? tank.getFill() * 52 / tank.getMaxFill() : 0;
            if (fuelH > 0) guiGraphics.fill(x + 35, y + 17 + (52 - fuelH), x + 51, y + 69, 0xFF804000);

            long max = blockEntity.getMaxEnergyStored();
            long energy = blockEntity.getEnergyStored();
            int energyH = max > 0 ? (int) (energy * 52L / max) : 0;
            if (energyH > 0) guiGraphics.fill(x + 143, y + 17 + (52 - energyH), x + 159, y + 69, 0xFFFF3020);

            if (blockEntity.isActive()) {
                guiGraphics.fill(x + 88, y + 13, x + 105, y + 27, 0xFF30FF30);
            }
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
