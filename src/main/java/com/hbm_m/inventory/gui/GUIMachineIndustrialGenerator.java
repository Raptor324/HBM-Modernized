package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineIndustrialGeneratorBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineIndustrialGeneratorMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Das Original hatte NIE ein GUI ({@code provideGUI()} gab {@code null} zurueck - der Block war
 * faktisch tot, siehe Klassenkommentar in {@link MachineIndustrialGeneratorBlockEntity}). Da keine
 * Original-Textur existiert, wird das Panel rein aus Fuellrechtecken gezeichnet statt eine Textur
 * zu erfinden.
 */
public class GUIMachineIndustrialGenerator extends AbstractContainerScreen<MachineIndustrialGeneratorMenu> {

    private final MachineIndustrialGeneratorBlockEntity blockEntity;

    public GUIMachineIndustrialGenerator(MachineIndustrialGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFF8B8B8B);

        for (int i = 0; i < 5; i++) {
            int slotX = i == 0 ? 17 : 62 + (i - 1) * 18;
            int slotY = i == 0 ? 71 : 36;
            guiGraphics.fill(x + slotX - 1, y + slotY - 1, x + slotX + 17, y + slotY + 17, 0xFF373737);
        }

        if (blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            if (blockEntity.isActive()) {
                int h = blockEntity.getBurnTimeScaled(14);
                guiGraphics.fill(x + 62, y + 55 - h, x + 74, y + 55, 0xFFFF8000);
            }

            int waterH = blockEntity.getWaterTank().getMaxFill() > 0
                    ? blockEntity.getWaterTank().getFill() * 34 / blockEntity.getWaterTank().getMaxFill() : 0;
            if (waterH > 0) guiGraphics.fill(x + 134, y + 18 + (34 - waterH), x + 146, y + 52, 0xFF3080FF);

            int lubeH = blockEntity.getLubricantTank().getMaxFill() > 0
                    ? blockEntity.getLubricantTank().getFill() * 34 / blockEntity.getLubricantTank().getMaxFill() : 0;
            if (lubeH > 0) guiGraphics.fill(x + 150, y + 18 + (34 - lubeH), x + 162, y + 52, 0xFFC0A000);

            int fuelH = blockEntity.getFuelTank().getMaxFill() > 0
                    ? blockEntity.getFuelTank().getFill() * 34 / blockEntity.getFuelTank().getMaxFill() : 0;
            if (fuelH > 0) guiGraphics.fill(x + 118, y + 18 + (34 - fuelH), x + 130, y + 52, 0xFF804000);

            if (blockEntity.getEnergyStored() > 0) {
                guiGraphics.fill(x + 17, y + 20, x + 29, y + 70, 0xFFFF3020);
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
