package com.hbm_m.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnergyCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final ItemEnergyStorage energyStorage;
    private final LazyOptional<IEnergyStorage> lazyEnergyStorage;

    public EnergyCapabilityProvider(ItemStack stack, int capacity, int maxTransfer) {
        this.energyStorage = new ItemEnergyStorage(stack, capacity, maxTransfer, maxTransfer);
        this.lazyEnergyStorage = LazyOptional.of(() -> this.energyStorage);
    }

    public EnergyCapabilityProvider(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
        this.energyStorage = new ItemEnergyStorage(stack, capacity, maxReceive, maxExtract);
        this.lazyEnergyStorage = LazyOptional.of(() -> this.energyStorage);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyStorage.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("energy", energyStorage.getEnergyStored());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        energyStorage.setEnergy(nbt.getInt("energy"));
    }
}