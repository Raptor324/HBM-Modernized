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
 * 1:1 {@code GUICore}: Feldstaerke links (8,17) und Hitzesaettigung rechts (152,17) als senkrechte Balken aus der Textur,
 * die beiden Katalysator-Tanks bei (26,17) und (134,17), Tooltips direkt an der Maus.
 */
public class GUIDFCCore extends AbstractContainerScreen<DFCCoreMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/dfc/gui_core.png");

    public GUIDFCCore(DFCCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Original getFieldScaled(52) / getHeatScaled(52): Wert * 52 / 100
        int i = menu.getField() * 52 / 100;
        guiGraphics.blit(TEXTURE, leftPos + 8, topPos + 69 - i, 176, 52 - i, 16, i);

        int j = menu.getHeat() * 52 / 100;
        guiGraphics.blit(TEXTURE, leftPos + 152, topPos + 69 - j, 192, 52 - j, 16, j);

        var tanks = menu.getBlockEntity().getTanks();
        tanks[0].renderTank(guiGraphics, leftPos + 26, topPos + 69 - 52, 16, 52);
        tanks[1].renderTank(guiGraphics, leftPos + 134, topPos + 69 - 52, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        var tanks = menu.getBlockEntity().getTanks();
        tanks[0].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + 26, topPos + 17, 16, 52);
        tanks[1].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + 134, topPos + 17, 16, 52);

        if (isOver(8, 16, 17, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(Component.literal("Restriction Field: " + menu.getField() + "%")), mouseX, mouseY);
        }
        if (isOver(152, 16, 17, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(Component.literal("Heat Saturation: " + menu.getHeat() + "%")), mouseX, mouseY);
        }
    }

    private boolean isOver(int x, int w, int y, int h, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }
}
