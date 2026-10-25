package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineWasteDrumMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1-Port von {@code GUIWasteDrum} (1.7.10 Original): 176x189, Info-Panel links mit Kuehlhinweis. */
public class GUIMachineWasteDrum extends GuiInfoScreen<MachineWasteDrumMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_waste_drum.png");

    public GUIMachineWasteDrum(MachineWasteDrumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 189;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        this.drawInfoPanel(guiGraphics, -16, 36, PanelType.LARGE_BLUE_INFO);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 5, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, -16, 36, 16, 16, leftPos - 8, topPos + 36 + 16,
                Component.literal("The drum will cool down hot nuclear"),
                Component.literal("waste when submerged in water. More"),
                Component.literal("water speeds up the process."));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
