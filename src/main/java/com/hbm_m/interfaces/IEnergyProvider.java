package com.hbm_m.interfaces;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.energy.PowerConductor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт IEnergyProviderMK2 из 1.7.10 (energymk2).
 */
public interface IEnergyProvider extends IEnergyConnector {

    long getEnergyStored();
    long getMaxEnergyStored();
    void setEnergyStored(long energy);

    /** Максимальная скорость отдачи энергии за тик (аналог getProviderSpeed). */
    long getProvideSpeed();

    long extractEnergy(long maxExtract, boolean simulate);

    boolean canExtract();

    /** Аналог usePower из 1.7.10. */
    default void usePower(long power) {
        this.extractEnergy(power, false);
    }

    /**
     * Аналог tryProvide из 1.7.10 (IEnergyProviderMK2): 1:1 структура —
     * 1) подписка как поставщика к сети соседнего проводника;
     * 2) direct provision — соседний приемник с allowDirectProvision получает
     *    энергию напрямую, минуя сеть (проверяется canConnect стороны приемника).
     * Обе ветки исполняются последовательно, как в оригинале.
     */
    default boolean tryProvide(ServerLevel level, int x, int y, int z, Direction dir) {

        boolean any = false;
        BlockEntity neighbor = level.getBlockEntity(new BlockPos(x, y, z));

        // Ветка 1: подписка на сеть проводника
        if (neighbor instanceof PowerConductor con && con.canConnectEnergy(dir.getOpposite())) {
            Nodespace.PowerNode node = Nodespace.getNode(level, new BlockPos(x, y, z));
            if (node != null && node.net != null && node.net.isValid()) {
                node.net.addProvider(this);
                any = true;
            }
        }

        // Ветка 2: прямое питание соседа-приемника (в т.ч. буферной батареи-проводника)
        if (neighbor instanceof IEnergyReceiver rec
                && rec.canConnectEnergy(dir.getOpposite())
                && rec.allowDirectProvision()) {

            // Паритет с оригиналом: передача ограничена спросом приемника
            // min(maxPower - power, receiverSpeed), а не только емкостью
            long receives = Math.min(
                    rec.getMaxEnergyStored() - rec.getEnergyStored(),
                    rec.getReceiveSpeed());

            long provide = Math.min(Math.min(this.getEnergyStored(), this.getProvideSpeed()), receives);
            if (provide > 0 && rec.canReceive()) {
                long overshoot = rec.transferPower(provide);
                this.usePower(provide - overshoot);
                any = true;
            }
        }

        return any;
    }

    /**
     * Аналог allowDirectProvision: может ли провайдер питать приемник напрямую
     * при касании блоков, минуя сеть.
     */
    default boolean allowDirectProvision() { return true; }
}
