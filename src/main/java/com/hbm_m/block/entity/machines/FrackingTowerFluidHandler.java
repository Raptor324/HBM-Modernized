package com.hbm_m.block.entity.machines;
//? if forge {
import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Обработчик жидкостей для Fracking Tower.
 * Управляет тремя танками: нефть (выход), газ (выход), FrackSol (вход).
 */
public class FrackingTowerFluidHandler implements IFluidHandler {

    private final MachineFrackingTowerBlockEntity blockEntity;

    public FrackingTowerFluidHandler(MachineFrackingTowerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int getTanks() {
        return 3;
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return switch (tank) {
            case 0 -> {
                var t = blockEntity.getOilTank();
                yield t.getFluidAmountMb() > 0
                        ? new FluidStack(t.getStoredFluid(), t.getFluidAmountMb())
                        : FluidStack.EMPTY;
            }
            case 1 -> {
                var t = blockEntity.getGasTank();
                yield t.getFluidAmountMb() > 0
                        ? new FluidStack(t.getStoredFluid(), t.getFluidAmountMb())
                        : FluidStack.EMPTY;
            }
            case 2 -> {
                var t = blockEntity.getFracksolTank();
                yield t.getFluidAmountMb() > 0
                        ? new FluidStack(t.getStoredFluid(), t.getFluidAmountMb())
                        : FluidStack.EMPTY;
            }
            default -> FluidStack.EMPTY;
        };
    }

    @Override
    public int getTankCapacity(int tank) {
        return switch (tank) {
            case 0 -> blockEntity.getOilTank().getCapacityMb();
            case 1 -> blockEntity.getGasTank().getCapacityMb();
            case 2 -> blockEntity.getFracksolTank().getCapacityMb();
            default -> 0;
        };
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return switch (tank) {
            case 0 -> VanillaFluidEquivalence.sameSubstance(stack.getFluid(), ModFluids.CRUDE_OIL.getSource());
            case 1 -> VanillaFluidEquivalence.sameSubstance(stack.getFluid(), ModFluids.GAS.getSource());
            case 2 -> VanillaFluidEquivalence.sameSubstance(stack.getFluid(), ModFluids.FRACKSOL.getSource());
            default -> false;
        };
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        int amountMb = resource.getAmount();

        // Вход принимает только FrackSol.
        if (VanillaFluidEquivalence.sameSubstance(resource.getFluid(), ModFluids.FRACKSOL.getSource())) {
            int spaceMb = Math.max(0, blockEntity.getFracksolTank().getSpaceMb());
            int acceptedMb = Math.min(spaceMb, amountMb);
            if (acceptedMb > 0 && action == FluidAction.EXECUTE) {
                blockEntity.getFracksolTank().fillMb(ModFluids.FRACKSOL.getSource(), acceptedMb);
            }
            return acceptedMb;
        }

        // Остальное нельзя заливать (выходные танки)
        return 0;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        int amountMb = resource.getAmount();

        // Можно выкачивать нефть из танка 0
        if (VanillaFluidEquivalence.sameSubstance(resource.getFluid(), ModFluids.CRUDE_OIL.getSource())) {
            int available = blockEntity.getOilTank().getFluidAmountMb();
            int planned = Math.min(amountMb, available);
            if (planned <= 0) return FluidStack.EMPTY;
            int drainedMb = action == FluidAction.EXECUTE
                    ? blockEntity.getOilTank().drainMb(planned)
                    : planned;
            return drainedMb > 0 ? new FluidStack(ModFluids.CRUDE_OIL.getSource(), drainedMb) : FluidStack.EMPTY;
        }

        // Можно выкачивать газ из танка 1
        if (VanillaFluidEquivalence.sameSubstance(resource.getFluid(), ModFluids.GAS.getSource())) {
            int available = blockEntity.getGasTank().getFluidAmountMb();
            int planned = Math.min(amountMb, available);
            if (planned <= 0) return FluidStack.EMPTY;
            int drainedMb = action == FluidAction.EXECUTE
                    ? blockEntity.getGasTank().drainMb(planned)
                    : planned;
            return drainedMb > 0 ? new FluidStack(ModFluids.GAS.getSource(), drainedMb) : FluidStack.EMPTY;
        }

        // FrackSol нельзя выкачивать (входной танк для потребления)
        return FluidStack.EMPTY;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int amountMb = Math.max(0, maxDrain);

        // Сначала пробуем нефть
        int oilPlanned = Math.min(amountMb, blockEntity.getOilTank().getFluidAmountMb());
        int oilDrainedMb = action == FluidAction.EXECUTE
            ? blockEntity.getOilTank().drainMb(oilPlanned)
            : oilPlanned;
        if (oilDrainedMb > 0) {
            return new FluidStack(ModFluids.CRUDE_OIL.getSource(), oilDrainedMb);
        }

        // Потом газ
        int gasPlanned = Math.min(amountMb, blockEntity.getGasTank().getFluidAmountMb());
        int gasDrainedMb = action == FluidAction.EXECUTE
            ? blockEntity.getGasTank().drainMb(gasPlanned)
            : gasPlanned;
        return gasDrainedMb > 0
                ? new FluidStack(ModFluids.GAS.getSource(), gasDrainedMb)
                : FluidStack.EMPTY;
    }
}
//?}
