package com.hbm_m.inventory.gui;

import com.hbm_m.block.entity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.inventory.menu.MachineCrucibleMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public class GUIMachineCrucible extends GuiInfoScreen<MachineCrucibleMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_crucible.png");

    // Left narrow column: x=7..25, y=16..127 (total fill height = 111px)
    private static final int COL_X  = 7;
    private static final int COL_W  = 18;
    private static final int COL_Y0 = 16;
    private static final int COL_Y1 = 127;
    private static final int COL_H  = COL_Y1 - COL_Y0;   // 111

    private static final int GAUGE_U = 176;

    public GUIMachineCrucible(MachineCrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // ── Material fill (narrow left column) ───────────────────────────────
        int stored = menu.getLiquidStored();
        int cap    = menu.getLiquidCap();
        if (stored > 0 && cap > 0) {
            int fillH = Math.min(COL_H, stored * COL_H / cap);
            if (fillH > 0) {
                int color = getMaterialColor();
                int yTop  = this.topPos + COL_Y1 - fillH;
                g.fill(this.leftPos + COL_X, yTop,
                       this.leftPos + COL_X + COL_W, this.topPos + COL_Y1, color);
            }
        }

        // ── Progress bar (0..33 px wide) ─────────────────────────────────────
        int pGauge = menu.getScaledProgress();
        if (pGauge > 0) {
            g.blit(TEXTURE, this.leftPos + 126, this.topPos + 82, GAUGE_U, 0, pGauge, 5);
        }

        // ── Heat bar (0..33 px wide) ──────────────────────────────────────────
        int hGauge = menu.getScaledHeat();
        if (hGauge > 0) {
            g.blit(TEXTURE, this.leftPos + 126, this.topPos + 91, GAUGE_U, 5, hGauge, 5);
        }
    }

    private int getMaterialColor() {
        MachineCrucibleBlockEntity be = menu.blockEntity;
        if (be != null) return be.getFillColor();
        return 0xFFC47A2C;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
        g.drawString(this.font, Component.translatable("container.inventory"),
                8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, delta);
        this.renderTooltip(g, mouseX, mouseY);

        drawCustomInfoStat(g, mouseX, mouseY,
                COL_X, COL_Y0, COL_W, COL_H, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", menu.getLiquidStored()) +
                        " / " + String.format(Locale.US, "%,d", menu.getLiquidCap()) + " mB"));

        drawCustomInfoStat(g, mouseX, mouseY,
                125, 81, 34, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", menu.getProgress()) +
                        " / " + String.format(Locale.US, "%,d", menu.getProcessTime()) + " TU"));

        drawCustomInfoStat(g, mouseX, mouseY,
                125, 90, 34, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", menu.getHeat()) +
                        " / " + String.format(Locale.US, "%,d", menu.getMaxHeat()) + " TU"));
    }
}
