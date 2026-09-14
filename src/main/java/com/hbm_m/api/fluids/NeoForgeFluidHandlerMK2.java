package com.hbm_m.api.fluids;

//? if neoforge {
/*import com.hbm_m.inventory.fluid.tank.FluidTank;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/// Универсальный outward NeoForge `IFluidHandler` для BlockEntity, реализующих
/// `IFluidStandardTransceiverMK2` (или один из его родителей: `IFluidStandardSenderMK2` /
/// `IFluidStandardReceiverMK2`). Один класс покрывает все HBM-машины с флюидами.
///
/// Семантика:
/// - `fill` идёт в `getReceivingTanks()` (приём от внешней трубы/моды).
/// - `drain` идёт в `getSendingTanks()` (выдача наружу).
/// - `getFluidInTank`/`getTankCapacity`/`isFluidValid` отражают `getAllTanks()` (для GUI/tooltip внешних модов).
///
/// Сопоставление HBM `Fluid` ↔ vanilla `Fluid` — через `VanillaFluidEquivalence`.
public final class NeoForgeFluidHandlerMK2 implements IFluidHandler {

    private final IFluidUserMK2 entity;

    public NeoForgeFluidHandlerMK2(IFluidUserMK2 entity) {
        this.entity = entity;
    }

    @Override
    public int getTanks() {
        return entity.getAllTanks().length;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        FluidTank[] all = entity.getAllTanks();
        if (tank < 0 || tank >= all.length) return FluidStack.EMPTY;
        FluidTank t = all[tank];
        if (t.getFill() <= 0) return FluidStack.EMPTY;
        return new FluidStack(VanillaFluidEquivalence.forVanillaContainerFill(t.getTankType()), t.getFill());
    }

    @Override
    public int getTankCapacity(int tank) {
        FluidTank[] all = entity.getAllTanks();
        if (tank < 0 || tank >= all.length) return 0;
        return all[tank].getMaxFill();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        if (stack.isEmpty()) return false;
        // Валидность определяется принимающими баками (fill-семантика).
        FluidTank[] receiving = entity instanceof IFluidStandardReceiverMK2 r
                ? r.getReceivingTanks()
                : FluidTank.EMPTY_ARRAY;
        if (tank < 0 || tank >= receiving.length) return false;
        return receiving[tank].isFluidValid(stack.getFluid());
    }

    @Override
    public int fill(@NotNull FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !(entity instanceof IFluidStandardReceiverMK2 receiver)) return 0;
        Fluid incoming = resource.getFluid();
        int remaining = resource.getAmount();
        int filled = 0;
        for (FluidTank tank : receiver.getReceivingTanks()) {
            if (remaining <= 0) break;
            if (tank.getFill() >= tank.getMaxFill()) continue;
            if (!tank.isFluidValid(incoming)) continue;
            int room = tank.getMaxFill() - tank.getFill();
            int planned = Math.min(remaining, room);
            if (planned <= 0) continue;
            if (action.execute()) {
                filled += tank.fillMb(incoming, planned);
            } else {
                filled += planned;
            }
            remaining -= planned;
        }
        return filled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !(entity instanceof IFluidStandardSenderMK2 sender)) return FluidStack.EMPTY;
        Fluid wanted = resource.getFluid();
        int toDrain = resource.getAmount();
        int drained = 0;
        for (FluidTank tank : sender.getSendingTanks()) {
            if (toDrain <= 0) break;
            if (tank.getFill() <= 0) continue;
            if (!VanillaFluidEquivalence.sameSubstance(tank.getTankType(), wanted)) continue;
            int planned = Math.min(toDrain, tank.getFill());
            if (planned <= 0) continue;
            int actual = action.execute() ? tank.drainMb(planned) : planned;
            drained += actual;
            toDrain -= actual;
        }
        return drained > 0
                ? new FluidStack(VanillaFluidEquivalence.forVanillaContainerFill(wanted), drained)
                : FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || !(entity instanceof IFluidStandardSenderMK2 sender)) return FluidStack.EMPTY;
        int remaining = maxDrain;
        Fluid drainedFluid = null;
        int drained = 0;
        for (FluidTank tank : sender.getSendingTanks()) {
            if (remaining <= 0) break;
            if (tank.getFill() <= 0) continue;
            // Не смешиваем разные флюиды в одном ответе — берём только первый совместимый бак.
            Fluid tankFluid = tank.getTankType();
            if (drainedFluid == null) {
                drainedFluid = tankFluid;
            } else if (!VanillaFluidEquivalence.sameSubstance(drainedFluid, tankFluid)) {
                continue;
            }
            int planned = Math.min(remaining, tank.getFill());
            int actual = action.execute() ? tank.drainMb(planned) : planned;
            drained += actual;
            remaining -= actual;
        }
        return drained > 0 && drainedFluid != null
                ? new FluidStack(VanillaFluidEquivalence.forVanillaContainerFill(drainedFluid), drained)
                : FluidStack.EMPTY;
    }
}
*///?}
