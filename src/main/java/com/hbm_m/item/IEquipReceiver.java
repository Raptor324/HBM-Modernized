package com.hbm_m.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code IEquipReceiver}: wird serverseitig aufgerufen, wenn der Spieler den Gegenstand in die Hand nimmt. */
public interface IEquipReceiver {

    void onEquip(Player player, ItemStack stack);
}
