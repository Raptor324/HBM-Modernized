package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.rbmk.RBMKBoilerBlockEntity;
import com.hbm_m.inventory.menu.RBMKBoilerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GUIRBMKBoiler extends GuiInfoScreen<RBMKBoilerMenu> {

    private static final String[] GRADE_NAMES = { "Steam", "Hot Steam", "Super-Hot", "Ultra-Hot" };

    private final RBMKBoilerBlockEntity be;

    public GUIRBMKBoiler(RBMKBoilerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth  = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Cycle Steam Grade"), btn -> {
            be.cycleCompressor();
        }).bounds(leftPos + 40, topPos + 90, 100, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2B2B);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);

        // Water tank bar
        int maxW = be.waterTank.getMaxFill();
        int fillW = maxW > 0 ? (int)(be.waterTank.getFill() * 20 / maxW) : 0;
        g.fill(leftPos + 20, topPos + 55, leftPos + 40, topPos + 75, 0xFF555555);
        g.fill(leftPos + 20, topPos + 75 - fillW, leftPos + 40, topPos + 75, 0xFF2266FF);

        // Steam tank bar
        int maxS = be.steamTank.getMaxFill();
        int fillS = maxS > 0 ? (int)(be.steamTank.getFill() * 20 / maxS) : 0;
        g.fill(leftPos + 136, topPos + 55, leftPos + 156, topPos + 75, 0xFF555555);
        g.fill(leftPos + 136, topPos + 75 - fillS, leftPos + 156, topPos + 75, 0xFFCCCCCC);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        GuiCompat.renderBackground(this, g, mx, my, partial);
        super.render(g, mx, my, partial);

        int x = leftPos + 8, y = topPos + 8;
        g.drawString(font, "RBMK Boiler", x, y, 0x333333, false);
        g.drawString(font, String.format("Heat:  %.1f°C", be.heat), x, y + 12, 0x990000, false);
        g.drawString(font, String.format("Water: %d / %d mB", be.waterTank.getFill(), be.waterTank.getMaxFill()), x, y + 22, 0x004499, false);
        g.drawString(font, String.format("Steam: %d / %d mB", be.steamTank.getFill(), be.steamTank.getMaxFill()), x, y + 32, 0x888888, false);
        g.drawString(font, "Type: " + GRADE_NAMES[Math.min(be.steamGrade, 3)], x, y + 42, 0x666666, false);

        renderTooltip(g, mx, my);
    }
}
