package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineVacuumDistillBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineVacuumDistillMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Direktport der Slot-/Tank-Koordinaten aus {@code GUIMachineVacuumDistill} (1.7.10 Original). */
public class GUIMachineVacuumDistill extends GuiInfoScreen<MachineVacuumDistillMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_vacuum_distill.png");

    private final MachineVacuumDistillBlockEntity be;

    private static final int[] TANK_X = { 44, 80, 98, 116, 134 };
    private static final int TANK_Y = 18;
    private static final int TANK_W = 16;
    private static final int TANK_H = 52;
    private static final int ENERGY_X = 26;

    public GUIMachineVacuumDistill(MachineVacuumDistillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.be = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 238;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        long power = be.getEnergyStored();
        long maxPower = Math.max(1L, be.getMaxEnergyStored());
        int j = (int) (power * 54 / maxPower);
        if (j > 0) {
            guiGraphics.blit(TEXTURE, leftPos + ENERGY_X, topPos + TANK_Y + TANK_H - j, 176, TANK_H - j, TANK_W, j);
        }

        var tanks = be.getTanks();
        for (int i = 0; i < TANK_X.length; i++) {
            tanks[i].renderTank(guiGraphics, leftPos + TANK_X[i], topPos + TANK_Y, TANK_W, TANK_H);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 5, 0xFFFFFF, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawElectricityInfo(guiGraphics, mouseX, mouseY, ENERGY_X, TANK_Y, TANK_W, TANK_H, be.getEnergyStored(), be.getMaxEnergyStored());

        var tanks = be.getTanks();
        for (int i = 0; i < TANK_X.length; i++) {
            if (isPointInRect(TANK_X[i], TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
                tanks[i].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_X[i], topPos + TANK_Y, TANK_W, TANK_H);
            }
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
