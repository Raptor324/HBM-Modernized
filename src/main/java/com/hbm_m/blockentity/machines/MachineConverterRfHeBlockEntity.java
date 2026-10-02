package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
//?}

/**
 * 1:1 {@code TileEntityConverterRfHe}: nimmt RF in einen 1.000.000-Speicher auf und setzt je 2 RF in 5 HE um
 * (Puffer 5.000.000), die an das HE-Netz abgegeben werden.
 */
public class MachineConverterRfHeBlockEntity extends BaseHbmBlockEntity implements IEnergyProvider {

    public long power;
    public final long maxPower = 5_000_000;
    public static long rfInput = 2;
    public static long heOutput = 5;
    public static double inputDecay = 0.0;

    //? if forge {
    public final RFStorage storage = new RFStorage();

    /** RF-Speicher des Originals ({@code cofh.api.energy.EnergyStorage}) mit {@code setEnergyStored}. */
    public static class RFStorage extends EnergyStorage {
        RFStorage() { super(1_000_000, 1_000_000, 1_000_000); }
        public void set(int value) { this.energy = Math.max(0, Math.min(value, capacity)); }
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return super.receiveEnergy(maxReceive, simulate); }
        @Override public boolean canReceive() { return true; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public boolean canExtract() { return false; }
    }
    private final LazyOptional<IEnergyStorage> fe = LazyOptional.of(() -> storage);
    private final LazyOptional<IEnergyProvider> heCap = LazyOptional.of(() -> this);
    //?}

    public MachineConverterRfHeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CONVERTER_RF_HE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineConverterRfHeBlockEntity be) {
        if (level.isClientSide) return;
        //? if forge {
        long rfCreated = Math.min(be.storage.getEnergyStored(), (be.maxPower - be.power) * rfInput / heOutput);
        be.storage.set((int) (be.storage.getEnergyStored() - rfCreated));
        be.power += rfCreated * heOutput / rfInput;
        if (be.storage.getEnergyStored() > 0) be.storage.set(be.storage.getEnergyStored() - (int) Math.ceil(be.storage.getEnergyStored() * inputDecay));
        if (rfCreated > 0) be.setChanged();
        //?}

        for (Direction dir : Direction.values()) {
            be.tryProvide((ServerLevel) level, pos.getX() + dir.getStepX(), pos.getY() + dir.getStepY(), pos.getZ() + dir.getStepZ(), dir);
        }
        level.sendBlockUpdated(pos, state, state, 3);
    }


    @Override public long getEnergyStored() { return power; }
    @Override public long getMaxEnergyStored() { return maxPower; }
    @Override public void setEnergyStored(long energy) { this.power = energy; setChanged(); }
    @Override public long getProvideSpeed() { return maxPower; }
    @Override public boolean canExtract() { return power > 0; }
    @Override public boolean canConnectEnergy(Direction side) { return true; }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        long extracted = Math.min(power, maxExtract);
        if (!simulate && extracted > 0) { power -= extracted; setChanged(); }
        return extracted;
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return fe.cast();
        if (cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_PROVIDER || cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_CONNECTOR) return heCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fe.invalidate();
        heCap.invalidate();
    }
    //?}

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putLong("power", power);
        //? if forge {
        nbt.putInt("Energy", storage.getEnergyStored());
        //?}
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        power = nbt.getLong("power");
        //? if forge {
        storage.set(nbt.getInt("Energy"));
        //?}
    }
}
