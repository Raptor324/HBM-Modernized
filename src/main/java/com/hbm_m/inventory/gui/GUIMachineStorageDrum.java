package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineStorageDrumBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineStorageDrumMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIStorageDrum}: achteckiges Lager, links der Fluessig-, rechts der Gasabfall als Pegel (106 px hoch).
 */
public class GUIMachineStorageDrum extends GuiInfoScreen<MachineStorageDrumMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_drum.png");

    private final MachineStorageDrumBlockEntity drum;

    public GUIMachineStorageDrum(MachineStorageDrumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.drum = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 234;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (drum != null) {
            drum.getLiquidTank().renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 16, topPos + 23, 9, 108);
            drum.getGasTank().renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 151, topPos + 23, 9, 108);
        }

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
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (drum == null) return;

        int liquid = drum.getLiquidTank().getFill() * 106 / drum.getLiquidTank().getMaxFill();
        g.blit(TEXTURE, leftPos + 17, topPos + 130 - liquid, 176, 106 - liquid, 7, liquid);

        int gas = drum.getGasTank().getFill() * 106 / drum.getGasTank().getMaxFill();
        g.blit(TEXTURE, leftPos + 152, topPos + 130 - gas, 183, 106 - gas, 7, gas);
    }
}
