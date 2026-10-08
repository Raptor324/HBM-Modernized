package com.hbm_m.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.items.ICustomizable}: Gegenstaende, die sich per {@code /ntmcustomize} anpassen lassen. */
public interface ICustomizable {

    void customize(Player player, ItemStack stack, String... args);
}
