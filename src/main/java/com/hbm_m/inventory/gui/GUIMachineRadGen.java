package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineRadGenBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineRadGenMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineRadGen extends GuiInfoScreen<MachineRadGenMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_radgen.png");

    private final MachineRadGenBlockEntity radgen;

    public GUIMachineRadGen(MachineRadGenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.radgen = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 184;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        for (int i = 0; i < MachineRadGenBlockEntity.QUEUE_COUNT; i++) {
            int max = radgen.getMaxProgress(i);
            if (max <= 0) continue;
            int j = radgen.getProgress(i) * 44 / max;
            guiGraphics.blit(TEXTURE, this.leftPos + 66, this.topPos + 19 + i * 5, 176, 0, j, 3);
        }

        int j = (int) (radgen.getEnergyStored() * 48L / Math.max(1L, radgen.getMaxEnergyStored()));
        guiGraphics.blit(TEXTURE, this.leftPos + 64, this.topPos + 83, 176, 3, j, 4);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                64, 83, 48, 4,
                radgen.getEnergyStored(), radgen.getMaxEnergyStored());

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
