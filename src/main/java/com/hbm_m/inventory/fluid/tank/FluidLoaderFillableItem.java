package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.armormod.util.ArmorModificationHelper;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if forge {
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
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
            } else {
                // fillItem: tank -> mod — all-or-nothing
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
            }

            any |= ok;
            if (tank.getFill() == (draining ? tank.getMaxFill() : 0)) return true;
        }

        return any;
    }
}
