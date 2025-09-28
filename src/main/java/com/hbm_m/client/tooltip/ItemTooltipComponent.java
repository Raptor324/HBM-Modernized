package com.hbm_m.client.tooltip;

// Компонент подсказки для отображения ItemStack в подсказках.
// Используется для отображения предметов внутри подсказок, например, в кастомных подсказках блоков или предметов.
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class ItemTooltipComponent implements TooltipComponent {
    private final ItemStack stack;

    public ItemTooltipComponent(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getStack() {
        return stack;
    }
}