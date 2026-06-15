package com.hbm_m.block.entity.machines;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import org.jetbrains.annotations.Nullable;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?} else {
/*import org.jetbrains.annotations.Nullable;
*///?}
import org.jetbrains.annotations.NotNull;

/**
 * BlockEntity for the (large) Cooling Tower multiblock.
 *
 * Purely passive fluid-to-fluid heat exchanger: converts "Hot Coolant" into
 * "Coolant" at a fixed rate per tick, as long as the structure has sky access
 * and the input tank has fluid / the output tank has space. No energy, no GUI -
 * input/output happen exclusively via the MK2 fluid pipe network.
 */
public class MachineCoolingTowerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int TANK_HOT_IN = 0;
    public static final int TANK_COOLANT_OUT = 1;

    /** Large tower: 5x5x13 footprint, central ladder shaft. */
    private static final int TANK_CAPACITY_HOT = 64_000;
    private static final int TANK_CAPACITY_COOLANT = 64_000;
    /** mB of hot coolant converted into cooled coolant per tick (passive, no energy). */
    private static final int CONVERSION_RATE_MB = 50;

    /** Relative Y offset (above the controller) used for the sky-access check and particle spawn. */
    private static final int STRUCTURE_HEIGHT = 13;

    private static final int PARTICLE_INTERVAL_TICKS = 10;

    private final FluidTank[] tanks = new FluidTank[] {
        new FluidTank(ModFluids.COOLANT_HOT.getSource(), TANK_CAPACITY_HOT),
        new FluidTank(ModFluids.COOLANT.getSource(), TANK_CAPACITY_COOLANT)
    };

    private boolean isCooling = false;
    private int particleTimer = 0;

    //? if forge {
    private LazyOptional<IFluidHandler> fluidHandler = LazyOptional.empty();
    //?}

    public MachineCoolingTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOLING_TOWER_BE.get(), pos, state, 0, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCoolingTowerBlockEntity be) {
        if (level.isClientSide()) {
            be.clientTick(level, pos);
            return;
        }

        FluidTank hotIn = be.tanks[TANK_HOT_IN];
        FluidTank coolantOut = be.tanks[TANK_COOLANT_OUT];

        boolean canConvert = hotIn.getFill() > 0
                && coolantOut.getSpaceMb() > 0
                && be.hasSkyAccess(level, pos);

        boolean changed = false;
        if (canConvert) {
            int amount = Math.min(CONVERSION_RATE_MB, Math.min(hotIn.getFill(), coolantOut.getSpaceMb()));
            if (amount > 0) {
                hotIn.drainMb(amount);
                coolantOut.fillMb(ModFluids.COOLANT.getSource(), amount);
                changed = true;
            }
        }

        if (be.isCooling != canConvert) {
            be.isCooling = canConvert;
            changed = true;
        }

        if (changed) {
            be.setChanged();
            be.sendUpdateToClient();
        }

        be.ensureNetworkInitialized();
    }

    /** Sky-access check: looks for an unobstructed path to the sky from the top of the structure. */
    private boolean hasSkyAccess(Level level, BlockPos pos) {
        BlockPos topPos = pos.above(STRUCTURE_HEIGHT - 1);
        return level.canSeeSky(topPos);
    }

    private void clientTick(Level level, BlockPos pos) {
        if (!isCooling) return;

        particleTimer++;
        if (particleTimer < PARTICLE_INTERVAL_TICKS) return;
        particleTimer = 0;

        if (!(level instanceof ServerLevel)) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + STRUCTURE_HEIGHT;
            double z = pos.getZ() + 0.5D;
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0D, 0.05D, 0.0D);
            level.addParticle(ParticleTypes.CLOUD, x + 0.3D, y, z - 0.3D, 0.0D, 0.05D, 0.0D);
            level.addParticle(ParticleTypes.CLOUD, x - 0.3D, y, z + 0.3D, 0.0D, 0.05D, 0.0D);
        }
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    public FluidTank getTank(int index) {
        return (index >= 0 && index < tanks.length) ? tanks[index] : tanks[0];
    }

    public boolean isCooling() {
        return isCooling;
    }

    // ═══════════════════════════ IFluidStandardTransceiverMK2 ════════════════════════════════

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[TANK_HOT_IN] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[TANK_COOLANT_OUT] };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank_" + i);
        }
        tag.putBoolean("isCooling", isCooling);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank_" + i);
        }
        isCooling = tag.getBoolean("isCooling");
    }

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        fluidHandler = LazyOptional.of(() -> new CoolingTowerFluidHandler(this));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }
    //?}

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.cooling_tower");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // No inventory slots are exposed for this machine.
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        // No GUI - purely passive fluid-to-fluid converter.
        return null;
    }
}
