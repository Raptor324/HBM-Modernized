package com.hbm_m.item.tool;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemCraftingDegradation} (chemistry_set / chemistry_set_boron): bleibt beim Handwerk im
 * Raster und nutzt sich je Rezept um 1 ab (Haltbarkeit 0 = unzerstoerbar); nicht reparierbar.
 */
public class ItemCraftingDegradation extends Item {

    public ItemCraftingDegradation(int durability, Properties properties) {
        super(durability > 0 ? properties.durability(durability).setNoRepair() : properties.stacksTo(1).setNoRepair());
    }

    //? if forge || neoforge {
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        if (copy.getMaxDamage() > 0) {
            copy.setDamageValue(copy.getDamageValue() + 1);
        }
        return copy;
    }
    //?}
}
