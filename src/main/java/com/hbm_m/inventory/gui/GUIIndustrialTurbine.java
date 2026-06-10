package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineIndustrialTurbineMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIIndustrialTurbine extends GuiInfoScreen<MachineIndustrialTurbineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_turbine_large.png");

    public GUIIndustrialTurbine(MachineIndustrialTurbineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int steamFill = menu.getSteamAmount();
        int steamCap = Math.max(menu.getSteamCapacity(), 1);
        int steamHeight = Math.min(52, steamFill * 52 / steamCap);
        if (steamHeight > 0) {
            guiGraphics.fill(this.leftPos + 43, this.topPos + 69 - steamHeight, this.leftPos + 49, this.topPos + 69, 0xFF99D9EA);
        }

        int spentFill = menu.getSpentAmount();
        int spentCap = Math.max(menu.getSpentCapacity(), 1);
        int spentHeight = Math.min(52, spentFill * 52 / spentCap);
        if (spentHeight > 0) {
            guiGraphics.fill(this.leftPos + 123, this.topPos + 69 - spentHeight, this.leftPos + 129, this.topPos + 69, 0xFF778899);
        }

        long energy = menu.getEnergyStoredLong();
        long maxEnergy = Math.max(menu.getMaxEnergyStoredLong(), 1L);
        int energyHeight = (int) Math.min(52L, energy * 52L / maxEnergy);
        if (energyHeight > 0) {
            guiGraphics.fill(this.leftPos + 84, this.topPos + 69 - energyHeight, this.leftPos + 90, this.topPos + 69, 0xFFFF4D4D);
        }

        if (menu.isActive()) {
            drawInfoPanel(guiGraphics, 84, 8, PanelType.SMALL_GREEN_INFO);
        } else {
            drawInfoPanel(guiGraphics, 84, 8, PanelType.SMALL_RED_EXCLAMATION);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                43, 17, 6, 52,
                mouseX, mouseY,
                Component.literal("Steam: " + menu.getSteamAmount() + " / " + menu.getSteamCapacity() + " mB"));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                123, 17, 6, 52,
                mouseX, mouseY,
                Component.literal("Spent Steam: " + menu.getSpentAmount() + " / " + menu.getSpentCapacity() + " mB"));

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                84, 17, 6, 52,
                menu.getEnergyStoredLong(), menu.getMaxEnergyStoredLong());

        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}