package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineLargePylonMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI для Large Pylon (WIP).
 * Использует текстуру кристаллизатора как заглушку — замените на gui_large_pylon.png,
 * когда текстура будет готова.
 */
public class GUIMachineLargePylon extends GuiInfoScreen<MachineLargePylonMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_crystallizer_alt.png");

    public GUIMachineLargePylon(MachineLargePylonMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        long maxEnergy = menu.getMaxEnergyLong();
        if (maxEnergy > 0) {
            int i = (int) menu.getBlockEntity().getPowerScaled(52);
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 70 - i, 176, 64 - i, 16, i);
        }

        int j = menu.getProgressScaled(28);
        if (j > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 47, 176, 0, j, 12);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                152, 18, 16, 52,
                menu.getEnergyLong(), menu.getMaxEnergyLong());
    }
}
