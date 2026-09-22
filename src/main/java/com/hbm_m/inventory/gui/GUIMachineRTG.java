package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineRTGMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI des RTG-Generators, 1:1 aus {@code GUIMachineRTG} (1.7.10): links das 3x5-Feld fuer die
 * Pellets, rechts zwei senkrechte Balken - Waerme bei x=124, Energie bei x=146, beide 52 Pixel
 * hoch und von unten nach oben gefuellt.
 */
public class GUIMachineRTG extends GuiInfoScreen<MachineRTGMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/gui_rtg.png");

    /** Original: {@code drawTexturedModalRect(guiLeft + 124, guiTop + 61 - i, 176, 10 + (51 - i), 16, i)}. */
    private static final int HEAT_X = 124;
    private static final int POWER_X = 146;
    private static final int BAR_BOTTOM = 61;
    private static final int BAR_WIDTH = 16;
    private static final int BAR_HEIGHT = 52;
    private static final int HEAT_U = 176;
    private static final int POWER_U = 192;
    private static final int BAR_V_BOTTOM = 10 + 51;

    public GUIMachineRTG(MachineRTGMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 188;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        drawBar(guiGraphics, HEAT_X, HEAT_U, menu.getHeatScaled(BAR_HEIGHT));
        drawBar(guiGraphics, POWER_X, POWER_U, menu.getPowerScaled(BAR_HEIGHT));
    }

    /** Fuellt von unten nach oben - genau die Rechnung des Originals. */
    private void drawBar(GuiGraphics guiGraphics, int x, int u, int filled) {
        if (filled <= 0) return;
        if (filled > BAR_HEIGHT) filled = BAR_HEIGHT;

        guiGraphics.blit(TEXTURE,
                this.leftPos + x, this.topPos + BAR_BOTTOM - filled,
                u, BAR_V_BOTTOM - filled,
                BAR_WIDTH, filled);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isOver(mouseX, mouseY, HEAT_X)) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.hbm_m.rtg.heat", menu.getHeat(), menu.getMaxHeat()),
                    mouseX, mouseY);
        }

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                POWER_X, BAR_BOTTOM - BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT,
                menu.getEnergyLong(), menu.getMaxEnergyLong());
    }

    private boolean isOver(int mouseX, int mouseY, int x) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + BAR_WIDTH
                && mouseY >= this.topPos + BAR_BOTTOM - BAR_HEIGHT && mouseY < this.topPos + BAR_BOTTOM;
    }
}
