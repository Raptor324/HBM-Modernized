package com.hbm_m.blockentity.machines;

//? if forge {
import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Forge fluid capability bridge for the Cooling Tower (Small).
 * Tank 0 = hot coolant input, tank 1 = cooled coolant output.
 */
public class TowerSmallFluidHandler implements IFluidHandler {

    private final MachineTowerSmallBlockEntity blockEntity;

    public TowerSmallFluidHandler(MachineTowerSmallBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int getTanks() {
        return 2;
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
        if (tank != MachineTowerSmallBlockEntity.TANK_HOT_IN) return false;
        return blockEntity.getTank(MachineTowerSmallBlockEntity.TANK_HOT_IN).isFluidValid(stack.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;

        FluidTank input = blockEntity.getTank(MachineTowerSmallBlockEntity.TANK_HOT_IN);
        if (!input.isFluidValid(resource.getFluid())) return 0;

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

        FluidTank out = blockEntity.getTank(MachineTowerSmallBlockEntity.TANK_COOLANT_OUT);
        if (out.getFill() <= 0) return FluidStack.EMPTY;
        if (!VanillaFluidEquivalence.sameSubstance(out.getTankType(), resource.getFluid())) return FluidStack.EMPTY;

        int planned = Math.min(resource.getAmount(), out.getFill());
        if (planned <= 0) return FluidStack.EMPTY;

        int drained = action == FluidAction.EXECUTE ? out.drainMb(planned) : planned;
        return drained > 0 ? new FluidStack(out.getTankType(), drained) : FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;

        FluidTank out = blockEntity.getTank(MachineTowerSmallBlockEntity.TANK_COOLANT_OUT);
        if (out.getFill() <= 0) return FluidStack.EMPTY;

        int planned = Math.min(maxDrain, out.getFill());
        int drained = action == FluidAction.EXECUTE ? out.drainMb(planned) : planned;
        return drained > 0 ? new FluidStack(out.getTankType(), drained) : FluidStack.EMPTY;
    }
}
//?}
