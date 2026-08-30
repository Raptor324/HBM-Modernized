package com.hbm_m.platform;

import net.minecraft.world.level.material.Fluid;

/**
 * Кросс-платформенный бэкенд для хранения жидкости. 
 * Скрывает под собой Forge/NeoForge FluidTank и Fabric SingleVariantStorage.
 */
public interface IPlatformFluidHandler {
    int getFluidAmountMb();
    int getCapacityMb();
    void setCapacityMb(int capacity);
    Fluid getStoredFluid();
    int fillMb(Fluid fluid, int amount, boolean simulate);
    int drainMb(int amount, boolean simulate);
    void setFluid(Fluid fluid, int amount);
    Object getCapability(); // IFluidHandler (NeoForge) / LazyOptional (Forge) / Storage (Fabric)
    net.minecraft.nbt.CompoundTag writeNBT(net.minecraft.nbt.CompoundTag tag);
    void readNBT(net.minecraft.nbt.CompoundTag tag);
}