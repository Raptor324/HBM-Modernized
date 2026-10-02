package com.hbm_m.api.tile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** 1:1 {@code IControlReceiver}: Empfaenger von {@code NBTControlPacket}. */
public interface IControlReceiver {

    boolean hasPermission(Player player);

    void receiveControl(CompoundTag data);
}
