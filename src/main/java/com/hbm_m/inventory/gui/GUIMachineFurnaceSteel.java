package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineFurnaceSteelMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI der Stahlpresse - drei parallele Fortschrittspfeile (eine je Schmelzspur) + gemeinsame
 *  Brennstoffanzeige, siehe {@link com.hbm_m.blockentity.machines.MachineFurnaceSteelBlockEntity}
 *  fuer die zugrundeliegende Vereinfachung ggue. dem waermequellenbetriebenen Original. */
public class GUIMachineFurnaceSteel extends AbstractContainerScreen<MachineFurnaceSteelMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_furnace_steel.png");

    public GUIMachineFurnaceSteel(MachineFurnaceSteelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Fortschritts-/Brennstoffanzeigen als einfache Fuellrechtecke: die genaue Sprite-Anordnung
        // der (bereits vorhandenen) GUI-Textur ist unbekannt, ein blit() von geratenen Koordinaten
        // wuerde falsche Bildausschnitte zeigen. Rechtecke sind immer korrekt, unabhaengig davon.
        if (menu.isLit()) {
            int fuelHeight = menu.getBurnProgressScaled(14);
            guiGraphics.fill(x + 80, y + 39 - fuelHeight, x + 94, y + 39, 0xFFE04B10);
        }

        for (int lane = 0; lane < 3; lane++) {
            int laneY = 17 + lane * 18;
            int cookProgress = menu.getCookProgressScaled(lane, 24);
            if (cookProgress > 0) {
                guiGraphics.fill(x + 60, y + laneY + 2, x + 60 + cookProgress, y + laneY + 10, 0xFFC0C0C0);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
