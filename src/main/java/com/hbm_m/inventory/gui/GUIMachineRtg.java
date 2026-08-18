package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineRtgBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineRtgMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Port of {@code GUIMachineRTG} (1.7.10 Original). */
public class GUIMachineRtg extends AbstractContainerScreen<MachineRtgMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_rtg.png");

    private final MachineRtgBlockEntity rtg;

    public GUIMachineRtg(MachineRtgMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.rtg = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 183;
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

        String heat = "Heat: " + rtg.getHeat() + " / " + rtg.getHeatMax();
        guiGraphics.drawString(this.font, heat, this.leftPos + 8, this.topPos + 74, 0x404040, false);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
