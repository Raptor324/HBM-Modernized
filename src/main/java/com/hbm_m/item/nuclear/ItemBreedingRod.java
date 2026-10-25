package com.hbm_m.item.nuclear;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemBreedingRod} ({@code rod}, {@code rod_dual}, {@code rod_quad}): Brutstab fuer den Brutreaktor.
 * Original {@code setContainerItem(rod_empty / rod_dual_empty / rod_quad_empty)}: beim Verarbeiten in der
 * Werkbank bleibt der leere Stab zurueck.
 */
public class ItemBreedingRod extends Item {

    private final BreedingRodType type;
    private final Supplier<Item> container;

    public ItemBreedingRod(Properties properties, BreedingRodType type, Supplier<Item> container) {
        super(properties);
        this.type = type;
        this.container = container;
    }

    public BreedingRodType getType() {
        return type;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(container.get());
    }
}
