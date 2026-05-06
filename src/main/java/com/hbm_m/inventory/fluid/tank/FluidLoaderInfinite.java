package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.item.liquids.InfiniteWaterItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/**
 * Загрузчик для бесконечных бочек (InfiniteFluidItem, InfiniteWaterItem).
 * Порт FluidLoaderInfinite из 1.7.10.
 *
 * При emptyItem: если тип бака задан и совпадает — заполняет бак полностью.
 * При fillItem: мгновенно опустошает бак (бесконечная бочка поглощает).
 */
public class FluidLoaderInfinite implements FluidTank.LoadingHandler {

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack stack = slots[in];
        if (stack == null || stack.isEmpty()) return false;

        if (stack.getItem() instanceof InfiniteFluidItem) {
            if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) return false;
            if (tank.getFill() >= tank.getMaxFill()) return false;
            tank.fill(tank.getMaxFill());
            return true;
        }

        if (stack.getItem() instanceof InfiniteWaterItem) {
            if (tank.getFill() >= tank.getMaxFill()) return false;
            if (!FluidTank.isFluidTypeExplicitlySet(tank.getTankType()) || !VanillaFluidEquivalence.sameSubstance(tank.getTankType(), Fluids.WATER)) {
                return false;
            }
            tank.fill(tank.getMaxFill());
            return true;
        }

        return false;
    }

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
        ItemStack stack = slots[in];
        if (stack == null || stack.isEmpty()) return false;

        if (stack.getItem() instanceof InfiniteFluidItem) {
            if (tank.getFill() > 0) {
                tank.fill(0);
                return true;
            }
        }
        return false;
    }
}
