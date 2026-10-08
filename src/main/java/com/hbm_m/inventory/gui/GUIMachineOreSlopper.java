package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineOreSlopperMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIOreSlopper}: Wasser- (26,18) und Schlammtank (116,18), Energiebalken (8,18), Fortschritt als
 * steigender Pegel (62,52), Betriebslampe (12,4) sobald genug Strom fuer einen Tick da ist.
 */
public class GUIMachineOreSlopper extends GuiInfoScreen<MachineOreSlopperMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_ore_slopper.png");

    private final MachineOreSlopperBlockEntity slopper;

    public GUIMachineOreSlopper(MachineOreSlopperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.slopper = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (slopper != null) {
            slopper.getWaterTank().renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 26, topPos + 18, 34, 52);
            slopper.getSlopTank().renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 116, topPos + 18, 16, 52);
            drawElectricityInfo(g, mouseX, mouseY, 8, 18, 16, 52, slopper.getEnergyStored(), MachineOreSlopperBlockEntity.maxPower);
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2 - 9, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (slopper == null) return;

        int i = (int) (slopper.progress * 35);
        g.blit(TEXTURE, leftPos + 62, topPos + 52 - i, 176, 34 - i, 34, i);

        int j = (int) (slopper.getEnergyStored() * 52 / MachineOreSlopperBlockEntity.maxPower);
        g.blit(TEXTURE, leftPos + 8, topPos + 70 - j, 176, 86 - j, 16, j);

        if (slopper.getEnergyStored() >= slopper.consumption)
            g.blit(TEXTURE, leftPos + 12, topPos + 4, 202, 34, 9, 12);

        slopper.getWaterTank().renderTank(g, leftPos + 26, topPos + 18, 16, 52);
        slopper.getSlopTank().renderTank(g, leftPos + 116, topPos + 18, 16, 52);
    }
}
