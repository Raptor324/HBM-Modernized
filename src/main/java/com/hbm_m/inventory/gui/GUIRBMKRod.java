package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.inventory.menu.RBMKRodMenu;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GUIRBMKRod extends GuiInfoScreen<RBMKRodMenu> {

    private final RBMKRodBlockEntity be;

    public GUIRBMKRod(RBMKRodMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth  = 200;
        this.imageHeight = 130;
        // Suppress the vanilla "Inventory" label offset check
        this.inventoryLabelY = imageHeight + 10;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        // Background
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2B2B);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);

        // Fuel slot background
        g.fill(leftPos + 6, topPos + 33, leftPos + 24, topPos + 51, 0xFF8B8B8B);
        g.fill(leftPos + 7, topPos + 34, leftPos + 23, topPos + 50, 0xFF3F3F3F);

        // Render the fuel rod item inside the slot
        ItemStack rod = be.fuelSlot;
        if (!rod.isEmpty()) {
            g.renderItem(rod, leftPos + 8, topPos + 35);
            g.renderItemDecorations(font, rod, leftPos + 8, topPos + 35);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // Suppress default title/inventory labels
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        super.render(g, mx, my, partial);

        int x = leftPos + 30;
        int y = topPos + 8;

        g.drawString(font, "RBMK Fuel Channel", x, y,      0x333333, false);
        g.drawString(font, String.format("Heat: %.1f / %.0f°C", be.heat, be.maxHeat()),
                     x, y + 14, 0x990000, false);
        g.drawString(font, String.format("Flux in:    %.2f", be.lastFluxQuantity),
                     x, y + 24, 0x004499, false);
        g.drawString(font, String.format("Flux ratio: %.2f", be.lastFluxRatio),
                     x, y + 34, 0x004499, false);

        ItemStack rod = be.fuelSlot;
        if (!rod.isEmpty() && rod.getItem() instanceof RBMKRodItem ri) {
            y += 52;
            g.drawString(font, String.format("Depletion: %.1f%%",
                    (1.0 - RBMKRodItem.getEnrichment(rod)) * 100), x, y,      0x006600, false);
            g.drawString(font, String.format("Xenon:     %.2f%%",
                    RBMKRodItem.getPoison(rod)),                   x, y + 10, 0x660066, false);
            g.drawString(font, String.format("Hull:      %.1f / %.0f°C",
                    RBMKRodItem.getHullHeat(rod), ri.meltingPoint),x, y + 20, 0xAA4400, false);
        } else {
            g.drawString(font, "No fuel rod", x, topPos + 66, 0x888888, false);
        }

        this.renderTooltip(g, mx, my);
    }
}
