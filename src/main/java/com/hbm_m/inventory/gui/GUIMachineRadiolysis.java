package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineRadiolysisBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineRadiolysisMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI des Radiolyse-Kollektors - Fuellstands-/Energieanzeigen als Fuellrechtecke (siehe
 *  {@code GUIMachineElectricFurnace}). */
public class GUIMachineRadiolysis extends AbstractContainerScreen<MachineRadiolysisMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_radiolysis.png");

    private final MachineRadiolysisBlockEntity blockEntity;

    public GUIMachineRadiolysis(MachineRadiolysisMenu menu, Inventory inventory, Component title) {
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
        // тайл может отсутствовать в реплее Flashback
        if (blockEntity == null) return;

        var tanks = blockEntity.getTanks();
        int inW = tanks[0].getMaxFill() > 0 ? tanks[0].getFill() * 16 / tanks[0].getMaxFill() : 0;
        if (inW > 0) guiGraphics.fill(x + 34, y + 18 + (16 - inW), x + 50, y + 34, 0xFF3080FF);

        int out1H = tanks[1].getMaxFill() > 0 ? tanks[1].getFill() * 34 / tanks[1].getMaxFill() : 0;
        if (out1H > 0) guiGraphics.fill(x + 60, y + 18 + (34 - out1H), x + 76, y + 52, 0xFF30FF80);

        int out2H = tanks[2].getMaxFill() > 0 ? tanks[2].getFill() * 34 / tanks[2].getMaxFill() : 0;
        if (out2H > 0) guiGraphics.fill(x + 90, y + 18 + (34 - out2H), x + 106, y + 52, 0xFFFFA030);

        if (blockEntity.getEnergyStored() > 0) {
            guiGraphics.fill(x + 130, y + 18, x + 142, y + 50, 0xFF3080FF);
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
