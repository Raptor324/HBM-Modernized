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
    /** Original skaliert auf 51 ("was 50"). */
    private static final int BAR_HEIGHT = 51;
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

        // Original: drawInfoPanel(guiLeft - 12, guiTop + 25, 16, 16, 2)
        this.drawInfoPanel(guiGraphics, -12, 25, PanelType.LARGE_BLUE_INFO);
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
        // Original: Titel bei 60 - Breite/2, y=7, Farbe 10925486
        guiGraphics.drawString(this.font, name, 60 - this.font.width(name) / 2, 7, 10925486, false);
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

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                POWER_X, 9, BAR_WIDTH, 51,
                menu.getEnergyLong(), menu.getMaxEnergyLong());

        // Original: drawCustomInfoStat(guiLeft + 124, guiTop + 9, 16, 51, heatText)
        String[] heat = Component.translatable("desc.gui.rtg.heat", menu.getHeat()).getString().split("\\$");
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, HEAT_X, 9, BAR_WIDTH, 51, mouseX, mouseY,
                java.util.Arrays.stream(heat).map(Component::literal).toArray(Component[]::new));

        // Original: Liste aller Pellets mit Leistung (Waerme * 5)
        java.util.List<com.hbm_m.item.machine.ItemRTGPellet> pellets = com.hbm_m.item.machine.ItemRTGPellet.PELLETS;
        Component[] pelletText = new Component[pellets.size() + 1];
        pelletText[0] = Component.translatable("desc.gui.rtg.pellets");
        for (int i = 0; i < pellets.size(); i++) {
            com.hbm_m.item.machine.ItemRTGPellet pellet = pellets.get(i);
            pelletText[i + 1] = Component.translatable("desc.gui.rtg.pelletPower",
                    Component.translatable(pellet.getDescriptionId()), pellet.getHeat() * 5);
        }
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, -12, 25, 16, 16,
                this.leftPos - 8, this.topPos + 36 + 16, pelletText);
    }
}
