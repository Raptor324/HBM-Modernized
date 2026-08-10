package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.MachineCombinationOvenBlockEntity;
import com.hbm_m.inventory.menu.MachineCombinationOvenMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineCombinationOven extends GuiInfoScreen<MachineCombinationOvenMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_furnace_combination.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_furnace_combination.png");
            //?}

    // Fluessigkeitstank - rechts im Panel (Koordinaten aus dem 1.7.10-Original, GUIFurnaceCombo:
    // renderTankInfo/renderTank bei guiLeft + 118, guiTop + 18/70, 16x52).
    private static final int TANK_X = 118;
    private static final int TANK_Y = 18;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 52;

    // Fortschrittsbalken - Texturregion aus dem 1.7.10-Original (u=176,v=0, max 38x5), an
    // Position (guiLeft + 45, guiTop + 37).
    private static final int PROGRESS_X = 45;
    private static final int PROGRESS_Y = 37;
    private static final int PROGRESS_MAX_WIDTH = 38;
    private static final int PROGRESS_HEIGHT = 5;
    private static final int PROGRESS_TEX_U = 176;
    private static final int PROGRESS_TEX_V = 0;

    public GUIMachineCombinationOven(MachineCombinationOvenMenu menu, Inventory inventory, Component title) {
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
        renderTank(guiGraphics, x, y);
    }

    private void renderTank(GuiGraphics guiGraphics, int x, int y) {
        MachineCombinationOvenBlockEntity be = menu.getBlockEntity();
        be.getTank().renderTank(guiGraphics, x + TANK_X, y + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
    }

    private void renderProgress(GuiGraphics guiGraphics, int x, int y) {
        MachineCombinationOvenBlockEntity be = menu.getBlockEntity();
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
        MachineCombinationOvenBlockEntity be = menu.getBlockEntity();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isPointInRect(TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            be.getTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, x + TANK_X, y + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        }
    }
}
