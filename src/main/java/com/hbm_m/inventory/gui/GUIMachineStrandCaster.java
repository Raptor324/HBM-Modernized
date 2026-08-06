package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineStrandCasterMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI der Straggusmaschine - Fuellstand/Tank als Fuellrechtecke (die genaue Sprite-Anordnung der
 *  vorhandenen Textur ist unbekannt, siehe {@code GUIMachineFurnaceIron}). */
public class GUIMachineStrandCaster extends AbstractContainerScreen<MachineStrandCasterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_strand_caster.png");

    public GUIMachineStrandCaster(MachineStrandCasterMenu menu, Inventory inventory, Component title) {
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

        int capacity = menu.blockEntity.getCapacity();
        if (capacity > 0) {
            int w = menu.blockEntity.amount * 52 / capacity;
            guiGraphics.fill(x + 26, y + 18, x + 26 + w, y + 34, 0xFFC85A17);
        }

        int fill = menu.blockEntity.getTank().getFill();
        int max = menu.blockEntity.getTank().getMaxFill();
        if (max > 0 && fill > 0) {
            int w = fill * 52 / max;
            guiGraphics.fill(x + 26, y + 54, x + 26 + w, y + 70, 0x8000AAFF);
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
