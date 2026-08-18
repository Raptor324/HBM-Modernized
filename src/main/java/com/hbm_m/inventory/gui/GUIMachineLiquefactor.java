package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineLiquefactorBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineLiquefactorMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Direktport der Slot-/Tank-Koordinaten aus {@code GUILiquefactor} (1.7.10 Original). Die
 *  Lade-Icon-Deko (176,52 / 9x12) des Originals wurde nicht uebernommen (rein kosmetisch). */
public class GUIMachineLiquefactor extends GuiInfoScreen<MachineLiquefactorMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_liquefactor.png");

    private final MachineLiquefactorBlockEntity be;

    private static final int TANK_X = 71;
    private static final int TANK_Y = 36;
    private static final int TANK_W = 16;
    private static final int TANK_H = 52;
    private static final int ENERGY_X = 134;
    private static final int ENERGY_Y = 18;
    private static final int PROGRESS_X = 42;
    private static final int PROGRESS_Y = 17;
    private static final int PROGRESS_MAX_WIDTH = 42;
    private static final int PROGRESS_HEIGHT = 35;

    public GUIMachineLiquefactor(MachineLiquefactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.be = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        long power = be.getEnergyStored();
        long maxPower = Math.max(1L, be.getMaxEnergyStored());
        int i = (int) (power * TANK_H / maxPower);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, leftPos + ENERGY_X, topPos + ENERGY_Y + TANK_H - i, 176, TANK_H - i, TANK_W, i);
        }

        int progressWidth = be.getProgressScaled(PROGRESS_MAX_WIDTH);
        if (progressWidth > 0) {
            guiGraphics.blit(TEXTURE, leftPos + PROGRESS_X, topPos + PROGRESS_Y, 192, 0, progressWidth, PROGRESS_HEIGHT);
        }

        be.getTank().renderTank(guiGraphics, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 70 - font.width(title) / 2, 6, 0xC7C1A3, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawElectricityInfo(guiGraphics, mouseX, mouseY, ENERGY_X, ENERGY_Y, TANK_W, TANK_H, be.getEnergyStored(), be.getMaxEnergyStored());

        if (isPointInRect(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            be.getTank().renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
