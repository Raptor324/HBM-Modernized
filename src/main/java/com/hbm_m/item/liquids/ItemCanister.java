package com.hbm_m.item.liquids;

import java.util.function.Supplier;

import com.hbm_m.inventory.fluid.FluidContainerDefs;
import com.hbm_m.inventory.fluid.FluidContainerDefs.CDCanister;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.items.machine.ItemCanister} ({@code canister_full}): nur Fluessigkeiten mit {@code CD_Canister},
 * Name "Kanister" + Fluessigkeit, Overlay in der Kanisterfarbe.
 */
public class ItemCanister extends ItemFluidTank {

    public ItemCanister(Properties properties, Supplier<Item> empty) {
        super(properties, empty);
    }

    @Override
    protected boolean accepts(Fluid f) {
        return FluidContainerDefs.getCanister(f) != null;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).append(" ").append(fluidName(getFluid(stack)));
    }

    @Override
    public int getColor(ItemStack stack, int pass) {
        if (pass == 0) return 0xFFFFFF;
        CDCanister canister = FluidContainerDefs.getCanister(getFluid(stack));
        int j = canister == null ? -1 : canister.color();
        if (j < 0) j = 0xFFFFFF;
        return j;
    }
}
