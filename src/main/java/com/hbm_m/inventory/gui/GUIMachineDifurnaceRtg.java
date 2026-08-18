package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineDifurnaceRtgBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineDifurnaceRtgMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Port of {@code GUIMachineDiFurnaceRTG} (1.7.10 Original). */
public class GUIMachineDifurnaceRtg extends AbstractContainerScreen<MachineDifurnaceRtgMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_rtg_difurnace.png");

    private final MachineDifurnaceRtgBlockEntity difurnace;

    public GUIMachineDifurnaceRtg(MachineDifurnaceRtgMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.difurnace = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 184;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        String progress = "Progress: " + difurnace.getProgress() + " / " + difurnace.getMaxProgress();
        guiGraphics.drawString(this.font, progress, this.leftPos + 8, this.topPos + 90, 0x404040, false);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
