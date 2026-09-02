package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineFurnaceSteelMenu;
import com.hbm_m.client.GuiCompat;
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

        // Fuellstand des Brennstoffs: das Original hatte keine Brennstoff-Slots (reine
        // Waermequelle mit vertikalem Heat-Balken bei 152,18); dieser Port nutzt eigene
        // Brennstoff-Slots (siehe MachineFurnaceSteelMenu), daher gibt es keine 1:1-Textur-UV
        // dafuer - Fuellrechteck bleibt als Behelfsanzeige bestehen.
        if (menu.isLit()) {
            int fuelHeight = menu.getBurnProgressScaled(14);
            guiGraphics.fill(x + 80, y + 39 - fuelHeight, x + 94, y + 39, 0xFFE04B10);
        }

        // Pro-Spur-Fortschrittsbalken 1:1 aus GUIFurnaceSteel (1.7.10 Original) uebernommen:
        // Position (54, 18+18*i), UV (176,18), Hoehe 5.
        for (int lane = 0; lane < 3; lane++) {
            int p = menu.getCookProgressScaled(lane, 69);
            if (p > 0) {
                guiGraphics.blit(TEXTURE, x + 54, y + 18 + 18 * lane, 176, 18, p, 5);
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
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
