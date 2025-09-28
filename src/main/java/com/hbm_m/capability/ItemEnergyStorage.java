package com.hbm_m.capability;

// Данный класс предоставляет capability для хранения энергии (FE) в предмете.
// Он реализует ICapabilityProvider и INBTSerializable для интеграции с системой capability Minecraft Forge.
// Энергия хранится в ItemEnergyStorage, который привязан к ItemStack, позволяя сохранять энергию вместе с предметом. Похоже, есть дубликат. TODO: унифицировать.
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;

public class ItemEnergyStorage implements IEnergyStorage {
    private final ItemStack stack;
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;
    private static final String NBT_KEY = "energy";

    public ItemEnergyStorage(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
        this.stack = stack;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }


    public void setEnergy(int energy) {
        if (energy < 0) energy = 0;
        if (energy > capacity) energy = capacity;
        this.stack.getOrCreateTag().putInt(NBT_KEY, energy);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int energy = getEnergyStored();
        int energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate) {
            setEnergy(energy + energyReceived);
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int energy = getEnergyStored();
        int energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate) {
            setEnergy(energy - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public int getEnergyStored() {
        return stack.hasTag() ? stack.getTag().getInt(NBT_KEY) : 0;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }
}