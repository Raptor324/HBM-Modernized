package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.platform.FluidHooks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FluidLoaderInfinite implements FluidTank.LoadingHandler {

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack stack = slots[in];
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof InfiniteFluidItem inf)) return false;
        if (tank.getFill() <= 0) return false;

        Fluid requestedType = tank.getTankType();
        Fluid itemType = inf.getFluidType(stack);
        if (itemType != null && itemType != Fluids.EMPTY && !VanillaFluidEquivalence.sameSubstance(requestedType, itemType)) return false;

        FluidHooks.FluidExtraction ex = FluidHooks.extractFluidFromItem(stack, Integer.MAX_VALUE, false);
        if (ex.amount() <= 0) return false;

        tank.setFill(Math.max(tank.getFill() - ex.amount(), 0));
        return true;
    }

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack stack = slots[in];
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof InfiniteFluidItem inf)) return false;

        Fluid currentType = tank.getTankType();
        if (currentType == null || currentType == Fluids.EMPTY) return false;

        Fluid itemType = inf.getFluidType(stack);
        if (itemType != null && itemType != Fluids.EMPTY && !VanillaFluidEquivalence.sameSubstance(currentType, itemType)) return false;

        FluidHooks.FluidExtraction ex = FluidHooks.extractFluidFromItem(stack, Integer.MAX_VALUE, false);
        if (ex.amount() <= 0) return false;

        int space = tank.getMaxFill() - tank.getFill();
        int toFill = Math.min(ex.amount(), space);
        if (toFill <= 0) return false;

        int filled = tank.fillMb(currentType, toFill, false);
        slots[in] = ex.remainder();
        return filled > 0;
    }
}