package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineGasCentrifugeMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineGasCentrifuge extends GuiInfoScreen<MachineGasCentrifugeMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_centrifuge_gas.png");

    public GUIMachineGasCentrifuge(MachineGasCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 185;
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

        // Energy bar
        long power = menu.getEnergyLong();
        long maxPower = menu.getMaxEnergyLong();
        if (power > 0 && maxPower > 0) {
            int barHeight = 52;
            int filled = (int) (power * barHeight / maxPower);
            if (filled > 0) {
                int x0 = this.leftPos + 152;
                int y0 = this.topPos + 16 + (barHeight - filled);
                guiGraphics.fill(x0, y0, x0 + 16, y0 + filled, 0xFF3FCFE0);
            }
        }

        // Progress arrow
        if (menu.isProcessing()) {
            int p = menu.getScaledProgress(24);
            if (p > 0) {
                int arrowX = this.leftPos + 52;
                int arrowY = this.topPos + 35;
                guiGraphics.fill(arrowX, arrowY, arrowX + p, arrowY + 6, 0xFFFFE066);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                152, 16,
                16, 52,
                menu.getEnergyLong(),
                menu.getMaxEnergyLong());
    }
}
