package com.hbm_m.api.energy;

import com.hbm_m.api.energy.*;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConverterBlockEntity extends BlockEntity implements IEnergyReceiver, IEnergyProvider {

    private long energy = 0;

    // Тиры
    private static final long[] TIERS = { 1_000L, 10_000L, 50_000L, 100_000L, 1_000_000L, 100_000_000L, (long)Integer.MAX_VALUE };
    private int tierIndex = 2;
    private long currentLimit = TIERS[tierIndex];

    // Режимы: 0=Bi, 1=Export(H->F), 2=Import(F->H)
    private int ioMode = 0;

    // Capabilities
    private final HbmForgeWrapper forgeWrapper = new HbmForgeWrapper(this);
    private LazyOptional<IEnergyStorage> forgeCap = LazyOptional.of(() -> forgeWrapper);

    private final LazyOptional<IEnergyProvider> hbmProviderCap = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyReceiver> hbmReceiverCap = LazyOptional.of(() -> this);

    public ConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVERTER_BE.get(), pos, state);
    }

    // --- ВАЖНО: ЖИЗНЕННЫЙ ЦИКЛ ---

    @Override
    public void onLoad() {
        super.onLoad();
        // Пересоздаем капу при загрузке, если она умерла
        if (!forgeCap.isPresent()) {
            forgeCap = LazyOptional.of(() -> forgeWrapper);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        forgeCap.invalidate();
        // Примечание: HBM капы у нас статические (this), их можно не инвалидировать жестко,
        // но для чистоты можно. Главное - ForgeCap.
    }

    // --- Управление ---
    public void cycleLimit() {
        tierIndex = (tierIndex + 1) % TIERS.length;
        currentLimit = TIERS[tierIndex];
        setChanged();
    }

    public void cycleMode() {
        ioMode = (ioMode + 1) % 3;
        setChanged();
    }

    public String getModeName() {
        return switch (ioMode) {
            case 1 -> "HBM -> FE (Export Only)";
            case 2 -> "FE -> HBM (Import Only)";
            default -> "Bi-Directional";
        };
    }
    public long getCurrentLimit() { return currentLimit; }

    // --- Логика ---
    public static void serverTick(Level level, BlockPos pos, BlockState state, ConverterBlockEntity be) {
        if (be.energy <= 0) return;
        if (be.ioMode == 2) return; // Импорт онли

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                    if (storage.canReceive()) {
                        long canExtract = Math.min(be.energy, be.currentLimit);
                        int toPush = (int) Math.min(canExtract, Integer.MAX_VALUE);
                        int accepted = storage.receiveEnergy(toPush, false);
                        if (accepted > 0) {
                            be.energy -= accepted;
                            be.setChanged();
                        }
                    }
                });
            }
        }
    }

    // --- HBM ---
    @Override
    public boolean canExtract() { return (ioMode == 0 || ioMode == 2) && energy > 0; }

    @Override
    public boolean canReceive() { return (ioMode == 0 || ioMode == 1) && energy < currentLimit; }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        long limit = Math.min(maxExtract, currentLimit);
        long extracted = Math.min(energy, limit);
        if (!simulate && extracted > 0) {
            energy -= extracted;
            setChanged();
        }
        return extracted;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        long space = currentLimit - energy;
        long limit = Math.min(maxReceive, currentLimit);
        long received = Math.min(space, limit);
        if (!simulate && received > 0) {
            energy += received;
            setChanged();
        }
        return received;
    }

    @Override public long getEnergyStored() { return energy; }
    @Override public void setEnergyStored(long energy) { this.energy = Math.min(energy, currentLimit); setChanged(); }
    @Override public long getMaxEnergyStored() { return currentLimit; }
    @Override public long getProvideSpeed() { return currentLimit; }
    @Override public long getReceiveSpeed() { return currentLimit; }
    @Override public Priority getPriority() { return Priority.NORMAL; }
    @Override public boolean canConnectEnergy(Direction side) { return true; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return forgeCap.cast();
        if (cap == ModCapabilities.HBM_ENERGY_PROVIDER) return hbmProviderCap.cast();
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER) return hbmReceiverCap.cast();
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) return hbmProviderCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("energy", energy);
        tag.putInt("tierIndex", tierIndex);
        tag.putInt("ioMode", ioMode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getLong("energy");
        if (tag.contains("tierIndex")) {
            tierIndex = tag.getInt("tierIndex");
            if (tierIndex >= 0 && tierIndex < TIERS.length) currentLimit = TIERS[tierIndex];
        }
        if (tag.contains("ioMode")) ioMode = tag.getInt("ioMode");
    }
}