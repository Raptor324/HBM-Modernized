package com.hbm_m.inventory.gui;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.BookMenu;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI книги Вагонов ({@code book_of_}) — порт {@code GUIBook} из 1.7.10.
 * Текстура и позиции 1:1 с оригиналом: сетка 2×2 с шагом 36 пикселей и
 * подсветка рамки вокруг сетки, когда результат есть.
 */
public class GUIBook extends AbstractContainerScreen<BookMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/gui_book.png");

    public GUIBook(BookMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("container.hbm_m.book.extended_crafting"), 28, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("container.hbm_m.book.standard_inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.blit(TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight);

        if(!this.menu.craftResult.getItem(0).isEmpty())
            guiGraphics.blit(TEXTURE, left + 29, top + 16, 176, 0, 54, 54);
    }
}
