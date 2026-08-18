package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Amat;

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

/**
 * Port of {@code TileEntityMachineDrain} (1.7.10 Original) - a small fluid-network sink that
 * continuously disposes of whatever fluid is piped into it (used to dump excess/waste fluid
 * instead of storing it). Antimatter still triggers a real explosion, matching the original.
 * <p>
 * SCOPE-Vereinfachung: Das Original "verschuettet" die Haelfte des Tankinhalts pro Tick als
 * sichtbare Verschmutzungspartikel/-fluid (Oelpfuetzen via {@code FT_Polluting}) - dieses
 * Partikel-/Verschuettungssystem existiert in diesem Port nicht in der gleichen Form. Hier wird die
 * Fluessigkeit ohne sichtbaren Nebeneffekt entsorgt; der Kernmechanismus (Netzwerk-Fluid-Senke,
 * Amat-Explosion) bleibt erhalten. Multiblock (Original: {@code BlockDummyable}) zu Einzelblock
 * vereinfacht.
 */
public class MachineDrainBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    private static final int TANK_CAPACITY = 2_000;

    private final FluidTank tank = new FluidTank(ModFluids.NONE.getSource(), TANK_CAPACITY);

    public MachineDrainBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_DRAIN_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public FluidTank getTank() { return tank; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDrainBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;
                trySubscribe(tank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        if (tank.getFluidAmountMb() > 0) {
            if (FluidType.getTrait(tank.getStoredFluid(), FT_Amat.class) != null) {
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        10.0F, Level.ExplosionInteraction.BLOCK);
                tank.drainMb(tank.getFluidAmountMb());
                return;
            }

            int toDispose = Math.max(tank.getFluidAmountMb() / 2, 1);
            tank.drainMb(toDispose);
        }

        setChanged();
    }

    public void retype(net.minecraft.world.level.material.Fluid fluid) {
        tank.setTankType(fluid);
        setChanged();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_drain");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.put("tank", tank.writeNBT(new CompoundTag()));
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    tag.put("tank", tank.writeNBT(new CompoundTag()));
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        if (tag.contains("tank")) tank.readNBT(tag.getCompound("tank"));
    }
}
