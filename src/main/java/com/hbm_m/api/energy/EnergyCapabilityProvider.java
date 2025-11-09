package com.hbm_m.api.energy;

import com.hbm_m.capability.ModCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Провайдер capability для ItemStack (батарейки).
 * Предоставляет IEnergyProvider и IEnergyReceiver для предметов.
 * Совместим с Forge Energy через LongEnergyWrapper.
 */
public class EnergyCapabilityProvider implements ICapabilityProvider {

    private final LazyOptional<ItemEnergyStorage> storage;

    public EnergyCapabilityProvider(ItemStack stack, long capacity, long maxReceive, long maxExtract) {
        this.storage = LazyOptional.of(() -> new ItemEnergyStorage(stack, capacity, maxReceive, maxExtract));
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // Предоставляем HBM capabilities
        if (cap == ModCapabilities.HBM_ENERGY_PROVIDER && storage.resolve().map(ItemEnergyStorage::canExtract).orElse(false)) {
            return storage.cast();
        }
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER && storage.resolve().map(ItemEnergyStorage::canReceive).orElse(false)) {
            return storage.cast();
        }

        // Совместимость с Forge Energy (всегда LOW биты для простоты)
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY) {
            return storage.lazyMap(s -> new LongEnergyWrapper(s, LongEnergyWrapper.BitMode.LOW)).cast();
        }

        return LazyOptional.empty();
    }

    /**
     * Внутреннее хранилище энергии для ItemStack.
     * Работает с NBT тега "energy".
     */
    private static class ItemEnergyStorage implements IEnergyProvider, IEnergyReceiver {
        private final ItemStack stack;
        private final long capacity;
        private final long maxReceive;
        private final long maxExtract;

        ItemEnergyStorage(ItemStack stack, long capacity, long maxReceive, long maxExtract) {
            this.stack = stack;
            this.capacity = capacity;
            this.maxReceive = maxReceive;
            this.maxExtract = maxExtract;
        }

        @Override
        public long getEnergyStored() {
            return stack.getOrCreateTag().getLong("energy");
        }

        @Override
        public void setEnergyStored(long energy) {
            long clamped = Math.max(0, Math.min(energy, capacity));
            stack.getOrCreateTag().putLong("energy", clamped);
        }

        @Override
        public long getMaxEnergyStored() {
            return this.capacity;
        }

        // --- IEnergyReceiver ---
        @Override
        public long receiveEnergy(long maxReceive, boolean simulate) {
            if (!canReceive()) return 0;

            long energyStored = getEnergyStored();
            long energyReceived = Math.min(capacity - energyStored, Math.min(this.maxReceive, maxReceive));

            if (!simulate && energyReceived > 0) {
                setEnergyStored(energyStored + energyReceived);
            }

            return energyReceived;
        }

        @Override
        public long getReceiveSpeed() {
            return this.maxReceive;
        }

        @Override
        public Priority getPriority() {
            return Priority.NORMAL;
        }

        @Override
        public boolean canReceive() {
            return this.maxReceive > 0 && getEnergyStored() < capacity;
        }

        // --- IEnergyProvider ---
        @Override
        public long extractEnergy(long maxExtract, boolean simulate) {
            if (!canExtract()) return 0;

            long energyStored = getEnergyStored();
            long energyExtracted = Math.min(energyStored, Math.min(this.maxExtract, maxExtract));

            if (!simulate && energyExtracted > 0) {
                setEnergyStored(energyStored - energyExtracted);
            }

            return energyExtracted;
        }

        @Override
        public long getProvideSpeed() {
            return this.maxExtract;
        }

        @Override
        public boolean canExtract() {
            return this.maxExtract > 0 && getEnergyStored() > 0;
        }

        @Override
        public boolean canConnectEnergy(Direction side) {
            return true;
        }
    }
}