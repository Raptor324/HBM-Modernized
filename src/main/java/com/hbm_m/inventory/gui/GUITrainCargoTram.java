package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.entity.train.TrainCargoTram;
import com.hbm_m.inventory.menu.TrainCargoTramMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code TrainCargoTram.GUITrainCargoTram}: Ladeplaetze, Batterieplatz, Stromanzeige (152, 18) und Betriebslampe. */
public class GUITrainCargoTram extends GuiInfoScreen<TrainCargoTramMenu> {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/vehicles/gui_cargo_tram.png");
    private final TrainCargoTram train;

    public GUITrainCargoTram(TrainCargoTramMenu menu, Inventory invPlayer, Component title) {
        super(menu, invPlayer, title);
        this.train = menu.train;
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);
        if (train != null) this.drawElectricityInfo(g, x, y, 152, 18, 16, 52, train.getPower(), train.getMaxPower());
        this.renderTooltip(g, x, y);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int i, int j) {
        g.drawString(this.font, this.title, 140 / 2 - this.font.width(this.title) / 2, 6, 0xffffff, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float interp, int x, int y) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (train == null) return;

        int i = train.getPower() * 53 / train.getMaxPower();
        g.blit(texture, leftPos + 152, topPos + 70 - i, 176, 52 - i, 16, i);

        if (train.getPower() > train.getPowerConsumption()) {
            g.blit(texture, leftPos + 156, topPos + 4, 176, 52, 9, 12);
        }
    }
}
