package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.inventory.fluid.tank.FluidLoaderInfinite}: das unendliche Fass fuellt bzw. leert den Tank direkt
 * um {@code getAmount()} in dessen eingestelltem Typ. Frueher lief das ueber die Item-Capability, deren
 * {@code drain} ohne NBT-Typ (universelles Fass) nichts lieferte - das Fass fuellte keinen Tank.
 */
public class FluidLoaderInfinite implements FluidTank.LoadingHandler {

    /** Original {@code item.getType() != null && tank.type != item.getType()}. */
    private static boolean typeMismatch(InfiniteFluidItem inf, FluidTank tank) {
        Fluid fixed = inf.getFixedFluid();
        return fixed != null && !VanillaFluidEquivalence.sameSubstance(tank.getTankType(), fixed);
    }

    private static boolean roll(InfiniteFluidItem inf) {
        return inf.getChance() <= 1 || java.util.concurrent.ThreadLocalRandom.current().nextInt(inf.getChance()) == 0;
    }

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack stack = slots[in];
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof InfiniteFluidItem inf)) return false;
        if (!inf.canOperate(stack)) return false; // NTM Space: Wassertabelle (ohne Space-Addon immer true)
        // Original allowPressure: nur das universelle Fass arbeitet mit Drucktanks
        if (!inf.isInstantNetwork() && tank.getPressure() != 0) return false;
        if (typeMismatch(inf, tank)) return false;

        if (roll(inf)) tank.setFill(Math.max(tank.getFill() - inf.getTransferRate(), 0));
        return true;
    }

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack stack = slots[in];
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof InfiniteFluidItem inf)) return false;
        if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return false;
        if (!inf.canOperate(stack)) return false; // NTM Space: Wassertabelle (ohne Space-Addon immer true)
        if (typeMismatch(inf, tank)) return false;

        if (roll(inf)) tank.setFill((int) Math.min((long) tank.getFill() + inf.getTransferRate(), tank.getMaxFill()));
        return true;
    }
}
