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
        // Без ветки neoforge метод возвращал НОВЫЙ пустой тег, который никуда не сохраняется!
        //? if fabric {
        /*return com.hbm_m.capability.FabricPlayerComponents.getPersistentData(player);
        *///?} elif forge {
        return player.getPersistentData();
        //?} elif neoforge {
        /*return player.getPersistentData();
        *///?}
    }
}

