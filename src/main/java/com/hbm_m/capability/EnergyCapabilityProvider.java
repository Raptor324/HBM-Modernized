package com.hbm_m.capability;

// Импорты твоих новых классов
import com.hbm_m.energy.ILongEnergyStorage;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.energy.LongToForgeWrapper;
// ИЗМЕНЕНИЕ: Импортируем НОВЫЙ ItemEnergyStorage
import com.hbm_m.energy.ItemEnergyStorage;

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

    // ИЗМЕНЕНИЕ: Тип поля теперь - новый ItemEnergyStorage
    private final ItemEnergyStorage energyStorage;
    private final LazyOptional<ILongEnergyStorage> lazyLongEnergyStorage;
    private final LazyOptional<IEnergyStorage> lazyForgeEnergyStorage;

    // Конструкторы теперь принимают long
    public EnergyCapabilityProvider(ItemStack stack, long capacity, long maxTransfer) {
        this(stack, capacity, maxTransfer, maxTransfer);
    }

    public EnergyCapabilityProvider(ItemStack stack, long capacity, long maxReceive, long maxExtract) {
        // Теперь этот вызов корректен, так как ItemEnergyStorage принимает long
        this.energyStorage = new ItemEnergyStorage(stack, capacity, maxReceive, maxExtract);
        this.lazyLongEnergyStorage = LazyOptional.of(() -> this.energyStorage);

        // Создаем адаптер LongToForgeWrapper для совместимости
        this.lazyForgeEnergyStorage = this.lazyLongEnergyStorage.lazyMap(LongToForgeWrapper::new);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // (Отвечаем на запросы о нашей long-системе)
        if (cap == ModCapabilities.LONG_ENERGY) {
            return lazyLongEnergyStorage.cast();
        }

        // (Отвечаем на запросы о старой int-системе)
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyForgeEnergyStorage.cast();
        }

        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("energy", energyStorage.getEnergyStored()); // <-- putLong (уже было)
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        // Теперь этот вызов корректен, т.к. у нового ItemEnergyStorage есть setEnergy(long)
        energyStorage.setEnergy(nbt.getLong("energy")); // <-- getLong (уже было)
    }

    // (Можно добавить этот метод, чтобы инвалидировать из предмета)
    public void invalidate() {
        lazyLongEnergyStorage.invalidate();
        lazyForgeEnergyStorage.invalidate();
    }
}