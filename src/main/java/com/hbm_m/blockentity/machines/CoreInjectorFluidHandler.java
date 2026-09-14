package com.hbm_m.blockentity.machines;

//? if forge {
import org.jetbrains.annotations.NotNull;

import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Forge-Fluid-Capability-Bruecke fuer den Core Injector: Tank 0 = Deuterium, Tank 1 = Tritium
 * (1:1 aus {@code TileEntityCoreInjector.tanks}). Erlaubt es Pumpen/Roehren, die beiden Tanks von
 * aussen zu befuellen; die eigentliche Weitergabe an ein Ziel entlang des Strahls erledigt
 * {@link MachineCoreInjectorBlockEntity#tick}.
 */
public class CoreInjectorFluidHandler implements IFluidHandler {

    private final MachineCoreInjectorBlockEntity blockEntity;

    public CoreInjectorFluidHandler(MachineCoreInjectorBlockEntity blockEntity) {
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
        FluidTank target = blockEntity.getTank(tank);
        return target == null ? 0 : target.getMaxFill();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        FluidTank target = blockEntity.getTank(tank);
        return target != null && target.isFluidValid(stack.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;

        for (int i = 0; i < 2; i++) {
            FluidTank target = blockEntity.getTank(i);
            if (!target.isFluidValid(resource.getFluid())) continue;

            int maxInsert = Math.min(resource.getAmount(), target.getSpaceMb());
            if (maxInsert <= 0) continue;

            if (action == FluidAction.SIMULATE) return maxInsert;
            return target.fillMb(resource.getFluid(), maxInsert);
        }

        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;

        for (int i = 0; i < 2; i++) {
            FluidTank target = blockEntity.getTank(i);
            if (target.getFill() <= 0 || target.getTankType() != resource.getFluid()) continue;

            int planned = Math.min(resource.getAmount(), target.getFill());
            int drained = action == FluidAction.EXECUTE ? target.drainMb(planned) : planned;
            return drained > 0 ? new FluidStack(target.getTankType(), drained) : FluidStack.EMPTY;
        }

        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;

        for (int i = 0; i < 2; i++) {
            FluidTank target = blockEntity.getTank(i);
            if (target.getFill() <= 0) continue;

            int planned = Math.min(maxDrain, target.getFill());
            int drained = action == FluidAction.EXECUTE ? target.drainMb(planned) : planned;
            if (drained > 0) {
                return new FluidStack(target.getTankType(), drained);
            }
        }

        return FluidStack.EMPTY;
    }
}
//?}
