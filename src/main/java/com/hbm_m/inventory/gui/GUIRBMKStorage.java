package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.rbmk.RBMKStorageBlockEntity;
import com.hbm_m.inventory.menu.RBMKStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GUIRBMKStorage extends GuiInfoScreen<RBMKStorageMenu> {

    private final RBMKStorageBlockEntity be;

    public GUIRBMKStorage(RBMKStorageMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth  = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2B2B);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);

        for (int i = 0; i < RBMKStorageBlockEntity.SLOTS; i++) {
            int sx = leftPos + 44 + i * 22;
            int sy = topPos + 35;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF888888);
            g.renderItem(be.slots[i], sx, sy);
            g.renderItemDecorations(font, be.slots[i], sx, sy);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);

        g.drawString(font, "RBMK Storage Column", leftPos + 8, topPos + 8, 0x333333, false);
        g.drawString(font, String.format("Heat: %.1f°C", be.heat), leftPos + 8, topPos + 20, 0x990000, false);

        renderTooltip(g, mx, my);
    }
}
