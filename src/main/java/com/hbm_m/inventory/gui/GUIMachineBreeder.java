package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineBreederMenu;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Direct port of the layout baked into {@code gui_breeder.png}: input slot -> vertical flux bar
 * -> output slot, standard 176x166 canvas. Coordinates below were reverse-engineered pixel-by-pixel
 * from the texture itself (input slot at 35,35; output at 125,35; flux bar at 73,19, 30x37) - the
 * previous version of this class assumed a much richer battery/fluid-tank/upgrade-slot layout that
 * has no art anywhere in this texture and was removed (see {@code MachineBreederBlockEntity}).
 */
public class GUIMachineBreeder extends GuiInfoScreen<MachineBreederMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_breeder.png");

    private static final int FLUX_X = 73;
    private static final int FLUX_Y = 19;
    private static final int FLUX_WIDTH = 30;
    private static final int FLUX_HEIGHT = 37;

    public GUIMachineBreeder(MachineBreederMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // The flux bar is baked fully "lit" into the background art; cover the unfilled top
        // portion with the panel's own background color so it fills bottom-up with progress,
        // matching vanilla's furnace-flame convention.
        int filled = menu.getProgressScaled(FLUX_HEIGHT);
        int unfilled = FLUX_HEIGHT - filled;
        if (unfilled > 0) {
            guiGraphics.fill(this.leftPos + FLUX_X, this.topPos + FLUX_Y,
                    this.leftPos + FLUX_X + FLUX_WIDTH, this.topPos + FLUX_Y + unfilled, 0xFFC6C6C6);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);

        // Original: der anliegende Fluss steht gruen mittig ueber der Anzeige.
        String flux = String.valueOf(menu.getFlux());
        guiGraphics.drawString(this.font, flux, 88 - this.font.width(flux) / 2, 21, 0x08FF00, false);
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

        // Original: ein Hinweisfeld statt einer Energieanzeige - der Brutreaktor braucht keinen Strom.
        if (mouseX >= this.leftPos + FLUX_X && mouseX < this.leftPos + FLUX_X + FLUX_WIDTH
                && mouseY >= this.topPos + FLUX_Y && mouseY < this.topPos + FLUX_Y + FLUX_HEIGHT) {
            guiGraphics.renderComponentTooltip(this.font,
                    java.util.List.of(
                            net.minecraft.network.chat.Component.translatable("gui.hbm_m.breeder.flux", menu.getFlux()),
                            net.minecraft.network.chat.Component.translatable("gui.hbm_m.breeder.hint")),
                    mouseX, mouseY);
        }
    }
}
