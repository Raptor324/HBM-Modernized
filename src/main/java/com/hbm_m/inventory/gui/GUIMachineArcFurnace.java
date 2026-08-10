package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity;
import com.hbm_m.inventory.menu.MachineArcFurnaceMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Eigenes, kompaktes Einzelblock-GUI-Layout (das Original {@code GUIMachineArcFurnaceLarge} ist
 * fuer den Multiblock und dient nur als grobe visuelle Inspiration - siehe Klassenkommentar in
 * {@link MachineArcFurnaceBlockEntity}). Zwei generische Fluid-Ausgabetanks nebeneinander rechts
 * im Panel, Fortschrittsbalken analog zum Combination Oven.
 */
public class GUIMachineArcFurnace extends GuiInfoScreen<MachineArcFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_arc_furnace.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_arc_furnace.png");
            //?}

    // Zwei generische Fluid-Ausgabetanks, nebeneinander rechts im Panel (eigenes Layout).
    private static final int TANK1_X = 116;
    private static final int TANK1_Y = 18;
    private static final int TANK2_X = 136;
    private static final int TANK2_Y = 18;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 52;

    // Fortschrittsbalken - Texturregion analog Combination Oven (u=176,v=0, max 38x5).
    private static final int PROGRESS_X = 45;
    private static final int PROGRESS_Y = 37;
    private static final int PROGRESS_MAX_WIDTH = 38;
    private static final int PROGRESS_HEIGHT = 5;
    private static final int PROGRESS_TEX_U = 176;
    private static final int PROGRESS_TEX_V = 0;

    public GUIMachineArcFurnace(MachineArcFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderProgress(guiGraphics, x, y);
        renderTanks(guiGraphics, x, y);
    }

    private void renderTanks(GuiGraphics guiGraphics, int x, int y) {
        MachineArcFurnaceBlockEntity be = menu.getBlockEntity();
        be.getTank1().renderTank(guiGraphics, x + TANK1_X, y + TANK1_Y, TANK_WIDTH, TANK_HEIGHT);
        be.getTank2().renderTank(guiGraphics, x + TANK2_X, y + TANK2_Y, TANK_WIDTH, TANK_HEIGHT);
    }

    private void renderProgress(GuiGraphics guiGraphics, int x, int y) {
        MachineArcFurnaceBlockEntity be = menu.getBlockEntity();
        int maxProgress = be.getMaxProgress();
        if (maxProgress <= 0) return;
        int width = be.getProgressScaled(PROGRESS_MAX_WIDTH);
        if (width <= 0) return;

        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_TEX_U, PROGRESS_TEX_V, width, PROGRESS_HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MachineArcFurnaceBlockEntity be = menu.getBlockEntity();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isPointInRect(TANK1_X, TANK1_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            be.getTank1().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, x + TANK1_X, y + TANK1_Y, TANK_WIDTH, TANK_HEIGHT);
        }
        if (isPointInRect(TANK2_X, TANK2_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            be.getTank2().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, x + TANK2_X, y + TANK2_Y, TANK_WIDTH, TANK_HEIGHT);
        }
    }
}
