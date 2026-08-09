package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineFurnaceIronMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI der Eisenpresse - Fortschrittspfeil + Brennstoffanzeige im Stil der Vanilla-Ofen-GUI. */
public class GUIMachineFurnaceIron extends AbstractContainerScreen<MachineFurnaceIronMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_furnace_iron.png");

    public GUIMachineFurnaceIron(MachineFurnaceIronMenu menu, Inventory inventory, Component title) {
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

        // UV-Koordinaten und Rechtecke 1:1 aus GUIFurnaceIron (1.7.10 Original) uebernommen.
        int i = menu.getCookProgressScaled(70);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 53, y + 36, 176, 18, i, 5);
        }

        int j = menu.getBurnProgressScaled(70);
        if (j > 0) {
            guiGraphics.blit(TEXTURE, x + 53, y + 45, 176, 23, j, 5);
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
