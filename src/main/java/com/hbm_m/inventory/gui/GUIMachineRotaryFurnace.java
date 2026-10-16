package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineRotaryFurnaceMenu;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI der Rotationsofen - Fortschritts-/Brennstoff-/Tankanzeigen als Fuellrechtecke (die genaue
 *  Sprite-Anordnung der vorhandenen Textur ist unbekannt, siehe {@code GUIMachineFurnaceIron}). */
public class GUIMachineRotaryFurnace extends AbstractContainerScreen<MachineRotaryFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_rotary_furnace.png");

    public GUIMachineRotaryFurnace(MachineRotaryFurnaceMenu menu, Inventory inventory, Component title) {
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

        if (menu.isLit()) {
            int fuelHeight = menu.getBurnProgressScaled(14);
            guiGraphics.fill(x + 44, y + 68 - fuelHeight, x + 62, y + 68, 0xFFE04B10);
        }

        int progress = menu.getProgressScaled(33);
        if (progress > 0) {
            guiGraphics.fill(x + 63, y + 30, x + 63 + progress, y + 40, 0xFFC0C0C0);
        }

        // тайл может отсутствовать в реплее Flashback
        if (menu.blockEntity != null) {
        int fill = menu.blockEntity.getTank().getFill();
        int max = menu.blockEntity.getTank().getMaxFill();
        if (max > 0 && fill > 0) {
            int w = fill * 52 / max;
            guiGraphics.fill(x + 8, y + 36, x + 8 + w, y + 52, 0x8000AAFF);
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
