package com.hbm_m.interfaces;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.energy.PowerConductor;
import com.hbm_m.api.network.UniNodespace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * Порт IEnergyReceiverMK2 из 1.7.10 (energymk2).
 * Если объект получает энергию — реализует этот интерфейс.
 */
public interface IEnergyReceiver extends IEnergyConnector {

    long getEnergyStored();
    long getMaxEnergyStored();
    void setEnergyStored(long energy);

    /** Максимальная скорость приема энергии за тик (аналог getReceiverSpeed). */
    long getReceiveSpeed();

    /** Приоритет получения энергии в сети. */
    Priority getPriority();

    /**
     * Принять энергию в приемник.
     * @return Фактически принятое количество энергии
     */
    long receiveEnergy(long maxReceive, boolean simulate);

    boolean canReceive();

    /**
     * Аналог transferPower из 1.7.10: принимает энергию, возвращает перелив (overshoot).
     */
    default long transferPower(long power) {
        if (power + this.getEnergyStored() <= this.getMaxEnergyStored()) {
            this.setEnergyStored(power + this.getEnergyStored());
            return 0;
        }
        long capacity = this.getMaxEnergyStored() - this.getEnergyStored();
        long overshoot = power - capacity;
        this.setEnergyStored(this.getMaxEnergyStored());
        return overshoot;
    }

    /**
     * Аналог getReceiverSpeed по умолчанию в оригинале: maxPower.
     * Здесь скорость задаётся явно, поэтому default не используется.
     */

    /** Подписаться на сеть соседнего проводника (аналог trySubscribe из 1.7.10). */
    default boolean trySubscribe(ServerLevel level, int x, int y, int z, Direction dir) {

        if (!(level.getBlockEntity(new BlockPos(x, y, z)) instanceof PowerConductor con)) return false;
        if (!con.canConnectEnergy(dir.getOpposite())) return false;

        Nodespace.PowerNode node = Nodespace.getNode(level, new BlockPos(x, y, z));

        if (node != null && node.net != null && node.net.isValid()) {
            node.net.addReceiver(this);
            return true;
        }
        return false;
    }

    default void tryUnsubscribe(ServerLevel level, int x, int y, int z) {

        if (!(level.getBlockEntity(new BlockPos(x, y, z)) instanceof PowerConductor)) return;

        Nodespace.PowerNode node = Nodespace.getNode(level, new BlockPos(x, y, z));

        if (node != null && node.net != null) {
            node.net.removeReceiver(this);
        }
    }

    /**
     * Приоритеты соединений — порт ConnectionPriority из 1.7.10 (5 уровней).
     * Распределение идет от HIGHEST к LOWEST.
     *
     * ВАЖНО для совместимости сохранений: исторические значения LOW/NORMAL/HIGH
     * сохранили свои ординалы (1/2/3), новые уровни добавлены по краям,
     * как в оригинальном enum'е energymk2.
     */
    enum Priority {
        LOWEST,   // 0
        LOW,      // 1
        NORMAL,   // 2
        HIGH,     // 3
        HIGHEST   // 4
    }
}
