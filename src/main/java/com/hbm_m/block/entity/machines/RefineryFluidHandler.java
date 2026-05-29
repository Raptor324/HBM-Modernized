package com.hbm_m.block.entity.machines;

//? if forge {
import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Forge fluid capability bridge for Refinery.
 * Tank 0 = input, tanks 1..4 = outputs.
 */
public class RefineryFluidHandler implements IFluidHandler {

    private final MachineRefineryBlockEntity blockEntity;

    public RefineryFluidHandler(MachineRefineryBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int getTanks() {
        return 5;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        FluidTank target = blockEntity.getTank(tank);
        if (target == null || target.getFill() <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(target.getTankType(), target.getFill());
    }

    @Override
    public int getTankCapacity(int tank) {
        return blockEntity.getTank(tank).getMaxFill();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        if (stack.isEmpty()) return false;
        if (tank != MachineRefineryBlockEntity.TANK_INPUT) return false;
        return blockEntity.canAcceptInputFluid(stack.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !blockEntity.canAcceptInputFluid(resource.getFluid())) {
            return 0;
        }

        FluidTank input = blockEntity.getTank(MachineRefineryBlockEntity.TANK_INPUT);
        int maxInsert = Math.min(resource.getAmount(), input.getMaxFill() - input.getFill());
        if (maxInsert <= 0) return 0;

        if (action == FluidAction.SIMULATE) {
            return maxInsert;
        }

        return input.fillMb(resource.getFluid(), maxInsert);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;

        for (int tankIndex = 1; tankIndex <= 4; tankIndex++) {
            FluidTank out = blockEntity.getTank(tankIndex);
            if (out.getFill() <= 0) continue;
            if (!VanillaFluidEquivalence.sameSubstance(out.getTankType(), resource.getFluid())) continue;

            int planned = Math.min(resource.getAmount(), out.getFill());
            if (planned <= 0) return FluidStack.EMPTY;

            int drained = action == FluidAction.EXECUTE ? out.drainMb(planned) : planned;
            return drained > 0 ? new FluidStack(out.getTankType(), drained) : FluidStack.EMPTY;
        }

        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;

        for (int tankIndex = 1; tankIndex <= 4; tankIndex++) {
            FluidTank out = blockEntity.getTank(tankIndex);
            if (out.getFill() <= 0) continue;

            int planned = Math.min(maxDrain, out.getFill());
            int drained = action == FluidAction.EXECUTE ? out.drainMb(planned) : planned;
            if (drained > 0) {
                return new FluidStack(out.getTankType(), drained);
            }
        }

        return FluidStack.EMPTY;
    }
}
//?}