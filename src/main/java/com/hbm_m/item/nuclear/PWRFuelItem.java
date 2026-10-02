package com.hbm_m.item.nuclear;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Fresh PWR fuel rod. Ported from {@code com.hbm.items.machine.ItemPWRFuel} (1.7.10), as a plain
 * item-per-type (see {@link PWRFuelType}) instead of an NBT-enum-meta item, matching this port's
 * {@code WatzPelletItem} convention. Depletion tracking lives entirely on
 * {@code PWRControllerBlockEntity} (typeLoaded/amountLoaded/progress), matching the original -
 * unlike Watz pellets, this item itself carries no per-stack yield NBT.
 */
public class PWRFuelItem extends Item implements com.hbm_m.item.ITooltipProvider {

    private final PWRFuelType type;

    public PWRFuelItem(Properties properties, PWRFuelType type) {
        super(properties);
        this.type = type;
    }

    public PWRFuelType getType() {
        return type;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String color = ChatFormatting.GOLD + "";
        String reset = ChatFormatting.RESET + "";
        tooltip.add(Component.literal(color + "Heat per flux: " + reset + type.heatEmission + " TU"));
        tooltip.add(Component.literal(color + "Reaction function: " + reset + type.function.getLabelForFuel()));
        tooltip.add(Component.literal(color + "Fuel type: " + reset + type.function.getDangerFromFuel()));
    }
}
