package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
//?}

/**
 * 1:1 {@code TileEntityConverterHeRf}: HE-Empfaenger (Puffer 5.000.000, Prioritaet niedrig), der je 5 HE in 1 RF umsetzt
 * und das RF aus einem 1.000.000-Speicher an Nachbarn abgibt.
 */
public class MachineConverterHeRfBlockEntity extends BaseHbmBlockEntity implements IEnergyReceiver {

    public long power;
    public final long maxPower = 5_000_000;
    public static long heInput = 5;
    public static long rfOutput = 1;
    public static double inputDecay = 0.0;

    //? if forge {
    public final RFStorage storage = new RFStorage();

    /** RF-Speicher des Originals ({@code cofh.api.energy.EnergyStorage}) mit {@code setEnergyStored}. */
    public static class RFStorage extends EnergyStorage {
        RFStorage() { super(1_000_000, 1_000_000, 1_000_000); }
        public void set(int value) { this.energy = Math.max(0, Math.min(value, capacity)); }
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public boolean canReceive() { return false; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return super.extractEnergy(maxExtract, simulate); }
        @Override public boolean canExtract() { return true; }
    }
    private final LazyOptional<IEnergyStorage> fe = LazyOptional.of(() -> storage);
    private final LazyOptional<IEnergyReceiver> heCap = LazyOptional.of(() -> this);
    //?} elif neoforge {
    /*public final RFStorage storage = new RFStorage();

    /^* RF-Speicher des Originals ({@code cofh.api.energy.EnergyStorage}) mit {@code setEnergyStored}. ^/
    public static class RFStorage extends net.neoforged.neoforge.energy.EnergyStorage {
        RFStorage() { super(1_000_000, 1_000_000, 1_000_000); }
        public void set(int value) { this.energy = Math.max(0, Math.min(value, capacity)); }
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public boolean canReceive() { return false; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return super.extractEnergy(maxExtract, simulate); }
        @Override public boolean canExtract() { return true; }
    }
    private final com.hbm_m.platform.LazyCap<net.neoforged.neoforge.energy.IEnergyStorage> fe = com.hbm_m.platform.LazyCap.of(() -> storage);
    private final com.hbm_m.platform.LazyCap<IEnergyReceiver> heCap = com.hbm_m.platform.LazyCap.of(() -> this);
    *///?}

    public MachineConverterHeRfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CONVERTER_HE_RF_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineConverterHeRfBlockEntity be) {
        if (level.isClientSide) return;
        //? if forge {
        long rfCreated = Math.min(be.storage.getMaxEnergyStored() - be.storage.getEnergyStored(), be.power / heInput * rfOutput);
        be.power -= rfCreated * heInput / rfOutput;
        be.storage.set((int) (be.storage.getEnergyStored() + rfCreated));
        if (be.power > 0) be.power *= (1D - inputDecay);
        if (rfCreated > 0) be.setChanged();

        for (Direction dir : Direction.values()) {
            be.trySubscribe((ServerLevel) level, pos.getX() + dir.getStepX(), pos.getY() + dir.getStepY(), pos.getZ() + dir.getStepZ(), dir);

            BlockEntity entity = level.getBlockEntity(pos.relative(dir));
            if (entity != null) {
                entity.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(receiver -> {
                    int maxAvailable = Math.min(be.storage.getEnergyStored(), 1_000_000);
                    int transferred = receiver.receiveEnergy(maxAvailable, false);
                    be.storage.set(be.storage.getEnergyStored() - transferred);
                });
            }
        }
        //?} elif neoforge {
        /*long rfCreated = Math.min(be.storage.getMaxEnergyStored() - be.storage.getEnergyStored(), be.power / heInput * rfOutput);
        be.power -= rfCreated * heInput / rfOutput;
        be.storage.set((int) (be.storage.getEnergyStored() + rfCreated));
        if (be.power > 0) be.power *= (1D - inputDecay);
        if (rfCreated > 0) be.setChanged();

        for (Direction dir : Direction.values()) {
            be.trySubscribe((ServerLevel) level, pos.getX() + dir.getStepX(), pos.getY() + dir.getStepY(), pos.getZ() + dir.getStepZ(), dir);

            BlockEntity entity = level.getBlockEntity(pos.relative(dir));
            if (entity != null) {
                com.hbm_m.platform.HbmCaps.get(entity, com.hbm_m.platform.HbmCap.ENERGY, dir.getOpposite()).ifPresent(receiver -> {
                    int maxAvailable = Math.min(be.storage.getEnergyStored(), 1_000_000);
                    int transferred = receiver.receiveEnergy(maxAvailable, false);
                    be.storage.set(be.storage.getEnergyStored() - transferred);
                });
            }
        }
        *///?}
        level.sendBlockUpdated(pos, state, state, 3);
    }


    @Override public long getEnergyStored() { return power; }
    @Override public long getMaxEnergyStored() { return maxPower; }
    @Override public void setEnergyStored(long energy) { this.power = energy; setChanged(); }
    @Override public long getReceiveSpeed() { return maxPower; }
    @Override public Priority getPriority() { return Priority.LOW; }
    @Override public boolean canReceive() { return power < maxPower; }
    @Override public boolean canConnectEnergy(Direction side) { return true; }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long received = Math.min(maxPower - power, maxReceive);
        if (!simulate && received > 0) { power += received; setChanged(); }
        return received;
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return fe.cast();
        if (cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER || cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_CONNECTOR) return heCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fe.invalidate();
        heCap.invalidate();
    }
    //?} elif neoforge {
    /*@Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ENERGY) return fe.cast();
        if (cap == com.hbm_m.platform.HbmCap.HBM_ENERGY_RECEIVER || cap == com.hbm_m.platform.HbmCap.HBM_ENERGY_CONNECTOR) return heCap.cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        fe.invalidate();
        heCap.invalidate();
    }
    *///?}

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putLong("power", power);
        //? if forge || neoforge {
        nbt.putInt("Energy", storage.getEnergyStored());
        //?}
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        power = nbt.getLong("power");
        //? if forge || neoforge {
        storage.set(nbt.getInt("Energy"));
        //?}
    }
}
