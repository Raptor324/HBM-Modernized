package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.rbmk.RBMKControlBlockEntity;
import com.hbm_m.inventory.menu.RBMKControlMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GUIRBMKControl extends GuiInfoScreen<RBMKControlMenu> {

    private final RBMKControlBlockEntity be;

    public GUIRBMKControl(RBMKControlMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth  = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // Insert / Extract buttons
        addRenderableWidget(Button.builder(Component.literal("Insert (0%)"), btn -> {
            be.setTarget(0.0);
        }).bounds(leftPos + 20, topPos + 60, 60, 16).build());

        addRenderableWidget(Button.builder(Component.literal("50%"), btn -> {
            be.setTarget(0.5);
        }).bounds(leftPos + 88, topPos + 60, 40, 16).build());

        addRenderableWidget(Button.builder(Component.literal("Extract"), btn -> {
            be.setTarget(1.0);
        }).bounds(leftPos + 136, topPos + 60, 50, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2B2B);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);

        // Level bar background
        g.fill(leftPos + 20, topPos + 40, leftPos + 156, topPos + 56, 0xFF888888);
        // Level fill
        int fillW = (int)((156 - 20) * be.level);
        g.fill(leftPos + 20, topPos + 40, leftPos + 20 + fillW, topPos + 56, 0xFF00CC00);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        GuiCompat.renderBackground(this, g, mx, my, partial);
        super.render(g, mx, my, partial);

        int x = leftPos + 8, y = topPos + 8;
        g.drawString(font, "RBMK Control Rod", x, y, 0x333333, false);
        g.drawString(font, String.format("Level:  %.1f%%", be.level * 100), x, y + 12, 0x006600, false);
        g.drawString(font, String.format("Target: %.1f%%", be.targetLevel * 100), x, y + 22, 0x004499, false);
        g.drawString(font, String.format("Heat:   %.1f°C", be.heat), x, y + 84, 0x990000, false);

        renderTooltip(g, mx, my);
    }
}
