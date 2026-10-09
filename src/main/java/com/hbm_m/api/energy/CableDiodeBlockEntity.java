package com.hbm_m.api.energy;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import com.hbm_m.capability.ModCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
//?}

/**
 * 1:1 {@code CableDiode.TileEntityDiode}: nimmt als Empfaenger von allen Seiten ausser dem Ausgang Strom an und
 * reicht ihn sofort - hoechstens {@code limit} HE je Tick und 10 Pulse - an Netz oder Empfaenger am Ausgang weiter.
 */
public class CableDiodeBlockEntity extends BaseHbmBlockEntity implements IEnergyReceiver, IControlReceiver {

    /** Innerhalb eines Ticks bereits durchgeleitete Energie. */
    private long power;
    private boolean recursionBrake = false;
    private int pulses = 0;
    public Priority priority = Priority.NORMAL;
    public long limit = 1_000;

    //? if forge {
    private final LazyOptional<IEnergyReceiver> receiverCap = LazyOptional.of(() -> this);
    //?} elif neoforge {
    /*private final com.hbm_m.platform.LazyCap<IEnergyReceiver> receiverCap = com.hbm_m.platform.LazyCap.of(() -> this);
    *///?}

    public CableDiodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_DIODE.get(), pos, state);
    }

    private Direction getDir() {
        return getBlockState().getValue(CableDiodeBlock.FACING).getOpposite();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CableDiodeBlockEntity te) {
        if (!(level instanceof ServerLevel sl)) return;
        Direction out = te.getDir();
        for (Direction dir : Direction.values()) {
            if (dir == out) continue;
            BlockPos p = pos.relative(dir);
            te.trySubscribe(sl, p.getX(), p.getY(), p.getZ(), dir);
        }
        te.pulses = 0;
        te.setEnergyStored(0);
    }

    @Override public boolean canConnectEnergy(Direction side) { return side != getDir(); }

    @Override
    public long transferPower(long power) {
        if (recursionBrake) return power;
        pulses++;
        if (this.getEnergyStored() >= this.getMaxEnergyStored() || pulses > 10) return power;
        if (!(level instanceof ServerLevel sl)) return power;

        recursionBrake = true;
        Direction dir = getDir();
        BlockPos p = worldPosition.relative(dir);
        Nodespace.PowerNode node = Nodespace.getNode(sl, p);
        BlockEntity te = level.getBlockEntity(p);

        if (node != null && !node.expired && node.hasValidNet() && te instanceof IEnergyConnector con && con.canConnectEnergy(dir.getOpposite())) {
            long toTransfer = Math.min(power, this.getReceiveSpeed());
            long remainder = node.net.sendPowerDiode(toTransfer);
            long transferred = toTransfer - remainder;
            this.power += transferred;
            power -= transferred;
        } else if (te instanceof IEnergyReceiver rec && te != this) {
            if (rec.canConnectEnergy(dir.getOpposite())) {
                long toTransfer = Math.min(power, rec.getReceiveSpeed());
                long remainder = rec.transferPower(toTransfer);
                power -= (toTransfer - remainder);
                recursionBrake = false;
                return power;
            }
        }
        recursionBrake = false;
        return power;
    }

    @Override public long getReceiveSpeed() { return this.getMaxEnergyStored() - this.getEnergyStored(); }
    @Override public long getMaxEnergyStored() { return this.limit; }
    @Override public long getEnergyStored() { return Math.min(power, this.getMaxEnergyStored()); }
    @Override public void setEnergyStored(long energy) { this.power = energy; }
    @Override public Priority getPriority() { return this.priority; }
    @Override public boolean canReceive() { return true; }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (simulate) return Math.min(maxReceive, getReceiveSpeed());
        return maxReceive - transferPower(maxReceive);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 128;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("limit")) this.limit = data.getLong("limit");
        if (data.contains("priority")) {
            int p = data.getByte("priority");
            this.priority = Priority.values()[Math.max(0, Math.min(Priority.values().length - 1, p))];
        }
        if (limit < 0) limit = 0;
        if (limit > 10_000_000_000L) limit = 10_000_000_000L;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.putLong("limit", limit);
        nbt.putByte("p", (byte) priority.ordinal());
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        this.limit = nbt.contains("level") ? (long) Math.pow(10, nbt.getInt("level")) : nbt.getLong("limit");
        int p = nbt.getByte("p");
        this.priority = Priority.values()[Math.max(0, Math.min(Priority.values().length - 1, p))];
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER && (side == null || side != getDir())) return receiverCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        receiverCap.invalidate();
    }
    //?} elif neoforge {
    /*@Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.HBM_ENERGY_RECEIVER && (side == null || side != getDir())) return receiverCap.cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        receiverCap.invalidate();
    }
    *///?}
}
