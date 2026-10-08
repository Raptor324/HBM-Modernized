package com.hbm_m.blockentity.machines;

import javax.annotation.Nullable;

import com.hbm_m.block.machines.PowerDetectorBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * audit10: 1:1 {@code TileEntityMachineDetector} ({@code detector}): nimmt an allen Seiten Strom an (Puffer 5 HE,
 * Prioritaet HIGH), verbraucht 1 HE je Tick und ist dabei "an" (Original-Metadate 1 = Redstone 15).
 */
public class MachineDetectorBlockEntity extends BlockEntity implements IEnergyReceiver {

    public long power;

    public MachineDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_DETECTOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineDetectorBlockEntity be) {
        be.updateEntity((ServerLevel) level);
    }

    private void updateEntity(ServerLevel world) {

        this.updateConnections(world);

        boolean meta = getBlockState().getValue(PowerDetectorBlock.POWERED);
        boolean state = false;

        if (power > 0) {
            state = true;
            power--;
        }

        if (meta != state) {
            world.setBlock(worldPosition, getBlockState().setValue(PowerDetectorBlock.POWERED, state), 3);
            this.setChanged();
        }
    }

    private void updateConnections(ServerLevel world) {
        for (Direction dir : Direction.values()) {
            BlockPos p = worldPosition.relative(dir);
            this.trySubscribe(world, p.getX(), p.getY(), p.getZ(), dir);
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.power = nbt.getLong("power");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("power", power);
    }

    // --- Energie (IEnergyReceiverMK2) ---

    @Override public long getEnergyStored() { return power; }
    @Override public long getMaxEnergyStored() { return 5; }
    @Override public void setEnergyStored(long energy) { this.power = Math.max(0, Math.min(5, energy)); setChanged(); }
    @Override public long getReceiveSpeed() { return 5; }
    @Override public IEnergyReceiver.Priority getPriority() { return IEnergyReceiver.Priority.HIGH; }
    @Override public boolean canReceive() { return power < 5; }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long received = Math.min(5 - power, maxReceive);
        if (!simulate && received > 0) setEnergyStored(power + received);
        return received;
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }

    //? if forge {
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER)
            return net.minecraftforge.common.util.LazyOptional.of(() -> (IEnergyReceiver) this).cast();
        if (cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_CONNECTOR)
            return net.minecraftforge.common.util.LazyOptional.of(() -> (com.hbm_m.interfaces.IEnergyConnector) this).cast();
        return super.getCapability(cap, side);
    }
    //?}
}
