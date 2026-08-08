package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Shared base for the water pump family. 1:1 port of {@code TileEntityMachinePumpBase} (1.7.10):
 * a ground-water pump that only operates when at least half of the 3x3 column beneath it (down to
 * {@link PumpGroundCheck#GROUND_DEPTH} blocks, with the first layer required to be a full solid
 * block) is one of the recognized "ground" block types, and the pump is at or below {@link #GROUND_HEIGHT}.
 * Concrete subclasses ({@code MachinePumpSteamBlockEntity}/{@code MachinePumpElectricBlockEntity})
 * supply the actual power source and {@link #canOperate()}/{@link #operate()}.
 */
public abstract class PumpBlockEntity extends BlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int GROUND_HEIGHT = PumpGroundCheck.GROUND_HEIGHT;
    public static final int STEAM_SPEED = 1_000;
    public static final int ELECTRIC_SPEED = 10_000;

    protected final FluidTank water;

    public boolean isOn = false;
    public float rotor;
    public float lastRotor;
    public boolean onGround = false;
    private int groundCheckDelay = 0;

    protected PumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int waterCapacity) {
        super(type, pos, state);
        this.water = new FluidTank(ModFluids.WATER.getSource(), waterCapacity);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PumpBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            be.clientTick();
            return;
        }
        be.serverTick(serverLevel, pos);
    }

    protected void serverTick(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos conPos = pos.relative(dir, 2);
            if (water.getFill() > 0) {
                tryProvide(water, level, conPos, dir);
            }
        }

        if (groundCheckDelay > 0) {
            groundCheckDelay--;
        } else {
            onGround = PumpGroundCheck.check(level, pos);
            groundCheckDelay = 20;
        }

        isOn = false;
        if (canOperate() && pos.getY() <= GROUND_HEIGHT && onGround) {
            isOn = true;
            operate();
        }

        setChanged();
        level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
    }

    protected void clientTick() {
        lastRotor = rotor;
        if (isOn) rotor += 10F;
        if (rotor >= 360F) {
            rotor -= 360F;
            lastRotor -= 360F;
        }
    }

    protected abstract boolean canOperate();
    protected abstract void operate();

    public FluidTank getWaterTank() { return water; }

    // ── IFluidStandardTransceiverMK2 ───────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return new FluidTank[]{ water }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[]{ water }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[0]; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ── NBT ───────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        water.writeToNBT(tag, "tank_water");
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("onGround", onGround);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        water.readFromNBT(tag, "tank_water");
        isOn = tag.getBoolean("isOn");
        onGround = tag.getBoolean("onGround");
    }
}
