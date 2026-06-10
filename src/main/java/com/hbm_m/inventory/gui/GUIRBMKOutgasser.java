package com.hbm_m.inventory.gui;

import com.hbm_m.block.entity.machines.rbmk.RBMKOutgasserBlockEntity;
import com.hbm_m.inventory.menu.RBMKOutgasserMenu;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GUIRBMKOutgasser extends GuiInfoScreen<RBMKOutgasserMenu> {

    private final RBMKOutgasserBlockEntity be;

    public GUIRBMKOutgasser(RBMKOutgasserMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth  = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2B2B);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
        g.renderItem(be.rodSlot, leftPos + 80, topPos + 35);
        g.renderItemDecorations(font, be.rodSlot, leftPos + 80, topPos + 35);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);

        int x = leftPos + 8, y = topPos + 8;
        g.drawString(font, "RBMK Outgasser", x, y, 0x333333, false);
        g.drawString(font, String.format("Heat:        %.1f°C", be.heat), x, y + 12, 0x990000, false);
        g.drawString(font, String.format("Flux buffer: %.2f", be.fluxBuffer), x, y + 22, 0x004499, false);

        ItemStack rod = be.rodSlot;
        if (!rod.isEmpty() && rod.getItem() instanceof RBMKRodItem) {
            g.drawString(font, String.format("Xenon: %.2f%%", RBMKRodItem.getPoison(rod)), x, y + 50, 0x660066, false);
        } else {
            g.drawString(font, "No rod inserted", x, y + 50, 0x888888, false);
        }

        renderTooltip(g, mx, my);
    }
}
