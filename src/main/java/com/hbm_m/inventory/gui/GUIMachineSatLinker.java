package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineSatLinkerMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the "Satellite ID Manager" - 1:1-Port von {@code GUIMachineSatLinker} (1.7.10):
 * 176x186, zwei Info-Panels links (Chip kopieren / Zufallsfrequenz).
 */
public class GUIMachineSatLinker extends GuiInfoScreen<MachineSatLinkerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_sat_linker.png");

    public GUIMachineSatLinker(MachineSatLinkerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        this.drawInfoPanel(guiGraphics, 12, 28, PanelType.LARGE_BLUE_INFO);
        this.drawInfoPanel(guiGraphics, 12, 28 + 16, PanelType.LARGE_GREEN_INFO);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 12, 28, 16, 16, leftPos + 20, topPos + 28 + 16,
                resolveKeyArray("desc.gui.satlinker.chip"));
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 12, 28 + 16, 16, 16, leftPos + 20, topPos + 28 + 32,
                resolveKeyArray("desc.gui.satlinker.random"));

        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
