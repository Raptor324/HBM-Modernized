package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineMiningLaserMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIMiningLaser}: Energiesaeule, Abbaufortschritt, An/Aus-Knopf mit Schachtbreite darueber, Oeltank und
 * das Info-Feld mit den zulaessigen Upgrades.
 */
public class GUIMachineMiningLaser extends GuiInfoScreen<MachineMiningLaserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_laser_miner.png");

    private final MachineMiningLaserBlockEntity laser;

    public GUIMachineMiningLaser(MachineMiningLaserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.laser = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (laser != null) {
            drawElectricityInfo(g, mouseX, mouseY, 8, 106 - 88, 16, 88, laser.getEnergyStored(), MachineMiningLaserBlockEntity.maxPower);

            drawCustomInfoStat(g, mouseX, mouseY, 87, 31, 8, 8, leftPos + 141, topPos + 39 + 16,
                    Component.literal("Acceptable upgrades:"),
                    Component.literal(" -Speed (stacks to level 12)"),
                    Component.literal(" -Effectiveness (stacks to level 12)"),
                    Component.literal(" -Overdrive (stacks to level 3)"),
                    Component.literal(" -Fortune (stacks to level 3)"),
                    Component.literal(" -Smelter (exclusive)"),
                    Component.literal(" -Shredder (exclusive)"),
                    Component.literal(" -Centrifuge (exclusive)"),
                    Component.literal(" -Crystallizer (exclusive)"),
                    Component.literal(" -Nullifier"));

            laser.tank.renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 35, topPos + 124 - 52, 7, 52);
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean handled = super.mouseClicked(x, y, button);
        if (laser != null && leftPos + 61 <= x && leftPos + 61 + 18 > x && topPos + 17 < y && topPos + 17 + 18 >= y) {
            playClickSound();
            com.hbm_m.network.MiningLaserToggleC2SPacket.send(laser.getBlockPos());
            return true;
        }
        return handled;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 4, 4210752, false);
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
        if (laser != null) {
            String width = "" + laser.getWidth();
            g.drawString(font, width, 43 - font.width(width) / 2, 26, 0xffffff, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (laser == null) return; // тайл может отсутствовать в реплее Flashback

        if (laser.isOn)
            g.blit(TEXTURE, leftPos + 61, topPos + 17, 200, 0, 18, 18);

        int i = (int) laser.getPowerScaled(88);
        g.blit(TEXTURE, leftPos + 8, topPos + 106 - i, 176, 88 - i, 16, i);

        int j = laser.getProgressScaled(34);
        g.blit(TEXTURE, leftPos + 66, topPos + 36, 192, 0, 8, j);

        drawInfoPanel(g, 87, 31, PanelType.SMALL_BLUE_STAR);

        laser.tank.renderTank(g, leftPos + 35, topPos + 72, 7, 52);
    }
}
