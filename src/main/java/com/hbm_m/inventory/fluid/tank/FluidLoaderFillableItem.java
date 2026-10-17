package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.platform.FluidHooks;
import net.minecraft.world.item.ItemStack;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.inventory.fluid.tank.FluidLoaderFillableItem}: fuellt/leert {@link IFillableItem}-Gegenstaende
 * (Jetpacks, PLSS, Loetlampe, Chemthrower ...) und die {@link IFillableItem}-Mods einer Ruestung.
 * Frueher pruefte der Port hier nur die Plattform-Capability der Ruestungsmods - IFillableItem wurde nie bedient.
 */
public class FluidLoaderFillableItem implements FluidTank.LoadingHandler {

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        return fill(slots[in], tank);
    }

    public boolean fill(ItemStack stack, FluidTank tank) {

        if (tank.getPressure() != 0) return false;

        if (stack == null || stack.isEmpty())
            return false;

        Fluid type = tank.getTankType();

        // Original: ItemArmor && hasMods - im Port tragen auch Nicht-ArmorItem-Ruestungen Mods, daher nur hasMods
        if (ArmorModificationHelper.hasMods(stack)) {
            for (ItemStack mod : ArmorModificationHelper.pryMods(stack)) {

                if (mod != null && mod.getItem() instanceof IFillableItem) {
                    fill(mod, tank);
                    ArmorModificationHelper.applyMod(stack, mod);
                } else if (mod != null && !mod.isEmpty()) {
                    // Port: Capability-Mods (kein IFillableItem) wie bisher bedienen
                    ItemStack res = fillCapabilityMod(mod, tank);
                    if (res != null) ArmorModificationHelper.applyMod(stack, res);
                }
            }
        }

        if (!(stack.getItem() instanceof IFillableItem fillable)) return false;

        if (fillable.acceptsFluid(type, stack)) {
            tank.setFill(fillable.tryFill(type, tank.getFill(), stack));
        }

        return true;
    }

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        return empty(slots[in], tank);
    }

    public boolean empty(ItemStack stack, FluidTank tank) {

        if (stack == null || stack.isEmpty())
            return false;

        Fluid type = tank.getTankType();

        if (ArmorModificationHelper.hasMods(stack)) {
            for (ItemStack mod : ArmorModificationHelper.pryMods(stack)) {

                if (mod != null && mod.getItem() instanceof IFillableItem) {
                    empty(mod, tank);
                    // Abweichung (technisch noetig): Original schreibt den geleerten Mod nicht zurueck, die Fuellung
                    // bliebe im Mod und die Fluessigkeit waere verdoppelt.
                    ArmorModificationHelper.applyMod(stack, mod);
                } else if (mod != null && !mod.isEmpty()) {
                    ItemStack res = emptyCapabilityMod(mod, tank);
                    if (res != null) ArmorModificationHelper.applyMod(stack, res);
                }
            }
        }

        if (!(stack.getItem() instanceof IFillableItem fillable)) return false;

        if (fillable.providesFluid(type, stack)) {
            tank.setFill(tank.getFill() + fillable.tryEmpty(type, tank.getMaxFill() - tank.getFill(), stack));
        }

        return tank.getFill() == tank.getMaxFill();
    }

    /** Bisheriger Port-Weg fuer Capability-Mods; liefert den geaenderten Mod oder null. */
    private static ItemStack fillCapabilityMod(ItemStack mod, FluidTank tank) {
        if (tank.getFill() <= 0 || !FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return null;
        int cap = FluidHooks.getItemFluidCapacity(mod);
        if (cap <= 0 || tank.getFill() < cap) return null;
        Fluid fluidToInsert = VanillaFluidEquivalence.forVanillaContainerFill(tank.getStoredFluid());
        FluidHooks.FluidInsertion sim = FluidHooks.insertFluidIntoItem(mod, fluidToInsert, cap, true);
        if (sim.amountInserted() != cap) return null;
        FluidHooks.FluidInsertion exec = FluidHooks.insertFluidIntoItem(mod, fluidToInsert, cap, false);
        tank.drainMb(cap, false);
        return exec.remainder();
    }

    private static ItemStack emptyCapabilityMod(ItemStack mod, FluidTank tank) {
        FluidHooks.FluidExtraction sim = FluidHooks.extractFluidFromItem(mod, Integer.MAX_VALUE, true);
        if (sim.amount() <= 0 || !VanillaFluidEquivalence.sameSubstance(sim.fluid(), tank.getTankType())) return null;
        if (tank.getMaxFill() - tank.getFill() < sim.amount()) return null;
        FluidHooks.FluidExtraction exec = FluidHooks.extractFluidFromItem(mod, sim.amount(), false);
        tank.fillMb(exec.fluid(), exec.amount(), false);
        return exec.remainder();
    }
}
