package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.PAMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Gemeinsame Oberflaeche der Bauteile des Teilchenbeschleunigers.
 *
 * <p>Alle fuenf teilen sich Aufbau und Groesse (176 x 204, Spielerinventar ab Zeile 122) und
 * unterscheiden sich nur in ihrer Hintergrundtextur. Gemeinsam ist auch die Temperaturanzeige -
 * jedes Bauteil muss unter -150 Grad Celsius kommen, sonst reisst der Strahl ab.</p>
 */
public class GUIPABase<T extends PAMenu> extends AbstractContainerScreen<T> {

    private final ResourceLocation texture;

    public GUIPABase(T menu, Inventory playerInventory, Component title, String textureName) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.texture = ResourceLocation.fromNamespaceAndPath(
                RefStrings.MODID, "textures/gui/particleaccelerator/" + textureName);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // Temperatur in Grad Celsius, gruen sobald das Bauteil betriebsbereit ist.
        float celsius = menu.getTemperature() - 273F;
        int color = menu.isCool() ? 0x08FF00 : 0xFF5555;
        guiGraphics.drawString(this.font,
                Component.translatable("gui.hbm_m.pa.temperature", String.format("%.0f", celsius)),
                8, 96, color, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /** Kleiner Helfer fuer die Zustandszeile der Quelle. */
    protected void drawStatus(GuiGraphics guiGraphics, int x, int y, Component text, int color) {
        guiGraphics.drawString(this.font, text.copy().withStyle(ChatFormatting.RESET), x, y, color, false);
    }
}
