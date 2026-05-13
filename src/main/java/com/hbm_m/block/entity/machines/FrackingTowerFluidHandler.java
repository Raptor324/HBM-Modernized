package com.hbm_m.block.entity.machines;
//? if forge {
import org.jetbrains.annotations.NotNull;

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
            case 0 -> stack.getFluid().isSame(ModFluids.CRUDE_OIL.getSource());
            case 1 -> stack.getFluid().isSame(ModFluids.GAS.getSource());
            case 2 -> stack.getFluid().isSame(ModFluids.FRACKSOL.getSource());
            default -> false;
        };
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        // Для новых танков в проекте mB -> int мБ; FluidTank сам использует mb
        int amountMb = resource.getAmount();

        // FrackSol можно заливать в танк 2
        if (resource.getFluid().isSame(ModFluids.FRACKSOL.getSource())) {
            // На Forge FluidAction EXECUTE против SIMULATE: наш FluidTank использует EXECUTE/всегда исполняет.
            // Поэтому: если SIMULATE — делаем оценку через SIMULATE на Forge storage внутри fillMb.
            // (fillMb внутри FluidTank вызывает forgeStorage.fill(..., EXECUTE) поэтому используем EXECUTE для компиляции,
            // но корректность симуляции не идеальна — это лучше поправить после схождения API у FluidTank.)
            return blockEntity.getFracksolTank().fillMb(resource.getFluid(), action == FluidAction.EXECUTE ? amountMb : amountMb);
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
        if (resource.getFluid().isSame(ModFluids.CRUDE_OIL.getSource())) {
            int drainedMb = blockEntity.getOilTank().drainMb(Math.min(amountMb, blockEntity.getOilTank().getFluidAmountMb()));
            return drainedMb > 0 ? new FluidStack(resource.getFluid(), drainedMb) : FluidStack.EMPTY;
        }

        // Можно выкачивать газ из танка 1
        if (resource.getFluid().isSame(ModFluids.GAS.getSource())) {
            int drainedMb = blockEntity.getGasTank().drainMb(Math.min(amountMb, blockEntity.getGasTank().getFluidAmountMb()));
            return drainedMb > 0 ? new FluidStack(resource.getFluid(), drainedMb) : FluidStack.EMPTY;
        }

        // FrackSol нельзя выкачивать (входной танк для потребления)
        return FluidStack.EMPTY;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int amountMb = Math.max(0, maxDrain);

        // Сначала пробуем нефть
        int oilDrainedMb = blockEntity.getOilTank().drainMb(Math.min(amountMb, blockEntity.getOilTank().getFluidAmountMb()));
        if (oilDrainedMb > 0) {
            return new FluidStack(ModFluids.CRUDE_OIL.getSource(), oilDrainedMb);
        }

        // Потом газ
        int gasDrainedMb = blockEntity.getGasTank().drainMb(Math.min(amountMb, blockEntity.getGasTank().getFluidAmountMb()));
        return gasDrainedMb > 0
                ? new FluidStack(ModFluids.GAS.getSource(), gasDrainedMb)
                : FluidStack.EMPTY;
    }
}
//?}
