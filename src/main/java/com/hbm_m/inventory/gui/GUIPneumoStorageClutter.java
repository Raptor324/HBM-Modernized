package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.network.pneumatic.PneumoTubeBlockEntity;
import com.hbm_m.inventory.menu.PneumoStorageClutterMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.PneumoStorageControlC2SPacket;
import com.hbm_m.network.PneumoStorageControlC2SPacket.Control;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 1:1-Port von {@code GUIPneumoStorageClutter} (1.7.10), 200x235.
 *
 * <p>Sechs Reihen zu neun Plaetzen. Rechts oben die runde Druckluftanzeige bei (184, 25), darunter
 * der Druckstufenregler bei (174, 36): ein Klick darauf schaltet die Stufe weiter und damit die
 * Reichweite, in der Terminals dieses Lager noch sehen.</p>
 */
public class GUIPneumoStorageClutter extends AbstractContainerScreen<PneumoStorageClutterMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/storage/gui_pneumatic_clutter.png");

    /** Original: der Regler ist 20 mal 8 gross und liegt bei (174, 36). */
    private static final int SLIDER_X = 174;
    private static final int SLIDER_Y = 36;
    private static final int SLIDER_W = 20;
    private static final int SLIDER_H = 8;
    /** Original: die runde Druckluftanzeige sitzt bei (184, 25). */
    private static final int GAUGE_X = 184;
    private static final int GAUGE_Y = 25;

    public GUIPneumoStorageClutter(PneumoStorageClutterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 235;
        // Original: der Name wird auf 176 zentriert, nicht auf die volle Breite von 200.
        this.titleLabelX = 176 / 2 - font.width(title) / 2;
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        var tank = menu.getBlockEntity().getTank();

        // Regler: vier Bildpunkte je Druckstufe nach rechts.
        guiGraphics.blit(TEXTURE, leftPos + SLIDER_X + 4 * (tank.getPressure() - 1), topPos + SLIDER_Y,
                200, 0, 4, 8);

        double fill = tank.getMaxFill() > 0 ? (double) tank.getFill() / tank.getMaxFill() : 0D;
        GuiGaugeNeedle.draw(guiGraphics, leftPos + GAUGE_X, topPos + GAUGE_Y, fill,
                5, 2, 1, 0xCA6C43, 0xAB4223);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isOverSlider(mouseX, mouseY)) {
            int pressure = menu.getBlockEntity().getTank().getPressure();
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.hbm_m.pneumo.pressure", pressure),
                    Component.translatable("gui.hbm_m.pneumo.range",
                            PneumoTubeBlockEntity.getRangeFromPressure(pressure)),
                    Component.translatable("gui.hbm_m.pneumo.pressure.hint")),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOverSlider((int) mouseX, (int) mouseY)) {
            PneumoStorageControlC2SPacket.send(menu.getBlockEntity().getBlockPos(), Control.PRESSURE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOverSlider(int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= SLIDER_X && localX < SLIDER_X + SLIDER_W
                && localY >= SLIDER_Y && localY < SLIDER_Y + SLIDER_H;
    }
}
