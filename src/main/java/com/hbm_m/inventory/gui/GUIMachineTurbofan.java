package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineTurbofanMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIMachineTurbofan}: Treibstofftank 34x52 (35,17), Energiebalken (143,17), Nachbrenner-Anzeige (98,44)
 * und - sobald Blut angefallen ist - das kleine Rundinstrument {@code small_round} (13 Bilder) fuer den Bluttank.
 */
public class GUIMachineTurbofan extends GuiInfoScreen<MachineTurbofanMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_turbofan.png");
    private static final ResourceLocation GAUGE_SMALL_ROUND =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gauges/small_round.png");

    private final MachineTurbofanBlockEntity turbofan;

    public GUIMachineTurbofan(MachineTurbofanMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.turbofan = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 203;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (turbofan != null) {
            turbofan.tank.renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 35, topPos + 17, 34, 52);
            if (turbofan.showBlood) turbofan.blood.renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 98, topPos + 17, 16, 16);
            drawElectricityInfo(g, mouseX, mouseY, 143, 17, 16, 52, turbofan.getEnergyStored(), MachineTurbofanBlockEntity.maxPower);
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 43 - this.font.width(this.title) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (turbofan == null) return;

        int i = (int) turbofan.getPowerScaled(52);
        g.blit(TEXTURE, leftPos + 152 - 9, topPos + 69 - i, 176 + 16, 52 - i, 16, i);

        if (turbofan.afterburner > 0) {
            int a = Math.min(turbofan.afterburner, 6);
            g.blit(TEXTURE, leftPos + 98, topPos + 44, 176, (a - 1) * 16, 16, 16);
        }

        if (turbofan.showBlood) {
            // Original GUIElements.renderGauge(ROUND_SMALL, 97, 16): 18x18, 13 senkrecht gestapelte Bilder
            double progress = (double) turbofan.blood.getFill() / (double) turbofan.blood.getMaxFill();
            int frameNum = (int) Math.round((13 - 1) * progress);
            g.blit(GAUGE_SMALL_ROUND, leftPos + 97, topPos + 16, 18, 18, 0, frameNum * 18, 18, 18, 18, 18 * 13);
        }

        turbofan.tank.renderTank(g, leftPos + 35, topPos + 17, 34, 52);
    }
}
