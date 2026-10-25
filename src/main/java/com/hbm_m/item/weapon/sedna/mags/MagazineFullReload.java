package com.hbm_m.item.weapon.sedna.mags;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** 1:1: Uses individual bullets which are loaded all at once */
public class MagazineFullReload extends MagazineSingleTypeBase {

    public MagazineFullReload(int index, int capacity) {
        super(index, capacity);
    }

    /** Reloads all rounds at once. If the mag is empty, the mag's type will change to the first valid ammo type */
    @Override
    public void reloadAction(ItemStack stack, @Nullable Container inventory) {
        standardReload(stack, inventory, this.capacity);
    }
}
