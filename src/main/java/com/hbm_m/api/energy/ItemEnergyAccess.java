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
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        //?} elif neoforge {
        /*return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
        *///?}
    }

    public static Optional<IEnergyProvider> getHbmProvider(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        //? if forge {
        return stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
        //?} else {
        /*IEnergyProvider p = stack.getCapability(ModCapabilities.HBM_ITEM_ENERGY_PROVIDER);
        return Optional.ofNullable(p);
        *///?}
    }

    public static Optional<IEnergyReceiver> getHbmReceiver(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        //? if forge {
        return stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();
        //?} else {
        /*IEnergyReceiver r = stack.getCapability(ModCapabilities.HBM_ITEM_ENERGY_RECEIVER);
        return Optional.ofNullable(r);
        *///?}
    }

    //? if forge {
    public static boolean canForgeExtract(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).map(net.minecraftforge.energy.IEnergyStorage::canExtract).orElse(false);
    }

    public static boolean canForgeReceive(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).map(net.minecraftforge.energy.IEnergyStorage::canReceive).orElse(false);
    }

    public static java.util.Optional<net.minecraftforge.energy.IEnergyStorage> getForgeEnergy(ItemStack stack) {
        if (stack.isEmpty()) return java.util.Optional.empty();
        return stack.getCapability(ForgeCapabilities.ENERGY).resolve();
    }
    //?}
    //? if neoforge {
    /*public static boolean canForgeExtract(ItemStack stack) {
        net.neoforged.neoforge.energy.IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.canExtract();
    }

    public static boolean canForgeReceive(ItemStack stack) {
        net.neoforged.neoforge.energy.IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.canReceive();
    }

    public static java.util.Optional<net.neoforged.neoforge.energy.IEnergyStorage> getForgeEnergy(ItemStack stack) {
        if (stack.isEmpty()) return java.util.Optional.empty();
        return java.util.Optional.ofNullable(stack.getCapability(Capabilities.EnergyStorage.ITEM));
    }
    *///?}

    // ═══════ FE-конверсия чужих энергопредметов на HE (курс в EnergyConversion) ═══════

    /** Запас чужого FE-предмета, пересчитанный в HE. */
    public static long feStoredAsHe(ItemStack stack) {
        //? if forge {
        return getForgeEnergy(stack)
                .map(fe -> EnergyConversion.heFromFe(fe.getEnergyStored()))
                .orElse(0L);
        //?}
        //? if neoforge {
        /*return getForgeEnergy(stack)
                .map(fe -> EnergyConversion.heFromFe(fe.getEnergyStored()))
                .orElse(0L);
        *///?}
    }

    /** Ёмкость чужого FE-предмета, пересчитанная в HE (минимум 1 — против деления на ноль). */
    public static long feMaxAsHe(ItemStack stack) {
        //? if forge {
        return getForgeEnergy(stack)
                .map(fe -> Math.max(1L, EnergyConversion.heFromFe(fe.getMaxEnergyStored())))
                .orElse(1L);
        //?}
        //? if neoforge {
        /*return getForgeEnergy(stack)
                .map(fe -> Math.max(1L, EnergyConversion.heFromFe(fe.getMaxEnergyStored())))
                .orElse(1L);
        *///?}
    }

    /**
     * Снять HE с чужого FE-предмета: запрос конвертируется в FE и выравнивается
     * по кванту, фактический перенос конвертируется обратно в HE.
     */
    public static long extractHeFromFe(ItemStack stack, long maxHe, boolean simulate) {
        if (maxHe <= 0 || !canForgeExtract(stack)) return 0L;
        long requestFe = EnergyConversion.feFromHe(maxHe) / EnergyConversion.feQuantum() * EnergyConversion.feQuantum();
        int request = (int) Math.min(Integer.MAX_VALUE, requestFe);
        if (request <= 0) return 0L;
        //? if forge {
        int movedFe = getForgeEnergy(stack)
                .filter(net.minecraftforge.energy.IEnergyStorage::canExtract)
                .map(fe -> fe.extractEnergy(request, simulate))
                .orElse(0);
        //?}
        //? if neoforge {
        /*int movedFe = getForgeEnergy(stack)
                .filter(net.neoforged.neoforge.energy.IEnergyStorage::canExtract)
                .map(fe -> fe.extractEnergy(request, simulate))
                .orElse(0);
        *///?}
        return EnergyConversion.heFromFe(movedFe);
    }

    /** Влить HE в чужой FE-предмет (зарядка). */
    public static long receiveHeIntoFe(ItemStack stack, long maxHe, boolean simulate) {
        if (maxHe <= 0 || !canForgeReceive(stack)) return 0L;
        long offerFe = EnergyConversion.feFromHe(maxHe) / EnergyConversion.feQuantum() * EnergyConversion.feQuantum();
        int request = (int) Math.min(Integer.MAX_VALUE, offerFe);
        if (request <= 0) return 0L;
        //? if forge {
        int movedFe = getForgeEnergy(stack)
                .filter(net.minecraftforge.energy.IEnergyStorage::canReceive)
                .map(fe -> fe.receiveEnergy(request, simulate))
                .orElse(0);
        //?}
        //? if neoforge {
        /*int movedFe = getForgeEnergy(stack)
                .filter(net.neoforged.neoforge.energy.IEnergyStorage::canReceive)
                .map(fe -> fe.receiveEnergy(request, simulate))
                .orElse(0);
        *///?}
        return EnergyConversion.heFromFe(movedFe);
    }
}
