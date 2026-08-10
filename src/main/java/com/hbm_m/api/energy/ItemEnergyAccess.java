package com.hbm_m.api.energy;

import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

//? if forge {
import com.hbm_m.capability.ModCapabilities;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}
//? if neoforge {
/*import com.hbm_m.capability.ModCapabilities;
import net.neoforged.neoforge.capabilities.Capabilities;
*///?}

public final class ItemEnergyAccess {

    private ItemEnergyAccess() {}

    public static boolean isEnergySource(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (getHbmProvider(stack).isPresent()) return true;

        //? if forge {
        if (stack.getCapability(ForgeCapabilities.ENERGY).isPresent()) return true;
        //?}
        //? if neoforge {
        /*if (stack.getCapability(Capabilities.EnergyStorage.ITEM) != null) return true;
        *///?}

        return false;
    }

    public static Optional<IEnergyProvider> getHbmProvider(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        //? if forge {
        return stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
        //?} else if neoforge {
        /*IEnergyProvider p = stack.getCapability(ModCapabilities.HBM_ITEM_ENERGY_PROVIDER);
        return Optional.ofNullable(p);
        *///?} else if fabric {
        /*if (stack.getItem() instanceof com.hbm_m.item.fekal_electric.ModBatteryItem battery) {
            var storage = new EnergyCapabilityProvider.ItemEnergyStorage(
                    stack, battery.getCapacity(), battery.getMaxReceive(), battery.getMaxExtract()
            );
            return storage.canExtract() ? Optional.of(storage) : Optional.empty();
        }
        return Optional.empty();
        *///?} else {
        /*return Optional.empty();
        *///?}
    }

    public static Optional<IEnergyReceiver> getHbmReceiver(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        //? if forge {
        return stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();
        //?} else if neoforge {
        /*IEnergyReceiver r = stack.getCapability(ModCapabilities.HBM_ITEM_ENERGY_RECEIVER);
        return Optional.ofNullable(r);
        *///?} else if fabric {
        /*if (stack.getItem() instanceof com.hbm_m.item.fekal_electric.ModBatteryItem battery) {
            var storage = new EnergyCapabilityProvider.ItemEnergyStorage(
                    stack, battery.getCapacity(), battery.getMaxReceive(), battery.getMaxExtract()
            );
            return storage.canReceive() ? Optional.of(storage) : Optional.empty();
        }
        return Optional.empty();
        *///?} else {
        /*return Optional.empty();
        *///?}
    }

    //? if forge {
    public static boolean canForgeExtract(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).map(net.minecraftforge.energy.IEnergyStorage::canExtract).orElse(false);
    }
    //?}
    //? if neoforge {
    /*public static boolean canForgeExtract(ItemStack stack) {
        net.neoforged.neoforge.energy.IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.canExtract();
    }
    *///?}

    /**
     * Кросс-платформенный доступ к FE-item-capability предмета (Forge Energy).
     * На forge возвращает {@code net.minecraftforge.energy.IEnergyStorage},
     * на neoforge — {@code net.neoforged.neoforge.energy.IEnergyStorage}.
     * У обоих интерфейсов идентичная сигнатура методов, call-сайт внутри
     * {@code .ifPresent(...)} работает одинаково.
     */
    //? if forge {
    public static java.util.Optional<net.minecraftforge.energy.IEnergyStorage> getForgeEnergy(ItemStack stack) {
        if (stack.isEmpty()) return java.util.Optional.empty();
        return stack.getCapability(ForgeCapabilities.ENERGY).resolve();
    }
    //?}
    //? if neoforge {
    /*public static java.util.Optional<net.neoforged.neoforge.energy.IEnergyStorage> getForgeEnergy(ItemStack stack) {
        if (stack.isEmpty()) return java.util.Optional.empty();
        return java.util.Optional.ofNullable(stack.getCapability(Capabilities.EnergyStorage.ITEM));
    }
    *///?}
}