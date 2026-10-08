package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.albion.CooledMachineBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.PAMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Gemeinsame Oberflaeche der Bauteile des Teilchenbeschleunigers - 1:1 nach den fuenf Original-GUIs
 * ({@code GUIPASource}, {@code GUIPADetector}, {@code GUIPADipole}, {@code GUIPAQuadrupole}, {@code GUIPARFC}).
 *
 * <p>Alle teilen Aufbau und Groesse (176 x 204, Spielerinventar ab Zeile 122), den Energiebalken
 * (Sprite 184/52), die beiden Kuehlmitteltanks (Unterkante 88, 16 x 52) und die Temperaturanzeige
 * "/123K" mit dem rechtsbuendigen Istwert darueber. Je Bauteil verschieden sind nur die Positionen.</p>
 */
public class GUIPABase<T extends PAMenu> extends GuiInfoScreen<T> {

    protected final ResourceLocation texture;
    private final int powerX;
    private final int tankX;
    private final int tempX;

    /**
     * @param powerX x des Energiebalkens
     * @param tankX  x des ersten Kuehlmitteltanks (der zweite liegt 18 rechts daneben)
     * @param tempX  x der Beschriftung "/123K" (der Istwert endet 30 weiter rechts)
     */
    public GUIPABase(T menu, Inventory playerInventory, Component title, String textureName, int powerX, int tankX, int tempX) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.powerX = powerX;
        this.tankX = tankX;
        this.tempX = tempX;
        this.texture = ResourceLocation.fromNamespaceAndPath(
                RefStrings.MODID, "textures/gui/particleaccelerator/" + textureName);
    }

    protected CooledMachineBlockEntity be() {
        return menu.getBlockEntity();
    }

    /** Original: {@code (int) Math.ceil(temperature) <= 123}. */
    protected boolean heatOk() {
        return (int) Math.ceil(be().getTemperature()) <= 123;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        long max = be().getMaxEnergyStored();
        int j = max > 0 ? (int) (be().getEnergyStored() * 52 / max) : 0;
        g.blit(texture, leftPos + powerX, topPos + 70 - j, 184, 52 - j, 16, j);

        renderIndicators(g, mouseX, mouseY);

        // Original: renderTank(guiLeft + x, guiTop + 88, ..., 16, 52) - Port-renderTank erwartet die Oberkante
        FluidTank[] tanks = be().getCoolantTanks();
        tanks[0].renderTank(g, leftPos + tankX, topPos + 88 - 52, 16, 52);
        tanks[1].renderTank(g, leftPos + tankX + 18, topPos + 88 - 52, 16, 52);
    }

    /** Bauteilspezifische Anzeigen (Bereitschaftslampen, Spulenbild ...). */
    protected void renderIndicators(GuiGraphics g, int mouseX, int mouseY) { }

    /** Titelzeile; RFC zeigt im Original keinen Titel. */
    protected void drawTitle(GuiGraphics g, int xOffset, int y, int color) {
        String name = this.title.getString();
        g.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2 + xOffset, y, color, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);

        g.drawString(this.font, ChatFormatting.AQUA + "/123K", tempX, 22, 4210752, false);
        int heat = (int) Math.ceil(be().getTemperature());
        String label = (heat > 123 ? ChatFormatting.RED : ChatFormatting.AQUA) + "" + heat + "K";
        g.drawString(this.font, label, tempX + 30 - this.font.width(label), 12, 4210752, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        FluidTank[] tanks = be().getCoolantTanks();
        tanks[0].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + tankX, topPos + 36, 16, 52);
        tanks[1].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + tankX + 18, topPos + 36, 16, 52);
        this.drawElectricityInfo(g, mouseX, mouseY, powerX, 18, 16, 52, be().getEnergyStored(), be().getMaxEnergyStored());

        renderExtraTooltips(g, mouseX, mouseY);
        this.renderTooltip(g, mouseX, mouseY);
    }

    protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) { }
}
