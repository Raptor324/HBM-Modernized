package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachinePyroOvenBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachinePyroOvenMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachinePyroOven extends GuiInfoScreen<MachinePyroOvenMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_pyrooven.png");

    private final MachinePyroOvenBlockEntity pyro;

    public GUIMachinePyroOven(MachinePyroOvenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.pyro = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // тайл может отсутствовать в реплее Flashback
        if (pyro == null) return;
        int power = (int) (pyro.getEnergyStored() * 52L / Math.max(1L, pyro.getMaxEnergyStored()));
        guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 70 - power, 176, 64 - power, 16, power);

        int p = (int) (pyro.getProgress() * 27);
        guiGraphics.blit(TEXTURE, this.leftPos + 57, this.topPos + 47, 176, 0, p, 12);

        pyro.getTank0().renderTank(guiGraphics, this.leftPos + 8, this.topPos + 70, 16, 52);
        pyro.getTank1().renderTank(guiGraphics, this.leftPos + 116, this.topPos + 70, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2 - 18, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // тайл может отсутствовать в реплее Flashback
        if (pyro != null) {
        pyro.getTank0().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 8, this.topPos + 18, 16, 52);
        pyro.getTank1().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 116, this.topPos + 18, 16, 52);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                152, 18, 16, 52,
                pyro.getEnergyStored(), pyro.getMaxEnergyStored());
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
