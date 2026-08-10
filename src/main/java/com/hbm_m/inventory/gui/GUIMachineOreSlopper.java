package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity;
import com.hbm_m.inventory.menu.MachineOreSlopperMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineOreSlopper extends GuiInfoScreen<MachineOreSlopperMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_ore_slopper.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_ore_slopper.png");
            //?}

    // Wassertank - links im Panel (vermessen anhand der Textur, siehe gui_ore_slopper.png).
    private static final int TANK_X = 8;
    private static final int TANK_Y = 8;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 80;

    public GUIMachineOreSlopper(MachineOreSlopperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = 112;
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

        renderTank(guiGraphics, x, y);
        renderProgress(guiGraphics, x, y);
    }

    private void renderTank(GuiGraphics guiGraphics, int x, int y) {
        MachineOreSlopperBlockEntity be = menu.getBlockEntity();
        be.getTank().renderTank(guiGraphics, x + TANK_X, y + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
    }

    private void renderProgress(GuiGraphics guiGraphics, int x, int y) {
        MachineOreSlopperBlockEntity be = menu.getBlockEntity();
        int progressPct = (be.getMaxProgress() <= 0) ? 0 : be.getProgressScaled(100);
        if (progressPct > 0) {
            guiGraphics.drawString(this.font, progressPct + "%", x + 100, y + 60, 4210752, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);

        MachineOreSlopperBlockEntity be = menu.getBlockEntity();
        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                152, 18, 16, 16,
                be.getEnergyStored(), be.getMaxEnergyStored());

        renderTooltip(guiGraphics, mouseX, mouseY);
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MachineOreSlopperBlockEntity be = menu.getBlockEntity();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isPointInRect(TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            be.getTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, x + TANK_X, y + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        }
    }
}
