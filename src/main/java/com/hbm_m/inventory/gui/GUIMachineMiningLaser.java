package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineMiningLaserMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur (nur der statische 176x222-Rahmen, keine
 * geratenen Icon-Koordinaten daraus) - Fortschritt/Energie/Tiefe als eigene Fuellrechteck-
 * Overlays, analog zu {@code GUIMachineCompressor}.
 */
public class GUIMachineMiningLaser extends AbstractContainerScreen<MachineMiningLaserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_laser_miner.png");

    private final MachineMiningLaserBlockEntity blockEntity;

    public GUIMachineMiningLaser(MachineMiningLaserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            int progress = blockEntity.getProgressScaled(24);
            if (progress > 0) {
                guiGraphics.fill(x + 76, y + 60, x + 76 + progress, y + 68, 0xFFFF3020);
            }

            if (blockEntity.getEnergyStored() > 0) {
                long max = blockEntity.getMaxEnergyStored();
                long ratio = max > 0 ? blockEntity.getEnergyStored() * 40L / max : 0;
                guiGraphics.fill(x + 12, y + 20 + (int) (40 - ratio), x + 20, y + 60, 0xFF3080FF);
            }

            if (blockEntity.isActive()) {
                guiGraphics.fill(x + 152, y + 20, x + 164, y + 32, 0xFFFF3020);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
        if (blockEntity != null) {
            guiGraphics.drawString(font, Component.translatable("gui.hbm_m.mining_laser.depth", blockEntity.getDrillDepth()),
                    12, imageHeight - 118, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
