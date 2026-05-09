package com.hbm_m.api.fluids;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * IFluidReceiverMK2 со стандартной реализацией через FluidTank[].
 * Аналог IFluidStandardReceiverMK2 из 1.7.10.
 */
public interface IFluidStandardReceiverMK2 extends IFluidReceiverMK2 {

    FluidTank[] getReceivingTanks();

    /**
     * Как у {@link FluidTank#isFluidValid}: без фиксации типа (NONE/EMPTY-сентинел) пустой бак может
     * принять сетевую жидкость; после заливки тип выставляет {@link FluidTank#fillMb}.
     *
     * <p>Публично для переопределений {@link #getDemand}/{@link #transferFluid}, где нужно ограничить множество баков
     * (например, химическая установка — только слоты из текущего рецепта).
     */
    static boolean receiverTankMatches(FluidTank t, Fluid fluid, int pressure) {
        if (t.getPressure() != pressure) return false;
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) return false;
        Fluid cfg = t.getTankType();
        if (t.getFill() <= 0 && !FluidTank.isFluidTypeExplicitlySet(cfg)) {
            return true;
        }
        return VanillaFluidEquivalence.sameSubstance(cfg, fluid);
    }

    static Fluid receiverResolveFillFluid(FluidTank t, Fluid networkFluid) {
        Fluid configured = t.getTankType();
        if (configured == Fluids.EMPTY || configured == ModFluids.NONE.getSource()) {
            return VanillaFluidEquivalence.forVanillaContainerFill(networkFluid);
        }
        return configured;
    }

    static long receiverFillReceivingTankMb(FluidTank t, Fluid networkFluid, long amountMb) {
        if (amountMb <= 0) return 0L;
        long done = 0L;
        long left = amountMb;
        Fluid fillFluid = receiverResolveFillFluid(t, networkFluid);
        while (left > 0) {
            int space = Math.max(0, t.getMaxFill() - t.getFill());
            if (space <= 0) break;
            int chunk = (int) Math.min(Math.min(left, space), Integer.MAX_VALUE);
            if (chunk <= 0) break;
            int filled = t.fillMb(fillFluid, chunk);
            if (filled <= 0) break;
            done += filled;
            left -= filled;
            fillFluid = receiverResolveFillFluid(t, networkFluid);
        }
        return done;
    }

    @Override
    default long getDemand(Fluid fluid, int pressure) {
        long amount = 0;
        for (FluidTank t : getReceivingTanks()) {
            if (!receiverTankMatches(t, fluid, pressure)) continue;
            amount += (long) Math.max(0, t.getMaxFill() - t.getFill());
        }
        return amount;
    }

    @Override
    default long transferFluid(Fluid fluid, int pressure, long amount) {
        if (amount <= 0 || fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) return amount;

        int tanksMatching = 0;
        for (FluidTank t : getReceivingTanks()) {
            if (receiverTankMatches(t, fluid, pressure)) tanksMatching++;
        }
        long remain = amount;
        // Первый проход — равные доли
        if (tanksMatching > 1 && remain > 0) {
            long share = (long) Math.floor((double) remain / tanksMatching);
            for (FluidTank t : getReceivingTanks()) {
                if (!receiverTankMatches(t, fluid, pressure)) continue;
                long got = receiverFillReceivingTankMb(t, fluid, Math.min(share, remain));
                remain -= got;
                if (remain <= 0) return 0L;
            }
        }
        // Второй проход — добираем порядком
        if (remain > 0) {
            for (FluidTank t : getReceivingTanks()) {
                if (!receiverTankMatches(t, fluid, pressure)) continue;
                long got = receiverFillReceivingTankMb(t, fluid, remain);
                remain -= got;
                if (remain <= 0) break;
            }
        }
        return remain;
    }

    @Override
    default int[] getReceivingPressureRange(Fluid fluid) {
        int lowest = IFluidUserMK2.HIGHEST_VALID_PRESSURE;
        int highest = 0;
        for (FluidTank t : getReceivingTanks()) {
            if (!receiverTankMatches(t, fluid, t.getPressure())) continue;
            int pr = t.getPressure();
            if (pr < lowest) lowest = pr;
            if (pr > highest) highest = pr;
        }
        return lowest <= highest ? new int[]{ lowest, highest } : IFluidUserMK2.DEFAULT_PRESSURE_RANGE;
    }
}
