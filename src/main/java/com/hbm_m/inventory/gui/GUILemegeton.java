package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.LemegetonMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUILemegeton}: Beschriftung in der Standard-Galactic-Schrift (minecraft:alt). */
public class GUILemegeton extends AbstractContainerScreen<LemegetonMenu> {

    public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_lemegeton.png");
    private static final Style GALACTIC = Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("alt"));

    public GUILemegeton(LemegetonMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mX, int mY) {
        g.drawString(font, Component.literal("Material Upgrade Conversion").withStyle(GALACTIC), 28, 6, 4210752, false);
        g.drawString(font, Component.literal("Standard Inventory").withStyle(GALACTIC), 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float inter, int mX, int mY) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (this.menu.getSlot(0).hasItem())
            g.blit(texture, leftPos + 7, topPos + 22, 0, 166, 162, 42);
    }
}
