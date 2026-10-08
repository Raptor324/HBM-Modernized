package com.hbm_m.item;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Einfaches Item mit Crafting-Rest, 1:1 zu {@code Item.setContainerItem(...)} des Originals
 * (z. B. Zellen -> cell_empty). Der Rest wird lazy aufgeloest, damit die Registrierungsreihenfolge egal ist.
 */
public class ContainerItem extends Item {

    private final Supplier<? extends Item> container;

    public ContainerItem(Properties properties, Supplier<? extends Item> container) {
        super(properties);
        this.container = container;
    }

    //? if forge {
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(container.get());
    }
    //?}
}
