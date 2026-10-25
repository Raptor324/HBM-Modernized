package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineRtgFurnaceBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineRtgFurnaceMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIRtgFurnace}: Flamme, sobald ein Pellet eingelegt ist, und der Fortschrittspfeil (24 px auf 1000).
 */
public class GUIRtgFurnace extends AbstractContainerScreen<MachineRtgFurnaceMenu> {

    private static final ResourceLocation texture =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/rtg_furnace.png");

    private final MachineRtgFurnaceBlockEntity diFurnace;

    public GUIRtgFurnace(MachineRtgFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.diFurnace = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (diFurnace == null) return;

        if (diFurnace.hasPower()) {
            g.blit(texture, leftPos + 55, topPos + 35, 176, 0, 18, 16);
        }

        int j1 = diFurnace.getDiFurnaceProgressScaled(24);
        g.blit(texture, leftPos + 79, topPos + 34, 176, 16, j1 + 1, 17);
    }
}
