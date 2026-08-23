package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineSolidifierBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineSolidifierMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineSolidifier extends GuiInfoScreen<MachineSolidifierMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_solidifier.png");

    private final MachineSolidifierBlockEntity solidifier;

    public GUIMachineSolidifier(MachineSolidifierMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.solidifier = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // тайл может отсутствовать в реплее Flashback
        if (solidifier == null) return;

        int power = (int) (solidifier.getEnergyStored() * 52L / Math.max(1L, solidifier.getMaxEnergyStored()));
        guiGraphics.blit(TEXTURE, this.leftPos + 134, this.topPos + 70 - power, 176, 52 - power, 16, power);

        int maxProgress = Math.max(1, solidifier.getProcessTime());
        int j = solidifier.getProgress() * 42 / maxProgress;
        guiGraphics.blit(TEXTURE, this.leftPos + 42, this.topPos + 17, 192, 0, j, 35);

        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 138, this.topPos + 4, 176, 52, 9, 12);
        }

        solidifier.getTank().renderTank(guiGraphics, this.leftPos + 35, this.topPos + 88, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, 70 - this.font.width(title) / 2, 6, 0xC7C1A3, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // тайл может отсутствовать в реплее Flashback
        if (solidifier != null) {
        solidifier.getTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 35, this.topPos + 36, 16, 52);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                134, 18, 16, 52,
                solidifier.getEnergyStored(), solidifier.getMaxEnergyStored());
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
