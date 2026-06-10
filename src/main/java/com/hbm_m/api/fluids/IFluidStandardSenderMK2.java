package com.hbm_m.api.fluids;

import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * IFluidProviderMK2 со стандартной реализацией через FluidTank[].
 * Аналог IFluidStandardSenderMK2 из 1.7.10.
 *
 * Двойной маршрут:
 *   1) Регистрация в сети трубы-соседа (addProvider).
 *   2) Прямой перенос к соседнему IFluidReceiverMK2 без сети.
 */
public interface IFluidStandardSenderMK2 extends IFluidProviderMK2 {

    FluidTank[] getSendingTanks();

    // --- tryProvide overloads ---

    default void tryProvide(FluidTank tank, Level level, BlockPos pipePos, Direction dirFromMeToPipe) {
        tryProvide(tank.getTankType(), tank.getPressure(), level, pipePos, dirFromMeToPipe);
    }

    default void tryProvide(Fluid fluid, Level level, BlockPos pipePos, Direction dirFromMeToPipe) {
        tryProvide(fluid, 0, level, pipePos, dirFromMeToPipe);
    }

    default void tryProvide(Fluid fluid, int pressure, Level level, BlockPos pipePos, Direction dirFromMeToPipe) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (fluid == Fluids.EMPTY) return;

        BlockEntity neighborBe = level.getBlockEntity(pipePos);

        // 1) Регистрация в сети трубы
        if (neighborBe instanceof IFluidConnectorMK2 con) {
            if (con.canConnect(fluid, dirFromMeToPipe.getOpposite())) {
                var node = UniNodespace.getNode(serverLevel, pipePos, FluidNetProvider.forFluid(fluid));
                if (node != null && node.net != null) {
                    node.net.addProvider(this);
                }
            }
        }

        // 2) Прямой перенос к соседнему IFluidReceiverMK2 (без сети)
        if (neighborBe != this && neighborBe instanceof IFluidReceiverMK2 rec) {
            if (rec.canConnect(fluid, dirFromMeToPipe.getOpposite())) {
                long provides  = Math.min(getFluidAvailable(fluid, pressure), getProviderSpeed(fluid, pressure));
                long receives  = Math.min(rec.getDemand(fluid, pressure),     rec.getReceiverSpeed(fluid, pressure));
                long toTransfer = Math.min(provides, receives);
                toTransfer -= rec.transferFluid(fluid, pressure, toTransfer);
                useUpFluid(fluid, pressure, toTransfer);
            }
        }
    }

    // --- Standard provider implementation ---

    @Override
    default long getFluidAvailable(Fluid fluid, int pressure) {
        long amount = 0;
        for (FluidTank tank : getSendingTanks()) {
            if (VanillaFluidEquivalence.sameSubstance(tank.getTankType(), fluid) && tank.getPressure() == pressure) {
                amount += tank.getFill();
            }
        }
        return amount;
    }

    @Override
    default void useUpFluid(Fluid fluid, int pressure, long amount) {
        int tanks = 0;
        for (FluidTank t : getSendingTanks()) {
            if (VanillaFluidEquivalence.sameSubstance(t.getTankType(), fluid) && t.getPressure() == pressure) tanks++;
        }
        if (tanks > 1) {
            int share = (int) Math.floor((double) amount / tanks);
            for (FluidTank t : getSendingTanks()) {
                if (VanillaFluidEquivalence.sameSubstance(t.getTankType(), fluid) && t.getPressure() == pressure) {
                    int rem = Math.min(share, t.getFill());
                    // Используем drainMb, чтобы корректно сработали platform storage + onContentsChanged (GUI sync, pipes, etc.)
                    // На Fabric простое setFill/fill может менять amount без транзакции и не дергать onFinalCommit.
                    t.drainMb(rem);
                    amount -= rem;
                }
            }
        }
        if (amount > 0) {
            for (FluidTank t : getSendingTanks()) {
                if (VanillaFluidEquivalence.sameSubstance(t.getTankType(), fluid) && t.getPressure() == pressure) {
                    int rem = (int) Math.min(amount, t.getFill());
                    t.drainMb(rem);
                    amount -= rem;
                }
            }
        }
    }

    @Override
    default int[] getProvidingPressureRange(Fluid fluid) {
        int lowest = IFluidUserMK2.HIGHEST_VALID_PRESSURE;
        int highest = 0;
        for (FluidTank t : getSendingTanks()) {
            if (VanillaFluidEquivalence.sameSubstance(t.getTankType(), fluid)) {
                if (t.getPressure() < lowest)  lowest  = t.getPressure();
                if (t.getPressure() > highest) highest = t.getPressure();
            }
        }
        return lowest <= highest ? new int[]{ lowest, highest } : IFluidUserMK2.DEFAULT_PRESSURE_RANGE;
    }
}
