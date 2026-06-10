package com.hbm_m.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Мультилоадерный доступ к "персистентному NBT" игрока.
 *
 * - Forge: {@code player.getPersistentData()}
 * - Fabric: CCA entity component (см. {@code FabricPlayerComponents})
 */
public final class PlayerPersistentData {
    private PlayerPersistentData() {}

    public static CompoundTag get(Player player) {
        CompoundTag tag = new CompoundTag();
        //? if fabric {
        /*tag = com.hbm_m.capability.FabricPlayerComponents.getPersistentData(player);
        *///?}
        //? if forge {
        tag = player.getPersistentData();
        //?}
        return tag;
    }
}

