package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.api.fluids.ModFluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Обработчик жидкостей для Fracking Tower.
 * Управляет тремя танками: нефть (выход), газ (выход), FrackSol (вход).
 */
public class FrackingTowerFluidHandler implements IFluidHandler {

    private final MachineHydraulicFrackiningTowerBlockEntity blockEntity;

    public FrackingTowerFluidHandler(MachineHydraulicFrackiningTowerBlockEntity blockEntity) {
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
            case 0 -> blockEntity.getOilTank().getFluidInTank(0);
            case 1 -> blockEntity.getGasTank().getFluidInTank(0);
            case 2 -> blockEntity.getFracksolTank().getFluidInTank(0);
            default -> FluidStack.EMPTY;
        };
    }

    @Override
    public int getTankCapacity(int tank) {
        return switch (tank) {
            case 0 -> blockEntity.getOilTank().getTankCapacity(0);
            case 1 -> blockEntity.getGasTank().getTankCapacity(0);
            case 2 -> blockEntity.getFracksolTank().getTankCapacity(0);
            default -> 0;
        };
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return switch (tank) {
            case 0 -> stack.getFluid().isSame(ModFluids.CRUDE_OIL.getSource());
            case 1 -> stack.getFluid().isSame(ModFluids.GAS.getSource());
            case 2 -> stack.getFluid().isSame(ModFluids.FRACKSOL.getSource());
            default -> false;
        };
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;

        // FrackSol можно заливать в танк 2
        if (resource.getFluid().isSame(ModFluids.FRACKSOL.getSource())) {
            return blockEntity.getFracksolTank().fill(resource, action);
        }

        // Остальное нельзя заливать (выходные танки)
        return 0;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;

        // Можно выкачивать нефть из танка 0
        if (resource.getFluid().isSame(ModFluids.CRUDE_OIL.getSource())) {
            return blockEntity.getOilTank().drain(resource, action);
        }

        // Можно выкачивать газ из танка 1
        if (resource.getFluid().isSame(ModFluids.GAS.getSource())) {
            return blockEntity.getGasTank().drain(resource, action);
        }

        // FrackSol нельзя выкачивать (входной танк для потребления)
        return FluidStack.EMPTY;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        // Сначала пробуем нефть
        FluidStack oilDrained = blockEntity.getOilTank().drain(maxDrain, action);
        if (!oilDrained.isEmpty()) return oilDrained;

        // Потом газ
        return blockEntity.getGasTank().drain(maxDrain, action);
    }
}
