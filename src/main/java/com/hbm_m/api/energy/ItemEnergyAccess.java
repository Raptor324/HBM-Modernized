package com.hbm_m.api.energy;

import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

//? if forge {
/*import com.hbm_m.capability.ModCapabilities;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
*///?}

/**
 * Loader-safe доступ к энергетическим интерфейсам у ItemStack.
 *
 * На Forge/NeoForge используется capability-система.
 * На Fabric батарейки читаются напрямую из NBT через {@link EnergyCapabilityProvider.ItemEnergyStorage}.
 */
public final class ItemEnergyAccess {

    private ItemEnergyAccess() {}

    public static Optional<IEnergyProvider> getHbmProvider(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        //? if forge {
        /*return stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
        *///?} else if fabric {
        if (stack.getItem() instanceof com.hbm_m.item.fekal_electric.ModBatteryItem battery) {
            var storage = new EnergyCapabilityProvider.ItemEnergyStorage(
                    stack, battery.getCapacity(), battery.getMaxReceive(), battery.getMaxExtract()
            );
            return storage.canExtract() ? Optional.of(storage) : Optional.empty();
        }

        return Optional.empty();
        //?} else {
        /*return Optional.empty();
        *///?}
    }

    public static Optional<IEnergyReceiver> getHbmReceiver(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        //? if forge {
        /*return stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();
        *///?} else if fabric {
        if (stack.getItem() instanceof com.hbm_m.item.fekal_electric.ModBatteryItem battery) {
            var storage = new EnergyCapabilityProvider.ItemEnergyStorage(
                    stack, battery.getCapacity(), battery.getMaxReceive(), battery.getMaxExtract()
            );
            return storage.canReceive() ? Optional.of(storage) : Optional.empty();
        }

        return Optional.empty();
        //?} else {
        /*return Optional.empty();
        *///?}
    }

    //? if forge {
    /*public static boolean canForgeExtract(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).map(net.minecraftforge.energy.IEnergyStorage::canExtract).orElse(false);
    }
    *///?}
}

