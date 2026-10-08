package com.hbm_m.block.machines.custom;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.config.CustomMachineConfigJSON;
import com.hbm_m.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm_m.item.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code ItemCustomMachine}: die Steuerung als Gegenstand; der Maschinentyp steht als {@code machineType} im NBT
 * (Original: Meta {@code 100 + Index}). Name aus der Konfiguration in der Sprache des Spielers, sonst
 * {@code "INVALID MACHINE CONTROLLER"}.
 */
public class ItemCustomMachine extends BlockItem {

    public ItemCustomMachine(Block block, Properties props) {
        super(block, props);
    }

    public static ItemStack make(String machineType) {
        ItemStack stack = new ItemStack(ModItems.CUSTOM_MACHINE.get());
        stack.getOrCreateTag().putString("machineType", machineType);
        return stack;
    }

    @Nullable
    public static String machineType(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("machineType") ? stack.getTag().getString("machineType") : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        String type = machineType(stack);
        MachineConfiguration conf = type != null ? CustomMachineConfigJSON.customMachines.get(type) : null;
        if (conf != null) return Component.literal(conf.displayName(CustomMachineConfigJSON.languageCode()));
        return Component.literal("INVALID MACHINE CONTROLLER");
    }
}
