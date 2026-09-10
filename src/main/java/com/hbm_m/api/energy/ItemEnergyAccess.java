package com.hbm_m.api.energy;

import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

//? if forge {
/*import com.hbm_m.capability.ModCapabilities;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
*///?}
//? if neoforge {
import com.hbm_m.capability.ModCapabilities;
import net.neoforged.neoforge.capabilities.Capabilities;
//?}

public final class ItemEnergyAccess {

    private ItemEnergyAccess() {}

    public static boolean isEnergySource(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return getHbmProvider(stack).isPresent() || hasLoaderEnergy(stack);
    }

    /** Слот питания принимает и отдающие, и заряжаемые батарейки, и чужие FE-предметы. */
    public static boolean isEnergyItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return getHbmProvider(stack).isPresent()
                || getHbmReceiver(stack).isPresent()
                || hasLoaderEnergy(stack);
    }

    /** Энергокапабилити самого лоадера (Forge FE / NeoForge FE). */
    public static boolean hasLoaderEnergy(ItemStack stack) {
        if (stack.isEmpty()) return false;
        //? if forge {
        /*return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        *///?} elif neoforge {
        return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
        //?}
    }

    public static Optional<IEnergyProvider> getHbmProvider(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        //? if forge {
        /*return stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
        *///?} else {
        IEnergyProvider p = stack.getCapability(ModCapabilities.HBM_ITEM_ENERGY_PROVIDER);
        return Optional.ofNullable(p);
        //?}
    }

    public static Optional<IEnergyReceiver> getHbmReceiver(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        //? if forge {
        /*return stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();
        *///?} else {
        IEnergyReceiver r = stack.getCapability(ModCapabilities.HBM_ITEM_ENERGY_RECEIVER);
        return Optional.ofNullable(r);
        //?}
    }

    //? if forge {
    /*public static boolean canForgeExtract(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).map(net.minecraftforge.energy.IEnergyStorage::canExtract).orElse(false);
    }

    public static java.util.Optional<net.minecraftforge.energy.IEnergyStorage> getForgeEnergy(ItemStack stack) {
        if (stack.isEmpty()) return java.util.Optional.empty();
        return stack.getCapability(ForgeCapabilities.ENERGY).resolve();
    }
    *///?}
    //? if neoforge {
    public static boolean canForgeExtract(ItemStack stack) {
        net.neoforged.neoforge.energy.IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.canExtract();
    }

    public static java.util.Optional<net.neoforged.neoforge.energy.IEnergyStorage> getForgeEnergy(ItemStack stack) {
        if (stack.isEmpty()) return java.util.Optional.empty();
        return java.util.Optional.ofNullable(stack.getCapability(Capabilities.EnergyStorage.ITEM));
    }
    //?}
}