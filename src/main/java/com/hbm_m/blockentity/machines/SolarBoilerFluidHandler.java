package com.hbm_m.blockentity.machines;

//? if forge {
import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Forge fluid capability bridge for the Solar Boiler.
 * Tank 0 = water (input only), tank 1 = steam (output only).
 */
public class SolarBoilerFluidHandler implements IFluidHandler {

    private final MachineSolarBoilerBlockEntity blockEntity;

    public SolarBoilerFluidHandler(MachineSolarBoilerBlockEntity blockEntity) {
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
        if (tank != MachineSolarBoilerBlockEntity.TANK_WATER) return false;
        return VanillaFluidEquivalence.sameSubstance(stack.getFluid(), blockEntity.getTank(MachineSolarBoilerBlockEntity.TANK_WATER).getTankType());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;

        FluidTank water = blockEntity.getTank(MachineSolarBoilerBlockEntity.TANK_WATER);
        if (!VanillaFluidEquivalence.sameSubstance(resource.getFluid(), water.getTankType())) return 0;

        int maxInsert = Math.min(resource.getAmount(), water.getMaxFill() - water.getFill());
        if (maxInsert <= 0) return 0;

        if (action == FluidAction.SIMULATE) {
            return maxInsert;
        }

        return water.fillMb(resource.getFluid(), maxInsert);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;

        FluidTank steam = blockEntity.getTank(MachineSolarBoilerBlockEntity.TANK_STEAM);
        if (steam.getFill() <= 0) return FluidStack.EMPTY;
        if (!VanillaFluidEquivalence.sameSubstance(steam.getTankType(), resource.getFluid())) return FluidStack.EMPTY;

        int planned = Math.min(resource.getAmount(), steam.getFill());
        if (planned <= 0) return FluidStack.EMPTY;

        int drained = action == FluidAction.EXECUTE ? steam.drainMb(planned) : planned;
        return drained > 0 ? new FluidStack(steam.getTankType(), drained) : FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;

        FluidTank steam = blockEntity.getTank(MachineSolarBoilerBlockEntity.TANK_STEAM);
        if (steam.getFill() <= 0) return FluidStack.EMPTY;

        int planned = Math.min(maxDrain, steam.getFill());
        int drained = action == FluidAction.EXECUTE ? steam.drainMb(planned) : planned;
        return drained > 0 ? new FluidStack(steam.getTankType(), drained) : FluidStack.EMPTY;
    }
}
//?}
