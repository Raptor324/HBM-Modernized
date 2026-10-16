package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.platform.FluidHooks;
import net.minecraft.world.item.ItemStack;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import net.minecraft.world.level.material.Fluid;

public class FluidLoaderFillableItem implements FluidTank.LoadingHandler {

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        return fillOrEmptyArmor(slots[in], tank, true);
    }

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        return fillOrEmptyArmor(slots[in], tank, false);
    }

    private boolean fillOrEmptyArmor(ItemStack armorStack, FluidTank tank, boolean draining) {
        if (armorStack == null || armorStack.isEmpty()) return false;
        if (tank.getPressure() != 0) return false;

        ItemStack[] mods = ArmorModificationHelper.pryMods(armorStack);
        if (mods == null) return false;
        boolean any = false;

        for (int i = 0; i < mods.length; i++) {
            ItemStack mod = mods[i];
            if (mod == null || mod.isEmpty()) continue;
            boolean ok = false;

            if (draining) {
                FluidHooks.FluidExtraction sim = FluidHooks.extractFluidFromItem(mod, Integer.MAX_VALUE, true);
                if (sim.amount() > 0 && VanillaFluidEquivalence.sameSubstance(sim.fluid(), tank.getTankType())) {
                    int space = tank.getMaxFill() - tank.getFill();
                    if (space >= sim.amount()) {
                        FluidHooks.FluidExtraction exec = FluidHooks.extractFluidFromItem(mod, sim.amount(), false);
                        tank.fillMb(exec.fluid(), exec.amount(), false);
                        mods[i] = exec.remainder();
                        ok = true;
                    }
                }
            } else {
                if (tank.getFill() > 0 && FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) {
                    int cap = FluidHooks.getItemFluidCapacity(mod);
                    if (cap > 0 && tank.getFill() >= cap) {
                        Fluid fluidToInsert = VanillaFluidEquivalence.forVanillaContainerFill(tank.getStoredFluid());
                        FluidHooks.FluidInsertion sim = FluidHooks.insertFluidIntoItem(mod, fluidToInsert, cap, true);
                        if (sim.amountInserted() == cap) {
                            FluidHooks.FluidInsertion exec = FluidHooks.insertFluidIntoItem(mod, fluidToInsert, cap, false);
                            tank.drainMb(cap, false);
                            mods[i] = exec.remainder();
                            ok = true;
                        }
                    }
                }
            }

            any |= ok;
            if (tank.getFill() == (draining ? tank.getMaxFill() : 0)) break;
        }

        return any;
    }
}