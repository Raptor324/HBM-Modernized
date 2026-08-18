package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCokerBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineCokerMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineCoker extends GuiInfoScreen<MachineCokerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_coker.png");

    private final MachineCokerBlockEntity coker;

    public GUIMachineCoker(MachineCokerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.coker = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int p = coker.getMaxProgress() > 0 ? coker.getProgress() * 53 / coker.getMaxProgress() : 0;
        guiGraphics.blit(TEXTURE, this.leftPos + 61, this.topPos + 46, 176, 0, p, 5);

        int h = coker.getMaxHeat() > 0 ? coker.getHeat() * 52 / coker.getMaxHeat() : 0;
        guiGraphics.blit(TEXTURE, this.leftPos + 61, this.topPos + 55, 176, 5, h, 5);

        coker.getTank0().renderTank(guiGraphics, this.leftPos + 35, this.topPos + 70, 16, 52);
        coker.getTank1().renderTank(guiGraphics, this.leftPos + 125, this.topPos + 70, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0xC7C1A3, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        coker.getTank0().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 35, this.topPos + 18, 16, 52);
        coker.getTank1().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 125, this.topPos + 18, 16, 52);

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                60, 45, 54, 7, mouseX, mouseY,
                Component.literal(coker.getProgress() + " / " + coker.getMaxProgress() + "TU"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                60, 54, 54, 7, mouseX, mouseY,
                Component.literal(coker.getHeat() + " / " + coker.getMaxHeat() + "TU"));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
