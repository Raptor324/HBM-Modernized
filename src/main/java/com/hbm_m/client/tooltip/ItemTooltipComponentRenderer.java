package com.hbm_m.client.tooltip;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

public class ItemTooltipComponentRenderer implements ClientTooltipComponent {
    private final ItemStack stack;

    public ItemTooltipComponentRenderer(ItemTooltipComponent component) {
        this.stack = component.getStack();
    }

    @Override
    public int getHeight() {
        // Высота иконки (16px) + 2px отступ
        return 18;
    }

    @Override
    public int getWidth(@NotNull Font pFont) {
        // Ширина иконки (16px) + ширина текста + отступ
        return 18 + pFont.width(stack.getHoverName());
    }

    @Override
    public void renderImage(@NotNull Font pFont, int pX, int pY, @NotNull GuiGraphics pGuiGraphics) {
        // Рендерим иконку предмета
        pGuiGraphics.renderFakeItem(this.stack, pX, pY);
        // Рендерим название предмета рядом с иконкой
        pGuiGraphics.drawString(pFont, this.stack.getHoverName(), pX + 20, pY + 5, 0xFFFFFF); // Белый цвет
    }
}