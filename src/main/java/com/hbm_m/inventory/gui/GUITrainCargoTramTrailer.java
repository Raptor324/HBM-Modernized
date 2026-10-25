package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.TrainCargoTramTrailerMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code TrainCargoTramTrailer.GUITrainCargoTramTrailer}. */
public class GUITrainCargoTramTrailer extends AbstractContainerScreen<TrainCargoTramTrailerMenu> {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/vehicles/gui_cargo_tram_trailer.png");

    public GUITrainCargoTramTrailer(TrainCargoTramTrailerMenu menu, Inventory invPlayer, Component title) {
        super(menu, invPlayer, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);
        this.renderTooltip(g, x, y);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int i, int j) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0xffffff, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float interp, int x, int y) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
