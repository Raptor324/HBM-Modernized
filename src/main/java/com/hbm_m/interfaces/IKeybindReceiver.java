package com.hbm_m.interfaces;

import com.hbm_m.handler.EnumKeybind;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.items.IKeybindReceiver}: Gegenstaende in der Hand, die auf HBM-Tasten reagieren. */
public interface IKeybindReceiver {

    boolean canHandleKeybind(Player player, ItemStack stack, EnumKeybind keybind);

    void handleKeybind(Player player, ItemStack stack, EnumKeybind keybind, boolean state);

    default void handleKeybindClient(Player player, ItemStack stack, EnumKeybind keybind, boolean state) { }
}
