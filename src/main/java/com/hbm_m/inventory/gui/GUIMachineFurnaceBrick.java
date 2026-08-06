package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineFurnaceBrickMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI des Ziegelofens - Fortschrittspfeil + Brennstoffanzeige im Stil der Vanilla-Ofen-GUI. */
public class GUIMachineFurnaceBrick extends AbstractContainerScreen<MachineFurnaceBrickMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_furnace_brick.png");

    public GUIMachineFurnaceBrick(MachineFurnaceBrickMenu menu, Inventory inventory, Component title) {
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
            guiGraphics.fill(x + 36, y + 32 - fuelHeight, x + 44, y + 32, 0xFFE04B10);
        }

        int cookProgress = menu.getCookProgressScaled(24);
        if (cookProgress > 0) {
            guiGraphics.fill(x + 82, y + 36, x + 82 + cookProgress, y + 44, 0xFFC0C0C0);
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
