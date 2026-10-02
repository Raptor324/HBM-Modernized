package com.hbm_m.item.special;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Partikelkapsel mit Behaelter: Original {@code setContainerItem(ModItems.particle_empty)}. Der leere Behaelter wird
 * erst beim Abfragen aufgeloest, weil einige Kapseln vor {@code particle_empty} registriert werden.
 */
public class ItemParticleCapsule extends Item {

    public ItemParticleCapsule(Properties properties) {
        super(properties);
    }

    public static ItemStack emptyCapsule() {
        return new ItemStack(com.hbm_m.item.ModItems.PARTICLE_EMPTY.get());
    }

    //? if forge {
    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return emptyCapsule();
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }
    //?}
}
