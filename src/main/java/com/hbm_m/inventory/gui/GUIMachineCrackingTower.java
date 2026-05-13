package com.hbm_m.inventory.gui;

import com.hbm_m.block.entity.machines.MachineCrackingTowerBlockEntity;
import com.hbm_m.inventory.menu.MachineCrackingTowerMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineCrackingTower extends GuiInfoScreen<MachineCrackingTowerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_cracking_tower.png");

    private final MachineCrackingTowerBlockEntity tower;

    public GUIMachineCrackingTower(MachineCrackingTowerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.tower = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int power = (int) (tower.getEnergyStored() * 52L / Math.max(tower.getMaxEnergyStored(), 1L));
        if (power > 52) {
            power = 52;
        }
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 70 - power, 176, 52 - power, 16, power);
        }

        int progress = tower.getProgressScaled(33);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 72, this.topPos + 37, 192, 0, progress, 14);
        }

        if (tower.getEnergyStored() > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 156, this.topPos + 4, 176, 52, 9, 12);
        }

        drawInfoPanel(guiGraphics, 78, 67, PanelType.SMALL_BLUE_INFO);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
            152, 18, 16, 52,
            tower.getEnergyStored(), tower.getMaxEnergyStored());

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
            this.leftPos + 78, this.topPos + 67, 8, 8,
            this.leftPos + 78, this.topPos + 67,
                Component.literal("Progress:"),
                Component.literal("   " + tower.getProgress() + " / " + tower.getMaxProgress()));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}