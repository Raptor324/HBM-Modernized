package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineKeyforgeMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1-Port von {@code GUIMachineKeyForge} (1.7.10 Original): weisser Titel, zwei Info-Panels links. */
public class GUIMachineKeyforge extends GuiInfoScreen<MachineKeyforgeMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_keyforge.png");

    public GUIMachineKeyforge(MachineKeyforgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        this.drawInfoPanel(guiGraphics, 12, 28, PanelType.LARGE_BLUE_INFO);
        this.drawInfoPanel(guiGraphics, 12, 28 + 16, PanelType.LARGE_GREEN_INFO);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0xffffff, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 12, 28, 16, 16, leftPos - 8, topPos + 36 + 16,
                resolveKeyArray("desc.gui.keyforge.key"));
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 12, 28 + 16, 16, 16, leftPos - 8, topPos + 36 + 16,
                resolveKeyArray("desc.gui.keyforge.random"));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
