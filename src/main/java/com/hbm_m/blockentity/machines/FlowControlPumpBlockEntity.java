package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.ConnectionPriority;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IFluidPartDelegateProvider;
import com.hbm_m.inventory.fluid.tank.FluidTank;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 port of {@code FlowControlPump.TileEntityFlowControlPump} (1.7.10).
 */
public class FlowControlPumpBlockEntity extends BaseHbmBlockEntity
        implements IFluidStandardTransceiverMK2, IControlReceiver, IFluidPartDelegateProvider {

    public static final float ROTOR_ACCELERATION = 1.0F;

    public int bufferSize = 100;
    public final FluidTank tank = new FluidTank(Fluids.EMPTY, bufferSize);
    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    public boolean redstone = false;

    public boolean topRail = false;
    public boolean bottomRail = true;
    public boolean isWorking = false;

    public float rotor;
    public float prevRotor;
    public float rotorSpeed;

    private final FluidTank[] allTanks = new FluidTank[] { tank };
    private final FluidTank[] noTanks = new FluidTank[0];

    private final IFluidStandardTransceiverMK2 inDelegate = new InPortDelegate();
    private final IFluidStandardTransceiverMK2 outDelegate = new OutPortDelegate();

    public FlowControlPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOW_CONTROL_PUMP_BE.get(), pos, state);
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING)
                ? state.getValue(DummyableMachineBlock.FACING)
                : Direction.NORTH;
    }

    /** 1.7.10: {@code dir.getRotation(ForgeDirection.DOWN)}. */
    public Direction getOutDir() {
        return facing().getCounterClockWise();
    }

    /** 1.7.10: {@code dir.getRotation(ForgeDirection.UP)}. */
    public Direction getInDir() {
        return facing().getClockWise();
    }

    public BlockPos getOutPortPos() {
        return worldPosition.relative(getOutDir());
    }

    public BlockPos getInPortPos() {
        return worldPosition.relative(getInDir());
    }

    public boolean bufferMismatch() {
        return this.bufferSize != this.tank.getMaxFill();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FlowControlPumpBlockEntity be) {
        if (level instanceof ServerLevel sl) {
            be.serverTick(sl);
        } else {
            be.clientTick(level);
        }
    }

    private void serverTick(ServerLevel sl) {
        Direction inDir = getInDir();
        Direction outDir = getOutDir();
        BlockPos inPos = getInPortPos();
        BlockPos outPos = getOutPortPos();

        this.trySubscribe(tank.getTankType(), sl, inPos.relative(inDir), inDir);
        if (!redstone) {
            this.tryProvide(tank, sl, outPos.relative(outDir), outDir);
        }

        // Preserve excess buffered fluid until drained rather than destroying it on capacity shrink.
        if (bufferMismatch()) {
            int nextBuffer = Math.max(this.tank.getFill(), this.bufferSize);
            this.tank.changeTankSize(nextBuffer);
        }

        this.redstone = SafeRedstone.hasNeighborSignal(sl, worldPosition)
                || SafeRedstone.hasNeighborSignal(sl, outPos)
                || SafeRedstone.hasNeighborSignal(sl, inPos);

        long lo = ((long) bufferSize) | (((long) priority.ordinal()) << 32)
                | (redstone ? (1L << 40) : 0L) | (isWorking ? (1L << 41) : 0L);
        long hi = (((long) tank.getFill() * 31L + tank.getMaxFill()) * 31L + tank.getPressure()) * 31L
                + tank.getTankType().hashCode();
        syncIfStateChanged(lo, hi);
        this.isWorking = false;
    }

    private void clientTick(Level level) {
        if (level.getGameTime() % 20L == 0L) {
            topRail = !level.getBlockState(worldPosition.above()).isAir();
            bottomRail = !level.getBlockState(worldPosition.below()).isAir();
        }

        if (isWorking) {
            this.rotorSpeed += ROTOR_ACCELERATION;
        } else {
            this.rotorSpeed -= ROTOR_ACCELERATION;
        }
        this.rotorSpeed = Mth.clamp(this.rotorSpeed, 0.0F, 30.0F);

        this.prevRotor = this.rotor;
        this.rotor += this.rotorSpeed;
        if (this.rotor >= 360.0F) {
            this.rotor -= 360.0F;
            this.prevRotor -= 360.0F;
        }
    }

    @Override
    @Nullable
    public Object getFluidDelegateForPart(BlockPos partPos) {
        if (partPos.equals(getInPortPos())) return inDelegate;
        if (partPos.equals(getOutPortPos())) return outDelegate;
        return null;
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        Direction rot = getOutDir();
        return fromDir == rot || fromDir == rot.getOpposite();
    }

    @Override
    public void useUpFluid(Fluid fluid, int pressure, long amount) {
        if (amount > 0 && VanillaFluidEquivalence.sameSubstance(tank.getTankType(), fluid)
                && tank.getPressure() == pressure) {
            int toRem = (int) Math.min(amount, tank.getFill());
            tank.setFill(tank.getFill() - toRem);
            this.isWorking = true;
        }
    }

    @Override public ConnectionPriority getFluidPriority() { return priority; }
    @Override public FluidTank[] getSendingTanks() { return allTanks; }
    @Override public FluidTank[] getReceivingTanks() { return bufferMismatch() ? noTanks : allTanks; }
    @Override public FluidTank[] getAllTanks() { return allTanks; }
    @Override public long getReceiverSpeed(Fluid fluid, int pressure) { return redstone || bufferMismatch() ? 0 : 1_000_000_000L; }
    @Override public long getProviderSpeed(Fluid fluid, int pressure) { return redstone ? 0 : 1_000_000_000L; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 128.0D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("capacity")) {
            this.bufferSize = Mth.clamp(data.getInt("capacity"), 0, 10_000);
        }
        if (data.contains("pressure")) {
            this.tank.withPressure(Mth.clamp(data.getByte("pressure"), 0, 5));
        }
        if (data.contains("priority")) {
            int ord = Mth.clamp(data.getByte("priority"), 0, ConnectionPriority.values().length - 1);
            this.priority = ConnectionPriority.values()[ord];
        }
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        tank.writeToNBT(tag, "t");
        tag.putByte("p", (byte) priority.ordinal());
        tag.putInt("buffer", bufferSize);
        tag.putBoolean("redstone", redstone);
        tag.putBoolean("working", isWorking);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        tank.readFromNBT(tag, "t");
        int ord = Mth.clamp(tag.getByte("p"), 0, ConnectionPriority.values().length - 1);
        priority = ConnectionPriority.values()[ord];
        bufferSize = tag.contains("buffer") ? tag.getInt("buffer") : 100;
        redstone = tag.getBoolean("redstone");
        isWorking = tag.getBoolean("working");
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 1, worldPosition.getZ() + 2);
    }

    private class InPortDelegate implements IFluidStandardTransceiverMK2 {
        @Override public FluidTank[] getSendingTanks() { return noTanks; }
        @Override public FluidTank[] getReceivingTanks() { return FlowControlPumpBlockEntity.this.getReceivingTanks(); }
        @Override public FluidTank[] getAllTanks() { return allTanks; }
        @Override public boolean isLoaded() { return FlowControlPumpBlockEntity.this.isLoaded(); }
        @Override public ConnectionPriority getFluidPriority() { return priority; }
        @Override public long getReceiverSpeed(Fluid f, int p) { return FlowControlPumpBlockEntity.this.getReceiverSpeed(f, p); }
    }

    private class OutPortDelegate implements IFluidStandardTransceiverMK2 {
        @Override public FluidTank[] getSendingTanks() { return FlowControlPumpBlockEntity.this.getSendingTanks(); }
        @Override public FluidTank[] getReceivingTanks() { return noTanks; }
        @Override public FluidTank[] getAllTanks() { return allTanks; }
        @Override public boolean isLoaded() { return FlowControlPumpBlockEntity.this.isLoaded(); }
        @Override public ConnectionPriority getFluidPriority() { return priority; }
        @Override public long getProviderSpeed(Fluid f, int p) { return FlowControlPumpBlockEntity.this.getProviderSpeed(f, p); }
        @Override public void useUpFluid(Fluid f, int p, long amt) { FlowControlPumpBlockEntity.this.useUpFluid(f, p, amt); }
    }
}
