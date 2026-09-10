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
        // Original: 230x166 - die Oberflaeche ist breiter, weil rechts die Pelletplaetze sitzen.
        this.imageWidth = 230;
        this.imageHeight = 166;
        this.inventoryLabelY = imageHeight - 96 + 2;
        this.titleLabelX = 88;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        // тайл может отсутствовать в реплее Flashback
        if (blockEntity == null) return;

        var tanks = blockEntity.getTanks();

        // 1:1: der Energiebalken sitzt bei (8, 17), 16 breit, 34 hoch - von unten gefuellt.
        int i = blockEntity.getMaxEnergyStored() > 0
                ? (int) (blockEntity.getEnergyStored() * 34L / blockEntity.getMaxEnergyStored()) : 0;
        if (i > 0) guiGraphics.blit(TEXTURE, x + 8, y + 51 - i, 240, 34 - i, 16, i);

        // Original: der Eingangstank schmal bei (61, 17), die beiden Ausgaben bei (87, 17/53).
        tanks[0].renderTank(guiGraphics, x + 61, y + 17, 8, 52);
        tanks[1].renderTank(guiGraphics, x + 87, y + 17, 12, 16);
        tanks[2].renderTank(guiGraphics, x + 87, y + 53, 12, 16);
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
