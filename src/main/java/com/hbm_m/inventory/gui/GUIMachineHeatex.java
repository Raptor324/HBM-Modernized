package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineHeatexBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineHeatexMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Direktport der Slot-/Tank-Koordinaten aus {@code GUIHeaterHeatex} (1.7.10 Original). */
public class GUIMachineHeatex extends GuiInfoScreen<MachineHeatexMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_heatex.png");

    private final MachineHeatexBlockEntity be;

    private static final int TANK_HOT_X = 44;
    private static final int TANK_COLD_X = 116;
    private static final int TANK_Y = 36;
    private static final int TANK_W = 16;
    private static final int TANK_H = 52;

    public GUIMachineHeatex(MachineHeatexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.be = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        be.getHotTank().renderTank(guiGraphics, leftPos + TANK_HOT_X, topPos + TANK_Y, TANK_W, TANK_H);
        be.getColdTank().renderTank(guiGraphics, leftPos + TANK_COLD_X, topPos + TANK_Y, TANK_W, TANK_H);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        be.getHotTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + TANK_HOT_X, topPos + TANK_Y, TANK_W, TANK_H);
        be.getColdTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + TANK_COLD_X, topPos + TANK_Y, TANK_W, TANK_H);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
