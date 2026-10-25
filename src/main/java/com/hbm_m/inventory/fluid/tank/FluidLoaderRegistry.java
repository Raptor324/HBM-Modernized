package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.platform.StackNbt;

import com.hbm_m.inventory.FluidContainerRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.inventory.fluid.tank.FluidLoaderStandard}: Befuellen/Leeren ueber die
 * {@link FluidContainerRegistry} (Kanister, Gasflaschen, Tanks, Faesser, Zellen, Eimer ...). Umbenennungen bleiben
 * erhalten. Laeuft vor dem Capability-Lader des Ports; {@code fillItem} meldet hier Erfolg, damit derselbe Behaelter
 * nicht im selben Tick noch einmal ueber die Capability befuellt wird.
 */
public class FluidLoaderRegistry implements FluidTank.LoadingHandler {

    @Override
    public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {

        if (tank.getPressure() != 0) return false;
        if (slots[in] == null || slots[in].isEmpty()) return false;

        Fluid type = tank.getTankType();
        ItemStack full = FluidContainerRegistry.getFullContainer(slots[in], type);

        if (full != null && tank.getFill() - FluidContainerRegistry.getFluidContent(full, type) >= 0) {

            Component name = StackNbt.hasCustomName(slots[in]) ? slots[in].getHoverName() : null;
            ItemStack outStack = slots[out];

            if (outStack == null || outStack.isEmpty()) {

                tank.setFill(tank.getFill() - FluidContainerRegistry.getFluidContent(full, type));
                slots[out] = full.copy();
                slots[in].shrink(1);
                if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;

                if (name != null) StackNbt.setCustomName(slots[out], name);
                return true;

            } else if (FluidContainerRegistry.isItemEqual(outStack, full) && outStack.getCount() < outStack.getMaxStackSize()) {

                tank.setFill(tank.getFill() - FluidContainerRegistry.getFluidContent(full, type));
                slots[in].shrink(1);
                if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
                slots[out].grow(1);

                if (name != null) StackNbt.setCustomName(slots[out], name);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {

        if (slots[in] == null || slots[in].isEmpty())
            return false;

        Fluid type = tank.getTankType();
        int amount = FluidContainerRegistry.getFluidContent(slots[in], type);

        if (amount > 0 && tank.getFill() + amount <= tank.getMaxFill()) {

            ItemStack emptyContainer = FluidContainerRegistry.getEmptyContainer(slots[in]);
            Component name = StackNbt.hasCustomName(slots[in]) ? slots[in].getHoverName() : null;
            ItemStack outStack = slots[out];

            if (outStack == null || outStack.isEmpty()) {

                tank.setFill(tank.getFill() + amount);
                slots[out] = emptyContainer == null ? ItemStack.EMPTY : emptyContainer;

                if (emptyContainer != null && name != null) StackNbt.setCustomName(slots[out], name);

                slots[in].shrink(1);
                if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;

            } else if (emptyContainer == null || (FluidContainerRegistry.isItemEqual(outStack, emptyContainer) && outStack.getCount() < outStack.getMaxStackSize())) {

                tank.setFill(tank.getFill() + amount);
                slots[in].shrink(1);
                if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;

                if (emptyContainer != null) {
                    slots[out].grow(1);
                    if (name != null) StackNbt.setCustomName(slots[out], name);
                }
            }

            return true;
        }

        return false;
    }
}
