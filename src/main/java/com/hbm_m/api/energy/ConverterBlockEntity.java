package com.hbm_m.api.energy; // Подставь свой пакет

import com.hbm_m.api.energy.*;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Реализуем твои HBM интерфейсы
public class ConverterBlockEntity extends BlockEntity implements IEnergyReceiver, IEnergyProvider {

    private long energy = 0;
    // Настройки: Емкость 100 млн HE, скорость 1 млн HE/тик
    public static final long CAPACITY = 100_000_000L;
    public static final long TRANSFER_RATE = 1_000_000L;

    // Capability для Forge Energy
    private final HbmForgeWrapper forgeWrapper = new HbmForgeWrapper(this);
    private final LazyOptional<IEnergyStorage> forgeCap = LazyOptional.of(() -> forgeWrapper);

    // Capabilities для HBM (чтобы провода подключались)
    // Мы можем возвращать 'this', так как класс реализует интерфейсы
    private final LazyOptional<IEnergyProvider> hbmProviderCap = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyReceiver> hbmReceiverCap = LazyOptional.of(() -> this);

    public ConverterBlockEntity(BlockPos pos, BlockState state) {
        // Передаем тип ВНУТРИ super(), беря его из нашего реджистри
        super(ModBlockEntities.CONVERTER_BE.get(), pos, state);
    }

    // === ЛОГИКА ТИКА (TICK) ===
    public static void serverTick(Level level, BlockPos pos, BlockState state, ConverterBlockEntity be) {
        if (be.energy <= 0) return;

        // Активная раздача энергии в соседние блоки (PUSH)
        // Это нужно, потому что многие FE трубы пассивны и ждут, что в них затолкают энергию
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null) {
                // Ищем FE у соседа
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                    if (storage.canReceive()) {
                        // Рассчитываем сколько отдать
                        long canExtract = Math.min(be.energy, TRANSFER_RATE);
                        int toPush = (int) Math.min(canExtract, Integer.MAX_VALUE);

                        // Пытаемся засунуть
                        int accepted = storage.receiveEnergy(toPush, false);

                        // Если взяли - списываем у себя
                        if (accepted > 0) {
                            be.extractEnergy(accepted, false);
                        }
                    }
                });
            }
        }
    }

    // === CAPABILITIES ===
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // 1. Для Forge Energy модов (Mekanism, Thermal, etc.)
        if (cap == ForgeCapabilities.ENERGY) {
            return forgeCap.cast();
        }
        // 2. Для HBM проводов
        if (cap == ModCapabilities.HBM_ENERGY_PROVIDER) {
            return hbmProviderCap.cast();
        }
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER) {
            return hbmReceiverCap.cast();
        }
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            // Можно вернуть провайдер или ресивер как коннектор, или обертку
            return hbmProviderCap.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        forgeCap.invalidate();
        hbmProviderCap.invalidate();
        hbmReceiverCap.invalidate();
    }

    // === HBM IMPLEMENTATION ===
    @Override
    public long getEnergyStored() { return energy; }

    @Override
    public void setEnergyStored(long energy) {
        this.energy = Math.min(energy, CAPACITY);
        setChanged();
    }

    @Override
    public long getMaxEnergyStored() { return CAPACITY; }

    @Override
    public long getProvideSpeed() { return TRANSFER_RATE; }

    @Override
    public long getReceiveSpeed() { return TRANSFER_RATE; }

    @Override
    public Priority getPriority() { return Priority.NORMAL; }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        long extracted = Math.min(energy, Math.min(maxExtract, TRANSFER_RATE));
        if (!simulate && extracted > 0) {
            energy -= extracted;
            setChanged();
        }
        return extracted;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long space = CAPACITY - energy;
        long received = Math.min(space, Math.min(maxReceive, TRANSFER_RATE));
        if (!simulate && received > 0) {
            energy += received;
            setChanged();
        }
        return received;
    }

    @Override
    public boolean canExtract() { return true; }

    @Override
    public boolean canReceive() { return true; }

    @Override
    public boolean canConnectEnergy(Direction side) { return true; }

    // === NBT ===
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("energy", energy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getLong("energy");
    }
}
