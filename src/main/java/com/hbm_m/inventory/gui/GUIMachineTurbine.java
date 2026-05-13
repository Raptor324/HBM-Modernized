package com.hbm_m.inventory.gui;

import com.hbm_m.block.entity.machines.MachineTurbineBlockEntity;
import com.hbm_m.inventory.menu.MachineTurbineMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineTurbine extends GuiInfoScreen<MachineTurbineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_turbine.png");

    private final MachineTurbineBlockEntity turbine;

    public GUIMachineTurbine(MachineTurbineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.turbine = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int power = (int) (turbine.getEnergyStored() * 52L / Math.max(turbine.getMaxEnergyStored(), 1L));
        if (power > 52) {
            power = 52;
        }
        if (power > 0) {
            guiGraphics.fill(this.leftPos + 152, this.topPos + 70 - power, this.leftPos + 158, this.topPos + 70, 0xFFFF4D4D);
        }

        int progress = turbine.getProgressScaled(33);
        if (progress > 0) {
            guiGraphics.fill(this.leftPos + 72, this.topPos + 37, this.leftPos + 72 + progress, this.topPos + 51, 0xFFB0E0E6);
        }

        if (turbine.isActive()) {
            drawInfoPanel(guiGraphics, 78, 58, PanelType.SMALL_GREEN_INFO);
        } else {
            drawInfoPanel(guiGraphics, 78, 58, PanelType.SMALL_RED_EXCLAMATION);
        }
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
                152, 18, 6, 52,
                turbine.getEnergyStored(), turbine.getMaxEnergyStored());

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                this.leftPos + 78, this.topPos + 58, 8, 8,
                this.leftPos + 78, this.topPos + 58,
                Component.literal("Progress:"),
                Component.literal("   " + turbine.getProgress() + " / " + turbine.getMaxProgress()));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
