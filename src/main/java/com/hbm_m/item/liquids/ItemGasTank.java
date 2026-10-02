package com.hbm_m.item.liquids;

import java.util.function.Supplier;

import com.hbm_m.inventory.fluid.FluidContainerDefs;
import com.hbm_m.inventory.fluid.FluidContainerDefs.CDGastank;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.items.machine.ItemGasTank} ({@code gas_full}): nur Fluessigkeiten mit {@code CD_Gastank}, drei
 * Schichten (Grundbild, Flasche {@code gas_bottle} in Flaschenfarbe, Etikett {@code gas_label} in Etikettfarbe).
 */
public class ItemGasTank extends ItemFluidTank {

    public ItemGasTank(Properties properties, Supplier<Item> empty) {
        super(properties, empty);
    }

    @Override
    protected boolean accepts(Fluid f) {
        return FluidContainerDefs.getGastank(f) != null;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).append(" ").append(fluidName(getFluid(stack)));
    }

    @Override
    public int getColor(ItemStack stack, int pass) {
        if (pass == 0) return 0xFFFFFF;
        CDGastank tank = FluidContainerDefs.getGastank(getFluid(stack));
        if (tank == null) return 0xFFFFFF;
        return pass == 1 ? tank.bottleColor() : tank.labelColor();
    }
}
