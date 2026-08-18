package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.MachineCyclotronBlockEntity;
import com.hbm_m.inventory.menu.MachineCyclotronMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineCyclotron extends GuiInfoScreen<MachineCyclotronMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_cyclotron.png");

    private final MachineCyclotronBlockEntity cyclotron;

    public GUIMachineCyclotron(MachineCyclotronMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.cyclotron = menu.getBlockEntity();
        this.imageWidth = 190;
        this.imageHeight = 215;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int powerBarHeight = getPowerScaled(63);
        if (powerBarHeight > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 168, this.topPos + 80 - powerBarHeight, 190, 62 - powerBarHeight, 16, powerBarHeight);
        }

        int progressWidth = cyclotron.getProgressScaled(34);
        if (progressWidth > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 48, this.topPos + 27, 206, 0, progressWidth, 34);
            guiGraphics.blit(TEXTURE, this.leftPos + 172, this.topPos + 4, 190, 63, 9, 12);
        }

        drawInfoPanel(guiGraphics, 49, 85, PanelType.SMALL_BLUE_INFO);

        var tanks = cyclotron.getAllTanks();
        tanks[0].renderTank(guiGraphics, this.leftPos + 11, this.topPos + 88, 34, 7, 1);
        tanks[1].renderTank(guiGraphics, this.leftPos + 11, this.topPos + 97, 34, 7, 1);
        tanks[2].renderTank(guiGraphics, this.leftPos + 107, this.topPos + 97, 34, 16, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                168, 18, 16, 63,
                cyclotron.getEnergyStored(), cyclotron.getMaxEnergyStored());

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                this.leftPos + 49, this.topPos + 85, 8, 8,
                mouseX, mouseY,
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.effectiveness"),
                Component.translatable("desc.gui.upgrade.power"));

        var tanks = cyclotron.getAllTanks();
        tanks[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 11, this.topPos + 81, 34, 7);
        tanks[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 11, this.topPos + 90, 34, 7);
        tanks[2].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 107, this.topPos + 81, 34, 16);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private int getPowerScaled(int scale) {
        long max = Math.max(cyclotron.getMaxEnergyStored(), 1L);
        long stored = Math.max(0L, cyclotron.getEnergyStored());
        return (int) Math.min(scale, stored * scale / max);
    }
}