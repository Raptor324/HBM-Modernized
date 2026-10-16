package com.hbm_m.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class PlayerPersistentData {
    private PlayerPersistentData() {}

    public static CompoundTag get(Player player) {
        return player.getPersistentData();
    }
}