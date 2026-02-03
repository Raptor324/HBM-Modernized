package com.hbm_m.client.overlay;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.MachineCentrifugeMenu;
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

    // Small manual adjustment to better match the legacy layout.
    private static final int GUI_Y_OFFSET = -8;

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_centrifuge.png");

    public GUIMachineCentrifuge(MachineCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.topPos += GUI_Y_OFFSET;
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // The supplied gui_centrifuge.png does not contain separate widget sprites in the usual
        // "x >= 176" area, so we render dynamic bars as colored fills over the background.

        // Energy bar (legacy coords)
        long power = menu.getEnergyLong();
        long maxPower = menu.getMaxEnergyLong();
        if (power > 0 && maxPower > 0) {
            int barHeight = 34;
            int filled = (int) (power * barHeight / maxPower);
            if (filled > 0) {
                int x0 = this.leftPos + 9;
                int y0 = this.topPos + 13 + (barHeight - filled);
                guiGraphics.fill(x0, y0, x0 + 16, y0 + filled, 0xFF3FCFE0);
            }
        }

        // Progress bars (legacy logic: total scaled to 145, split into 4 columns of 36px)
        if (menu.isProcessing()) {
            int p = menu.getScaledProgress(145);
            int baseY = this.topPos + 50;
            for (int i = 0; i < 4; i++) {
                int h = Math.min(p, 36);
                if (h > 0) {
                    int x0 = this.leftPos + 65 + i * 20;
                    guiGraphics.fill(x0, baseY - h, x0 + 12, baseY, 0xFFFFE066);
                }
                p -= h;
                if (p <= 0) {
                    break;
                }
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

        // Tooltip (legacy hover rect)
        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                9, 13,
                16, 34,
                menu.getEnergyLong(),
                menu.getMaxEnergyLong());
    }
}
