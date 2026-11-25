package com.hbm_m.blockentity.machines;

import java.util.Iterator;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.energy.PowerNet;
import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.util.SafeRedstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 port of {@code BlockNetworkSeparator.TileEntityNetworkSeparator} (1.7.10).
 */
public class NetworkSeparatorBlockEntity extends BaseHbmBlockEntity
        implements IEnergyProvider, IEnergyReceiver, IControlReceiver, IRORValueProvider, IRORInteractive {

    public static final long MAX_LIMIT = 10_000_000_000L;

    protected long deltaTracker = 0L;
    public long[] deltaTick = new long[20];
    public long deltaSecond = 0L;

    private long power = 0L;
    public IEnergyReceiver.Priority priority = IEnergyReceiver.Priority.NORMAL;
    public boolean redstone = false;
    public long configuredLimit = 1_000L;
    public long actualLimit = configuredLimit;

    public NetworkSeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_SEPARATOR_BE.get(), pos, state);
    }

    /** 1.7.10: {@code dir.getRotation(DOWN)} for output, {@code opposite} for input. */
    public Direction getDir(boolean opposite) {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(DummyableMachineBlock.FACING)
                ? state.getValue(DummyableMachineBlock.FACING)
                : Direction.NORTH;
        Direction outDir = facing.getCounterClockWise();
        return opposite ? outDir.getOpposite() : outDir;
    }

    public BlockPos getOutPortPos() {
        return worldPosition.relative(getDir(false));
    }

    public BlockPos getInPortPos() {
        return worldPosition.relative(getDir(true));
    }

    public boolean isInputPart(BlockPos partPos) {
        return partPos.equals(getInPortPos());
    }

    public boolean isOutputPart(BlockPos partPos) {
        return partPos.equals(getOutPortPos());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, NetworkSeparatorBlockEntity be) {
        if (level instanceof ServerLevel sl) {
            be.serverTick(sl);
        }
    }

    private void serverTick(ServerLevel sl) {
        Direction inDir = getDir(true);
        Direction outDir = getDir(false);
        BlockPos inNeighbor = getInPortPos().relative(inDir);
        BlockPos outNeighbor = getOutPortPos().relative(outDir);

        this.trySubscribe(sl, inNeighbor.getX(), inNeighbor.getY(), inNeighbor.getZ(), inDir);
        this.tryProvide(sl, outNeighbor.getX(), outNeighbor.getY(), outNeighbor.getZ(), outDir);

        this.actualLimit = this.configuredLimit;

        Nodespace.PowerNode outNode = Nodespace.getNode(sl, outNeighbor);
        if (outNode != null && outNode.net != null) {
            this.surveyOutgoingNetwork(outNode.net);
        }

        this.actualLimit = Math.max(this.actualLimit, this.power);

        long total = this.deltaTick[0];
        for (int i = 1; i < this.deltaTick.length; i++) {
            total += this.deltaTick[i];
            this.deltaTick[i - 1] = this.deltaTick[i];
        }
        this.deltaTick[19] = deltaTracker;
        this.deltaTracker = 0L;
        this.deltaSecond = total;

        this.redstone = SafeRedstone.hasNeighborSignal(sl, worldPosition)
                || SafeRedstone.hasNeighborSignal(sl, getOutPortPos())
                || SafeRedstone.hasNeighborSignal(sl, getInPortPos());

        long lo = configuredLimit ^ (((long) priority.ordinal()) << 40) ^ (redstone ? (1L << 48) : 0L);
        long hi = deltaTick[19] * 31L + deltaSecond;
        syncIfStateChanged(lo, hi);
    }

    protected void surveyOutgoingNetwork(PowerNet net) {
        long demand = 0L;
        Iterator<Map.Entry<IEnergyReceiver, Long>> it = net.receiverEntries.entrySet().iterator();
        while (it.hasNext()) {
            IEnergyReceiver rec = it.next().getKey();
            if (rec instanceof NetworkSeparatorBlockEntity sep) {
                demand += sep.actualLimit;
            } else {
                demand += Math.min(rec.getMaxEnergyStored() - rec.getEnergyStored(), rec.getReceiveSpeed());
            }
        }
        if (demand < this.actualLimit) {
            this.actualLimit = demand;
        }
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        Direction rot = getDir(false);
        return side == rot || side == rot.getOpposite();
    }

    @Override
    public void usePower(long power) {
        this.setEnergyStored(this.getEnergyStored() - power);
        this.deltaTracker += power;
    }

    @Override public long getReceiveSpeed() { return redstone || bufferMismatch() ? 0L : this.getMaxEnergyStored(); }
    @Override public long getProvideSpeed() { return redstone ? 0L : this.getMaxEnergyStored(); }
    @Override public long getMaxEnergyStored() { return actualLimit; }
    @Override public long getEnergyStored() { return power; }
    @Override public void setEnergyStored(long power) { this.power = power; }
    @Override public IEnergyReceiver.Priority getPriority() { return this.priority; }
    @Override public boolean canReceive() { return true; }
    @Override public boolean canExtract() { return true; }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (simulate) return Math.min(maxReceive, getReceiveSpeed());
        long accepted = Math.min(maxReceive, Math.max(0L, getMaxEnergyStored() - getEnergyStored()));
        if (accepted > 0) setEnergyStored(getEnergyStored() + accepted);
        return accepted;
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        long extracted = Math.min(maxExtract, Math.min(getEnergyStored(), getProvideSpeed()));
        if (!simulate && extracted > 0) {
            setEnergyStored(getEnergyStored() - extracted);
            this.deltaTracker += extracted;
        }
        return extracted;
    }

    public boolean bufferMismatch() {
        return this.configuredLimit < this.getMaxEnergyStored();
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        tag.putLong("power", power);
        tag.putLong("configuredLimit", configuredLimit);
        tag.putLong("actualLimit", actualLimit);
        tag.putByte("p", (byte) this.priority.ordinal());
        tag.putBoolean("redstone", redstone);
        tag.putLong("deltaTick", deltaTick[19]);
        tag.putLong("deltaSecond", deltaSecond);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        this.power = tag.getLong("power");
        this.configuredLimit = tag.contains("configuredLimit") ? tag.getLong("configuredLimit") : 1_000L;
        this.actualLimit = tag.contains("actualLimit") ? tag.getLong("actualLimit") : this.configuredLimit;
        int ord = Mth.clamp(tag.getByte("p"), 0, IEnergyReceiver.Priority.values().length - 1);
        this.priority = IEnergyReceiver.Priority.values()[ord];
        this.redstone = tag.getBoolean("redstone");
        this.deltaTick[19] = tag.getLong("deltaTick");
        this.deltaSecond = tag.getLong("deltaSecond");
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 128.0D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("limit")) this.configuredLimit = data.getLong("limit");
        if (data.contains("priority")) {
            int ord = Mth.clamp(data.getByte("priority"), 0, IEnergyReceiver.Priority.values().length - 1);
            this.priority = IEnergyReceiver.Priority.values()[ord];
        }
        if (configuredLimit < 0L) configuredLimit = 0L;
        if (configuredLimit > MAX_LIMIT) configuredLimit = MAX_LIMIT;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "deltatick",
                PREFIX_VALUE + "deltasecond",
                PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)",
                PREFIX_FUNCTION + "setlimit" + NAME_SEPARATOR + "limit",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "deltatick").equals(name)) return "" + deltaTick[19];
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return "" + deltaSecond;
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            int prio = IRORInteractive.parseInt(params[0], 0, 2) + 1;
            int ord = Mth.clamp(prio, 0, IEnergyReceiver.Priority.values().length - 1);
            this.priority = IEnergyReceiver.Priority.values()[ord];
            this.setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setlimit").equals(name) && params.length > 0) {
            this.configuredLimit = parseLong(params[0], 0L, MAX_LIMIT);
            this.setChanged();
            return null;
        }
        return null;
    }

    private static long parseLong(String val, long min, long max) {
        long result;
        try {
            result = Long.parseLong(val);
        } catch (Exception x) {
            try {
                result = Math.round(Double.parseDouble(val));
            } catch (Exception y) {
                throw new com.hbm_m.api.redstoneoverradio.RORFunctionException(IRORInteractive.EX_FORMAT);
            }
        }
        if (result < min || result > max) {
            throw new com.hbm_m.api.redstoneoverradio.RORFunctionException(IRORInteractive.EX_FORMAT);
        }
        return result;
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2);
    }
}
