package com.hbm_m.interfaces;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for items that receive control data from client (e.g. via packets).
 * Used by fluid identifier GUI to apply primary/secondary fluid selection.
 */
public interface IItemControlReceiver {

    void receiveControl(ItemStack stack, CompoundTag data);

    /** Original {@code receiveControl(EntityPlayer, ItemStack, NBTTagCompound)}; Standard ohne Spieler. */
    default void receiveControl(net.minecraft.world.entity.player.Player player, ItemStack stack, CompoundTag data) {
        receiveControl(stack, data);
    }
}
