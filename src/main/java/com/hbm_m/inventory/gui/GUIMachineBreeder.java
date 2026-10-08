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

        // Original: Fortschrittsbalken (53,32) aus (176,0), 70 px breit, 20 hoch
        int i = menu.getProgressScaled(70);
        guiGraphics.blit(TEXTURE, this.leftPos + 53, this.topPos + 32, 176, 0, i, 20);

        this.drawInfoPanel(guiGraphics, -16, 16, PanelType.LARGE_GREEN_INFO);
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

        // Original: Info-Panel links (fester englischer Text)
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, -16, 16, 16, 16, this.leftPos - 8, this.topPos + 16 + 16,
                Component.literal("The reactor has to recieve"),
                Component.literal("neutron flux from adjacent"),
                Component.literal("research reactors to breed."));
    }
}
