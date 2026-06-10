package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.armormod.util.ArmorModificationHelper;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
*///?}
/**
 * Порт FluidLoaderFillableItem из 1.7.10.
 *
 * Эквивалент старого IFillableItem (fluidmk2) реализован через Forge capability IFluidHandlerItem
 * на модификациях брони.
 *
 * Семантика (как в 1.7.10):
 * - emptyItem (контейнер/мод -> бак): переливает жидкость из "модов" в tank
 * - fillItem  (бак -> контейнер/мод): переливает из tank в "моды"
 *
 * В 1.7.10 заполнение блокировалось давлением tank.pressure != 0 — переносим это как запрет
 * и на emptyItem, и на fillItem.
 */
@SuppressWarnings("UnstableApiUsage")
public class FluidLoaderFillableItem implements FluidTank.LoadingHandler {

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        return fillOrEmptyArmor(slots[in], tank, true);
    }

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        return fillOrEmptyArmor(slots[in], tank, false);
    }

    /**
     * @param armorStack входной стак (в 1.7.10 это был stack, где лежали моды)
     * @param draining   true  => tank += drain(mod) (emptyItem)
     *                    false => tank -= fill(mod)  (fillItem)
     */
    private boolean fillOrEmptyArmor(ItemStack armorStack, FluidTank tank, boolean draining) {
        if (armorStack == null || armorStack.isEmpty()) return false;
        if (tank.getPressure() != 0) return false;

        ItemStack[] mods = ArmorModificationHelper.pryMods(armorStack);
        if (mods == null) return false;

        boolean any = false;

        for (ItemStack mod : mods) {
            if (mod == null || mod.isEmpty()) continue;

            Item modItem = mod.getItem();
            if (modItem == null) continue;

            boolean ok;
            if (draining) {
                // emptyItem: mod (как fluid handler item) -> tank — all-or-nothing
                //? if forge {
                ok = mod.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
                    FluidStack simulatedDrain = handler.drain(Integer.MAX_VALUE, IFluidHandlerItem.FluidAction.SIMULATE);
                    if (simulatedDrain.isEmpty()) return false;

                    int modFill = simulatedDrain.getAmount();

                    if (simulatedDrain.getFluid() != tank.getTankType()) return false;

                    int space = tank.getMaxFill() - tank.getFill();
                    if (space < modFill) return false; // all-or-nothing

                    FluidStack drainedReal = handler.drain(simulatedDrain, IFluidHandlerItem.FluidAction.EXECUTE);
                    if (drainedReal.isEmpty() || drainedReal.getAmount() != modFill) return false;

                    tank.fill(tank.getFill() + drainedReal.getAmount());
                    return true;
                }).orElse(false);
                //?}
                //? if fabric {
                /*ok = fabricDrainModToTankAllOrNothing(mod, tank);
                *///?}
            } else {
                // fillItem: tank -> mod — all-or-nothing
                //? if forge {
                ok = mod.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
                    if (tank.getFill() <= 0) return false;
                    if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return false;

                    int available = tank.getFill();

                    int capacity = handler.getTankCapacity(0);
                    if (capacity <= 0) return false;

                    if (available < capacity) return false; // all-or-nothing

                    FluidStack resource = new FluidStack(tank.getTankType(), capacity);

                    int filledSim = handler.fill(resource, IFluidHandlerItem.FluidAction.SIMULATE);
                    if (filledSim != capacity) return false;

                    int filledReal = handler.fill(resource, IFluidHandlerItem.FluidAction.EXECUTE);
                    if (filledReal != capacity) return false;

                    tank.fill(tank.getFill() - filledReal);
                    return true;
                }).orElse(false);
                //?}
                //? if fabric {
                /*ok = fabricFillTankToModAllOrNothing(mod, tank);
                *///?}
            }

            any |= ok;
            if (tank.getFill() == (draining ? tank.getMaxFill() : 0)) return true;
        }

        return any;
    }

    //? if fabric {
    /*@SuppressWarnings("UnstableApiUsage")
    private static boolean fabricDrainModToTankAllOrNothing(ItemStack mod, FluidTank tank) {
        Storage<FluidVariant> storage = FluidStorage.ITEM.find(mod, ContainerItemContext.withConstant(mod));
        if (storage == null) return false;
        FluidVariant want = FluidVariant.of(tank.getTankType());
        long simDrop;
        try (Transaction tx = Transaction.openOuter()) {
            simDrop = storage.extract(want, Long.MAX_VALUE, tx);
        }
        if (simDrop <= 0 || simDrop % 81L != 0) return false;
        int modMb = (int) (simDrop / 81L);
        int space = tank.getMaxFill() - tank.getFill();
        if (space < modMb) return false;
        try (Transaction tx = Transaction.openOuter()) {
            long ext = storage.extract(want, simDrop, tx);
            if (ext != simDrop) return false;
            tx.commit();
        }
        int filled = tank.fillMb(want.getFluid(), modMb);
        return filled == modMb;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static boolean fabricFillTankToModAllOrNothing(ItemStack mod, FluidTank tank) {
        if (tank.getFill() <= 0) return false;
        if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return false;
        Storage<FluidVariant> storage = FluidStorage.ITEM.find(mod, ContainerItemContext.withConstant(mod));
        if (storage == null) return false;
        FluidVariant fv = FluidVariant.of(tank.getTankType());
        long capDrop;
        try (Transaction tx = Transaction.openOuter()) {
            capDrop = storage.insert(fv, Long.MAX_VALUE, tx);
        }
        if (capDrop <= 0 || capDrop % 81L != 0) return false;
        int capMb = (int) (capDrop / 81L);
        int available = tank.getFill();
        if (available < capMb) return false;
        try (Transaction tx = Transaction.openOuter()) {
            long ins = storage.insert(fv, capDrop, tx);
            if (ins != capDrop) return false;
            tx.commit();
        }
        int drained = tank.drainMb(capMb);
        return drained == capMb;
    }
    *///?}
}
