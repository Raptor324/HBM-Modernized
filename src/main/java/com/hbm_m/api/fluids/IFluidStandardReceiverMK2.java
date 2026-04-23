package com.hbm_m.api.fluids;

import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.world.level.material.Fluid;

/**
 * IFluidReceiverMK2 со стандартной реализацией через FluidTank[].
 * Аналог IFluidStandardReceiverMK2 из 1.7.10.
 */
public interface IFluidStandardReceiverMK2 extends IFluidReceiverMK2 {

    FluidTank[] getReceivingTanks();

    @Override
    default long getDemand(Fluid fluid, int pressure) {
        long amount = 0;
        for (FluidTank t : getReceivingTanks()) {
            if (t.getTankType() == fluid && t.getPressure() == pressure) {
                amount += (t.getMaxFill() - t.getFill());
            }
        }
        return amount;
    }

    @Override
    default long transferFluid(Fluid fluid, int pressure, long amount) {
        int tanks = 0;
        for (FluidTank t : getReceivingTanks()) {
            if (t.getTankType() == fluid && t.getPressure() == pressure) tanks++;
        }
        // Первый проход: равномерное распределение
        if (tanks > 1) {
            int share = (int) Math.floor((double) amount / tanks);
            for (FluidTank t : getReceivingTanks()) {
                if (t.getTankType() == fluid && t.getPressure() == pressure) {
                    int add = Math.min(share, t.getMaxFill() - t.getFill());
                    t.fill(t.getFill() + add);
                    amount -= add;
                }
            }
        }
        // Второй проход: добрать остаток по порядку
        if (amount > 0) {
            for (FluidTank t : getReceivingTanks()) {
                if (t.getTankType() == fluid && t.getPressure() == pressure) {
                    int add = (int) Math.min(amount, t.getMaxFill() - t.getFill());
                    t.fill(t.getFill() + add);
                    amount -= add;
                }
            }
        }
        return amount; // возвращаем остаток, который не поместился
    }

    @Override
    default int[] getReceivingPressureRange(Fluid fluid) {
        int lowest = IFluidUserMK2.HIGHEST_VALID_PRESSURE;
        int highest = 0;
        for (FluidTank t : getReceivingTanks()) {
            if (t.getTankType() == fluid) {
                if (t.getPressure() < lowest)  lowest  = t.getPressure();
                if (t.getPressure() > highest) highest = t.getPressure();
            }
        }
        return lowest <= highest ? new int[]{ lowest, highest } : IFluidUserMK2.DEFAULT_PRESSURE_RANGE;
    }
}
