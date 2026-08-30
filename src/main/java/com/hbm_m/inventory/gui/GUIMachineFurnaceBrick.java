package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineFurnaceBrickMenu;
import com.hbm_m.client.GuiCompat;
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

        // UV-Koordinaten und Rechtecke 1:1 aus GUIFurnaceBrick (1.7.10 Original) uebernommen.
        if (menu.isLit()) {
            int b = menu.getBurnProgressScaled(13);
            guiGraphics.blit(TEXTURE, x + 62, y + 54 + 12 - b, 176, 12 - b, 14, b + 1);

            int p = menu.getCookProgressScaled(24);
            guiGraphics.blit(TEXTURE, x + 85, y + 34, 176, 14, p + 1, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
