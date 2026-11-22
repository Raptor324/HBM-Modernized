package com.hbm_m.interfaces;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Порт com.hbm.interfaces.IControlReceiver (1.7.10): приём управляющих NBT-команд
 * из GUI (кнопки клапана/зажигания, дроссель и т.п.) через
 * {@link com.hbm_m.network.MachineControlC2SPacket} - аналог NBTControlPacket.
 */
public interface IControlReceiver {

    void receiveControl(CompoundTag data);

    /** Оригинал: дистанционная проверка (факел ≤ 16 блоков, двигатель < 25). */
    default boolean hasPermission(Player player) {
        return true;
    }
}
