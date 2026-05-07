package com.hbm_m.api.fluids;

import net.minecraft.world.level.material.Fluid;

/**
 * Поставщик жидкости в MK2 сети.
 * Аналог IFluidProviderMK2 из 1.7.10.
 */
public interface IFluidProviderMK2 extends IFluidUserMK2 {

    /** Сколько жидкости этого типа/давления доступно для передачи. */
    long getFluidAvailable(Fluid fluid, int pressure);

    /** Снять указанное количество жидкости из источника. */
    void useUpFluid(Fluid fluid, int pressure, long amount);

    /** Максимальная скорость передачи за тик. По умолчанию без ограничений. */
    default long getProviderSpeed(Fluid fluid, int pressure) {
        return 1_000_000_000L;
    }

    /** Диапазон давлений [min, max], которые этот провайдер может отдавать. */
    default int[] getProvidingPressureRange(Fluid fluid) {
        return IFluidUserMK2.DEFAULT_PRESSURE_RANGE;
    }

    /**
     * Флаг для сетевого обхода балансировщика: этот провайдер должен рассматриваться как "бесконечный источник"
     * для данного типа жидкости. Сеть может использовать это, чтобы заполнить всех получателей за один тик.
     *
     * По умолчанию выключено.
     */
    default boolean isInfiniteNetworkSource(Fluid fluid) {
        return false;
    }
}
