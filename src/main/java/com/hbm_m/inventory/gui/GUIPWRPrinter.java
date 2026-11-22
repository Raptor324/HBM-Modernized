package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind;
import com.hbm_m.client.GuiCompat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Construction-diagram viewer for the PWR Printer (see {@code PWRFuelPrinterItem}). Shows the
 * scanned reactor structure as a flat grid per Y-layer (one colored cell per block kind) with
 * Prev/Next-layer navigation - the 2D-grid adaptation of the original's rotatable 3D slice
 * renderer (see {@code PWRFuelPrinterItem}'s class doc).
 */
public class GUIPWRPrinter extends Screen {

    private static final int CELL = 16;

    private final int sizeX, sizeY, sizeZ;
    private final byte[] grid;
    private int layer;

    private Button prevButton;
    private Button nextButton;

    public GUIPWRPrinter(int sizeX, int sizeY, int sizeZ, byte[] grid) {
        super(Component.literal("PWR Construction Diagram"));
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.grid = grid;
        this.layer = sizeY / 2;
    }

    @Override
    protected void init() {
        int gridWidth = sizeX * CELL;
        int left = (this.width - gridWidth) / 2;
        int bottom = (this.height + sizeZ * CELL) / 2 + 24;

        this.prevButton = this.addRenderableWidget(Button.builder(Component.literal("< Layer"), b -> {
            layer = Math.max(0, layer - 1);
        }).bounds(left, bottom, 80, 20).build());

        this.nextButton = this.addRenderableWidget(Button.builder(Component.literal("Layer >"), b -> {
            layer = Math.min(sizeY - 1, layer + 1);
        }).bounds(left + gridWidth - 80, bottom, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);

        int gridWidth = sizeX * CELL;
        int gridHeight = sizeZ * CELL;
        int left = (this.width - gridWidth) / 2;
        int top = (this.height - gridHeight) / 2 - 10;

        guiGraphics.fill(left - 2, top - 2, left + gridWidth + 2, top + gridHeight + 2, 0xFF222222);

        for (int z = 0; z < sizeZ; z++) {
            for (int x = 0; x < sizeX; x++) {
                int idx = x + layer * sizeX + z * sizeX * sizeY;
                byte cell = grid[idx];
                int color = colorFor(cell);
                int cx = left + x * CELL;
                int cz = top + z * CELL;
                guiGraphics.fill(cx, cz, cx + CELL - 1, cz + CELL - 1, color);
            }
        }

        Component layerLabel = Component.literal("Layer " + (layer + 1) + " / " + sizeY);
        guiGraphics.drawCenteredString(this.font, layerLabel, this.width / 2, top - 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top - 34, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderLegend(guiGraphics, left, top + gridHeight + 30);
    }

    private void renderLegend(GuiGraphics guiGraphics, int x, int y) {
        Kind[] kinds = Kind.values();
        for (int i = 0; i < kinds.length; i++) {
            int lx = x + (i % 3) * 140;
            int ly = y + (i / 3) * 14;
            guiGraphics.fill(lx, ly, lx + 10, ly + 10, colorFor((byte) (i + 1)));
            guiGraphics.drawString(this.font, kinds[i].name(), lx + 14, ly + 1, 0xCCCCCC, false);
        }
        int cx = x, cy = y + ((kinds.length + 2) / 3) * 14;
        guiGraphics.fill(cx, cy, cx + 10, cy + 10, colorFor((byte) 16));
        guiGraphics.drawString(this.font, "CONTROLLER", cx + 14, cy + 1, 0xCCCCCC, false);
    }

    private static int colorFor(byte cell) {
        return switch (cell) {
            case 0 -> 0x00000000;
            case 1 -> 0xFFE55B4B; // FUEL
            case 2 -> 0xFF4B9FE5; // CONTROL
            case 3 -> 0xFF66CCCC; // CHANNEL
            case 4 -> 0xFFCC6633; // HEATEX
            case 5 -> 0xFF999999; // HEATSINK
            case 6 -> 0xFFCCFF33; // NEUTRON_SOURCE
            case 7 -> 0xFF888888; // CASING
            case 8 -> 0xFFAAAAFF; // REFLECTOR
            case 9 -> 0xFF33AA33; // PORT
            case 16 -> 0xFFFFCC00; // controller marker
            default -> 0xFF000000;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
