package com.hbm_m.platform;

import net.minecraft.world.level.material.Fluid;

/**
 * Кросс-платформенный бэкенд для хранения жидкости (Forge / NeoForge FluidTank).
 */
public interface IPlatformFluidHandler {
    int getFluidAmountMb();
    int getCapacityMb();
    void setCapacityMb(int capacity);
    Fluid getStoredFluid();
    int fillMb(Fluid fluid, int amount, boolean simulate);
    int drainMb(int amount, boolean simulate);
    void setFluid(Fluid fluid, int amount);
    Object getCapability(); // IFluidHandler (NeoForge) / LazyOptional (Forge)
    net.minecraft.nbt.CompoundTag writeNBT(net.minecraft.nbt.CompoundTag tag);
    void readNBT(net.minecraft.nbt.CompoundTag tag);

    default net.minecraft.nbt.CompoundTag writeNBT(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.nbt.CompoundTag tag) {
        return writeNBT(tag);
    }

    default void readNBT(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.nbt.CompoundTag tag) {
        readNBT(tag);
    }
}