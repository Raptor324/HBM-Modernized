package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineMicrowaveBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineMicrowaveMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.MicrowaveSpeedC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 1:1 port of {@code GUIMicrowave} (1.7.10 original):
 * Power bar (8,17..51), progress arrow (104,34), speed bar (62,26..60),
 * speed +/- hitboxes at (43,25,18,18) and (43,43,18,18).
 */
@OnlyIn(Dist.CLIENT)
public class GUIMachineMicrowave extends GuiInfoScreen<MachineMicrowaveMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_microwave.png");

    private final MachineMicrowaveBlockEntity blockEntity;

    public GUIMachineMicrowave(MachineMicrowaveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && blockEntity != null) {
            if (isPointInRect(43, 25, 18, 18, (int) mouseX, (int) mouseY)) {
                playClickSound();
                MicrowaveSpeedC2SPacket.sendToServer(blockEntity.getBlockPos(), 1);
                return true;
            }
            if (isPointInRect(43, 43, 18, 18, (int) mouseX, (int) mouseY)) {
                playClickSound();
                MicrowaveSpeedC2SPacket.sendToServer(blockEntity.getBlockPos(), -1);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        if (blockEntity != null) {
            int powerHeight = (int) blockEntity.getPowerScaled(34);
            if (powerHeight > 0) {
                guiGraphics.blit(TEXTURE, x + 8, y + 51 - powerHeight, 176, 34 - powerHeight, 16, powerHeight, 256, 256);
            }

            int progressWidth = Math.min(blockEntity.getProgressScaled(23), 22);
            if (progressWidth > 0) {
                guiGraphics.blit(TEXTURE, x + 104, y + 34, 192, 0, progressWidth, 16, 256, 256);
            }

            int speedHeight = blockEntity.getSpeedScaled(34);
            if (speedHeight > 0) {
                guiGraphics.blit(TEXTURE, x + 62, y + 60 - speedHeight, 214, 34 - speedHeight, 4, speedHeight, 256, 256);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (blockEntity != null) {
            drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 17, 16, 34,
                    blockEntity.getEnergyStored(), blockEntity.getMaxEnergyStored());
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
