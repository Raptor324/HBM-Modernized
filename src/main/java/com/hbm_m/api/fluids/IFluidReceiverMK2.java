package com.hbm_m.api.fluids;

import com.hbm_m.api.network.UniNodespace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

/**
 * Получатель жидкости в MK2 сети.
 * Аналог IFluidReceiverMK2 из 1.7.10.
 */
public interface IFluidReceiverMK2 extends IFluidUserMK2 {

    /**
     * Принять {@code amount} единиц жидкости.
     * @return остаток, который не удалось принять (0 = всё принято)
     */
    long transferFluid(Fluid fluid, int pressure, long amount);

    /** Сколько жидкости этого типа/давления можно принять. */
    long getDemand(Fluid fluid, int pressure);

    /** Максимальная скорость приёма за тик. По умолчанию без ограничений. */
    default long getReceiverSpeed(Fluid fluid, int pressure) {
        return 1_000_000_000L;
    }

    /** Диапазон давлений, которые этот получатель принимает. */
    default int[] getReceivingPressureRange(Fluid fluid) {
        return IFluidUserMK2.DEFAULT_PRESSURE_RANGE;
    }

    /** Приоритет в сети. */
    default ConnectionPriority getFluidPriority() {
        return ConnectionPriority.NORMAL;
    }

    /**
     * Флаг для сетевого обхода балансировщика: этот получатель должен рассматриваться как "бесконечный сток/утилизатор"
     * для данного типа жидкости. Сеть может использовать это, чтобы опустошить всех провайдеров за один тик.
     *
     * По умолчанию выключено.
     */
    default boolean isInfiniteNetworkSink(Fluid fluid) {
        return false;
    }

    /**
     * Зарегистрировать себя как получатель в сети трубы по указанному адресу.
     * Вызывается каждый тик машиной для подписки.
     *
     * @param fluid   тип жидкости
     * @param level   серверный уровень
     * @param pipePos позиция трубы-соседа
     * @param dirFromMeToPipe направление от этой машины к трубе
     */
    default void trySubscribe(Fluid fluid, Level level, BlockPos pipePos, Direction dirFromMeToPipe) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockEntity be = level.getBlockEntity(pipePos);
        if (!(be instanceof IFluidConnectorMK2 con)) return;
        if (!con.canConnect(fluid, dirFromMeToPipe.getOpposite())) return;

        var node = UniNodespace.getNode(serverLevel, pipePos, FluidNetProvider.forFluid(fluid));
        if (node != null && node.net != null) {
            node.net.addReceiver(this);
        }
    }
}
