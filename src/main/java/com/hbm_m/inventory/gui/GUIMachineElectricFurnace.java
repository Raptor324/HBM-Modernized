package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineElectricFurnaceMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI der Elektroofen - Fortschrittspfeil im Stil der Vanilla-Ofen-GUI. */
public class GUIMachineElectricFurnace extends AbstractContainerScreen<MachineElectricFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_electric_furnace.png");

    public GUIMachineElectricFurnace(MachineElectricFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Fortschritts-/Energieanzeige als einfache Fuellrechtecke: die genaue Sprite-Anordnung
        // der (aus dem Original uebernommenen) GUI-Textur ist unbekannt, ein blit() von geratenen
        // Koordinaten wuerde falsche Bildausschnitte zeigen. Rechtecke sind immer korrekt.
        int cookProgress = menu.getCookProgressScaled(28);
        if (cookProgress > 0) {
            guiGraphics.fill(x + 43, y + 36, x + 43 + cookProgress, y + 48, 0xFFC0C0C0);
        }

        if (menu.hasPower()) {
            guiGraphics.fill(x + 154, y + 20, x + 166, y + 52, 0xFF3080FF);
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
