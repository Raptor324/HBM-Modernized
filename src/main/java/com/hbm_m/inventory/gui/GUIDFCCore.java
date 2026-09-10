package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.DFCCoreMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 1:1-Port von {@code GUICore} (1.7.10), 176x166.
 *
 * <p>Links der Deuteriumtank, rechts der Tritiumtank, in der Mitte die drei Plaetze. Darueber die
 * beiden Balken, auf die es ankommt: <b>Feld</b> und <b>Hitze</b>. Solange das Feld ueber der
 * Hitze liegt, ist alles gut - kippt es, sprengt sich der Kern im selben Tick.</p>
 *
 * <p>Der Farbstreifen unter den Plaetzen zeigt die Mischfarbe beider Katalysatoren. Ist er
 * schwarz, fehlt einer - dann laeuft der Kern gar nicht erst an.</p>
 */
public class GUIDFCCore extends AbstractContainerScreen<DFCCoreMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/dfc/gui_core.png");

    private static final int TANK_TOP = 17;
    private static final int TANK_H = 52;
    private static final int TANK_A_X = 8;
    private static final int TANK_B_X = 152;

    /** Original: {@code getFieldScaled}/{@code getHeatScaled} rechnen gegen 100. */
    private static final int BAR_SCALE = 100;
    private static final int BAR_W = 52;

    public GUIDFCCore(DFCCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        var tanks = menu.getBlockEntity().getTanks();
        tanks[0].renderTank(guiGraphics, leftPos + TANK_A_X, topPos + TANK_TOP, 16, TANK_H);
        tanks[1].renderTank(guiGraphics, leftPos + TANK_B_X, topPos + TANK_TOP, 16, TANK_H);

        // Feld und Hitze als waagerechte Balken - im Original zwei Texturausschnitte.
        int field = Math.min(BAR_W, menu.getField() * BAR_W / BAR_SCALE);
        int heat = Math.min(BAR_W, menu.getHeat() * BAR_W / BAR_SCALE);

        if (field > 0) {
            guiGraphics.fill(leftPos + 62, topPos + 22, leftPos + 62 + field, topPos + 28, 0xFF3FA8E8);
        }
        if (heat > 0) {
            guiGraphics.fill(leftPos + 62, topPos + 32, leftPos + 62 + heat, topPos + 38, 0xFFE85A3F);
        }

        // Die Mischfarbe der Katalysatoren.
        int color = menu.getColor();
        if (color != 0) {
            guiGraphics.fill(leftPos + 62, topPos + 44, leftPos + 114, topPos + 48, 0xFF000000 | color);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        var tanks = menu.getBlockEntity().getTanks();
        tanks[0].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_A_X, topPos + TANK_TOP, 16, TANK_H);
        tanks[1].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_B_X, topPos + TANK_TOP, 16, TANK_H);

        if (isOver(62, BAR_W, 22, 6, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.hbm_m.dfc.field", menu.getField())), mouseX, mouseY);
        }
        if (isOver(62, BAR_W, 32, 6, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.hbm_m.dfc.heat", menu.getHeat()),
                    Component.translatable("gui.hbm_m.dfc.heat.warning")), mouseX, mouseY);
        }
        if (isOver(62, BAR_W, 44, 4, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    menu.getColor() == 0
                            ? Component.translatable("gui.hbm_m.dfc.color.missing")
                            : Component.translatable("gui.hbm_m.dfc.consumption", menu.getConsumption())),
                    mouseX, mouseY);
        }
    }

    private boolean isOver(int x, int w, int y, int h, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }
}
