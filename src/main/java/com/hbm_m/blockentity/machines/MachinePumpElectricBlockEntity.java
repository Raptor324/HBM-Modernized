package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1 port of {@code TileEntityMachinePumpElectric}: FE-powered ground-water pump. Uses
 * {@link BaseMachineBlockEntity}'s built-in FE energy handling (matches this port's convention for
 * ALL registered machines, see {@code MachineBoilerBlockEntity}) instead of sharing a base class with
 * {@link MachinePumpSteamBlockEntity} - ground-check logic is instead shared via the static
 * {@link PumpGroundCheck} helper.
 */
public class MachinePumpElectricBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final long MAX_POWER = 10_000L;

    private final FluidTank water = new FluidTank(ModFluids.WATER.getSource(), PumpBlockEntity.ELECTRIC_SPEED * 100);

    public boolean isOn = false;
    public float rotor;
    public float lastRotor;
    public boolean onGround = false;
    private int groundCheckDelay = 0;

    public MachinePumpElectricBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PUMP_ELECTRIC_BE.get(), pos, state, 0, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachinePumpElectricBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            be.clientTick();
            return;
        }
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
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
        if (canOperate() && pos.getY() <= PumpGroundCheck.GROUND_HEIGHT && onGround) {
            isOn = true;
            operate();
        }

        setChanged();
        level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
    }

    private void clientTick() {
        lastRotor = rotor;
        if (isOn) rotor += 10F;
        if (rotor >= 360F) {
            rotor -= 360F;
            lastRotor -= 360F;
        }
    }

    private boolean canOperate() {
        return getEnergyStored() >= 1_000 && water.getFill() < water.getCapacityMb();
    }

    private void operate() {
        setEnergyStored(getEnergyStored() - 1_000);
        water.fillMb(water.getTankType(), Math.min(PumpBlockEntity.ELECTRIC_SPEED, water.getCapacityMb() - water.getFill()));
    }

    public FluidTank getWaterTank() { return water; }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.hbm_m.machine_pump_electric"); }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }

    @Override
    public Component getDisplayName() { return getDefaultName(); }

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
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        water.writeToNBT(tag, "tank_water");
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("onGround", onGround);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        water.readFromNBT(tag, "tank_water");
        isOn = tag.getBoolean("isOn");
        onGround = tag.getBoolean("onGround");
    }
}
