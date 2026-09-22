package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.inventory.menu.MachineCentrifugeMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Centrifuge GUI.
 * Renders the energy bar and segmented progress bars matching the legacy 1.7.10 layout.
 */
public class GUIMachineCentrifuge extends GuiInfoScreen<MachineCentrifugeMenu> {

    private static final ResourceLocation TEXTURE =
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_centrifuge.png");


    public GUIMachineCentrifuge(MachineCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 182;
        this.imageHeight = 189;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        long power = menu.getEnergyLong();
        long maxPower = menu.getMaxEnergyLong();
        if (power > 0 && maxPower > 0) {
            int i1 = (int) (power * 37 / maxPower);
            if (i1 > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 55 - i1, 182, 37 - i1, 16, i1);
            }
        }

        if (menu.isProcessing()) {
            int p = menu.getScaledProgress(145);
            for (int i = 0; i < 4; i++) {
                int h = Math.min(p, 36);
                if (h > 0) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 72 + i * 20, this.topPos + 57 - h, 182, 73 - h, 12, h);
                }
                p -= h;
                if (p <= 0) break;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip (legacy hover rect)
        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                9, 13,
                16, 34,
                menu.getEnergyLong(),
                menu.getMaxEnergyLong());
    }
}
