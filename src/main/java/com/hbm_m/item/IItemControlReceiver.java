package com.hbm_m.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for items that receive control data from client (e.g. via packets).
 * Used by fluid identifier GUI to apply primary/secondary fluid selection.
 */
public interface IItemControlReceiver {

    void receiveControl(ItemStack stack, CompoundTag data);
}
