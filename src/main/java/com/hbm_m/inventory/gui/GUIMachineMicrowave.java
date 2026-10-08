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

/**
 * 1:1-Port von {@code GUIMicrowave} (1.7.10): 176x168, Energiebalken (8, 17-51), Fortschritt (104, 34),
 * Geschwindigkeitssaeule (62, 26-60) und die beiden unsichtbaren Schaltflaechen der Textur
 * (43/25 schneller, 43/43 langsamer). ACHTUNG: Geschwindigkeit 5 (Maximum) laesst die Maschine
 * explodieren - absichtliches Originalverhalten.
 */
public class GUIMachineMicrowave extends GuiInfoScreen<MachineMicrowaveMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_microwave.png");

    private final MachineMicrowaveBlockEntity blockEntity;

    public GUIMachineMicrowave(MachineMicrowaveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 168;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (blockEntity == null) return handled; // тайл может отсутствовать в реплее Flashback
        double x = mouseX, y = mouseY;
        // Original: guiLeft + 43 <= x < +18, guiTop + 25 < y <= +18 -> AuxButtonPacket 0 (speed++)
        if (leftPos + 43 <= x && leftPos + 43 + 18 > x && topPos + 25 < y && topPos + 25 + 18 >= y) {
            MicrowaveSpeedC2SPacket.sendToServer(blockEntity.getBlockPos(), 1);
            return true;
        }
        // AuxButtonPacket 1 (speed--)
        if (leftPos + 43 <= x && leftPos + 43 + 18 > x && topPos + 43 < y && topPos + 43 + 18 >= y) {
            MicrowaveSpeedC2SPacket.sendToServer(blockEntity.getBlockPos(), -1);
            return true;
        }
        return handled;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (blockEntity == null) return;

        int i = (int) blockEntity.getPowerScaled(34);
        guiGraphics.blit(TEXTURE, leftPos + 8, topPos + 51 - i, 176, 34 - i, 16, i);

        int j = Math.min(blockEntity.getProgressScaled(23), 22);
        guiGraphics.blit(TEXTURE, leftPos + 104, topPos + 34, 192, 0, j, 16);

        int k = blockEntity.getSpeedScaled(34);
        guiGraphics.blit(TEXTURE, leftPos + 62, topPos + 60 - k, 214, 34 - k, 4, k);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(font, name, imageWidth / 2 - font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (blockEntity != null) {
            this.drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 51 - 34, 16, 34,
                    blockEntity.getEnergyStored(), blockEntity.getMaxEnergyStored());
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
