package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineICFMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 1:1-Port von {@code GUIICF} (1.7.10), 248x222.
 *
 * <p>Ganz links der Laserbalken, daneben der Kuehlmitteltank, rechts der heisse Ruecklauf und der
 * Sternenfluss - alle vier 16 breit und 70 hoch. Rechts unten die Hitzeanzeige.</p>
 *
 * <p>Die Hitze zeigt die runde Anzeige bei (196, 98) - derselbe Zeiger wie im Original.</p>
 */
public class GUIMachineICF extends AbstractContainerScreen<MachineICFMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/reactors/gui_icf.png");

    /** Original: alle Balken sind 16 breit, 70 hoch und beginnen auf Hoehe 18. */
    private static final int BAR_TOP = 18;
    private static final int BAR_HEIGHT = 70;
    private static final int BAR_BOTTOM = BAR_TOP + BAR_HEIGHT;

    private static final int LASER_X = 8;
    private static final int TANK_COLD_X = 44;
    private static final int TANK_HOT_X = 188;
    private static final int TANK_FLUX_X = 224;

    public GUIMachineICF(MachineICFMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 248;
        this.imageHeight = 222;
        this.titleLabelY = 6;
        this.inventoryLabelX = 44;
        this.inventoryLabelY = this.imageHeight - 93;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Original: der obere Teil und der Inventarteil kommen aus zwei Ausschnitten.
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, 114);
        guiGraphics.blit(TEXTURE, leftPos + 36, topPos + 122, 36, 122, 176, 108);

        int laser = menu.getLaserPermille() * BAR_HEIGHT / 1000;
        if (laser > 0) {
            guiGraphics.blit(TEXTURE, leftPos + LASER_X, topPos + BAR_BOTTOM - laser,
                    212, 192 - laser, 16, laser);
        }

        // Original: drawSmoothGauge(guiLeft + 196, guiTop + 98, ..., 5, 2, 1, 0xFF00AF)
        GuiGaugeNeedle.draw(guiGraphics, leftPos + 196, topPos + 98,
                menu.getHeatPermille() / 1000D, 5, 2, 1, 0xFF00AF);

        var tanks = menu.getBlockEntity().getTanks();
        tanks[0].renderTank(guiGraphics, leftPos + TANK_COLD_X, topPos + BAR_TOP, 16, BAR_HEIGHT);
        tanks[1].renderTank(guiGraphics, leftPos + TANK_HOT_X, topPos + BAR_TOP, 16, BAR_HEIGHT);
        tanks[2].renderTank(guiGraphics, leftPos + TANK_FLUX_X, topPos + BAR_TOP, 16, BAR_HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        var tanks = menu.getBlockEntity().getTanks();
        tanks[0].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_COLD_X, topPos + BAR_TOP, 16, BAR_HEIGHT);
        tanks[1].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_HOT_X, topPos + BAR_TOP, 16, BAR_HEIGHT);
        tanks[2].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_FLUX_X, topPos + BAR_TOP, 16, BAR_HEIGHT);

        if (isOver(LASER_X, 16, BAR_TOP, BAR_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    menu.getLaserPermille() <= 0
                            ? Component.translatable("gui.hbm_m.icf.offline")
                            : Component.translatable("gui.hbm_m.icf.laser",
                                    String.format(java.util.Locale.US, "%.1f", menu.getLaserPermille() / 10D))),
                    mouseX, mouseY);
        }

        if (isOver(187, 18, 89, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.hbm_m.icf.heat",
                            String.format(java.util.Locale.US, "%.1f", menu.getHeatPermille() / 10D))),
                    mouseX, mouseY);
        }
    }

    private boolean isOver(int x, int w, int y, int h, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }
}
