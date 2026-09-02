package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.inventory.menu.HeatingOvenMenu;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI screen for the Heating Oven - a pure combustion source.
 * Displays a fuel slot, a burn-time bar and a TU/energy bar.
 */
public class GUIHeatingOven extends GuiInfoScreen<HeatingOvenMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/machine/gui_heating_oven.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/machine/gui_heating_oven.png");
            //?}

    private static final int BURN_BAR_X = 81;
    private static final int BURN_BAR_Y = 28;
    private static final int BURN_BAR_W = 68;
    private static final int BURN_BAR_H = 6;

    private static final int ENERGY_BAR_X = 81;
    private static final int ENERGY_BAR_Y = 37;
    private static final int ENERGY_BAR_W = 68;
    private static final int ENERGY_BAR_H = 5;

    private static final int FLAME_X = 16;
    private static final int FLAME_Y = 13;
    private static final int FLAME_W = 18;
    private static final int FLAME_H = 28;

    public GUIHeatingOven(HeatingOvenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (menu.isOn()) {
            guiGraphics.blit(GUI_TEXTURE, this.leftPos + FLAME_X, this.topPos + FLAME_Y, 176, 0, FLAME_W, FLAME_H);
        }

        // Burn-time bar
        int burnWidth = menu.getBurnTimeScaled(BURN_BAR_W);
        if (burnWidth > 0) {
            int x0 = this.leftPos + BURN_BAR_X;
            int y0 = this.topPos + BURN_BAR_Y;
            guiGraphics.fill(x0, y0, x0 + burnWidth, y0 + BURN_BAR_H, 0xFFFF8C00);
        }

        // TU/energy bar
        int energyWidth = menu.getEnergyScaled(ENERGY_BAR_W);
        if (energyWidth > 0) {
            int x0 = this.leftPos + ENERGY_BAR_X;
            int y0 = this.topPos + ENERGY_BAR_Y;
            guiGraphics.fill(x0, y0, x0 + energyWidth, y0 + ENERGY_BAR_H, 0xFF3FCFE0);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                BURN_BAR_X, BURN_BAR_Y, BURN_BAR_W, BURN_BAR_H,
                mouseX, mouseY,
                Component.translatable("gui.hbm_m.burn_time", menu.getBurnTimeScaled(100)));

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_W, ENERGY_BAR_H,
                menu.getEnergyStored(), menu.getMaxEnergyStored());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title,
                (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                8, this.imageHeight - 96 + 2, 0x404040, false);
    }
}
