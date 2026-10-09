package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
//?}

/**
 * 1:1 {@code TileEntityDeuteriumExtractor} (machine_deuterium_extractor): Einzelblock, der aus allen sechs Seiten
 * Wasser (1.000 mB) und Strom (10.000 HE, 500 HE je Arbeitstakt) nimmt und je 50 mB Wasser 1 mB Schwerwasser
 * (Tank 100 mB) erzeugt, das er an alle Seiten abgibt.
 */
public class DeuteriumExtractorBlockEntity extends BaseHbmBlockEntity implements IEnergyReceiver, IFluidStandardTransceiverMK2 {

    public long power = 0;
    public FluidTank[] tanks;

    //? if forge {
    private final LazyOptional<IEnergyReceiver> heCap = LazyOptional.of(() -> this);
    //?} elif neoforge {
    /*private final com.hbm_m.platform.LazyCap<IEnergyReceiver> heCap = com.hbm_m.platform.LazyCap.of(() -> this);
    *///?}

    public DeuteriumExtractorBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.DEUTERIUM_EXTRACTOR_BE.get(), pos, state);
    }

    protected DeuteriumExtractorBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tanks = new FluidTank[2];
        tanks[0] = new FluidTank(ModFluids.WATER.getSource(), 1000);
        tanks[1] = new FluidTank(ModFluids.HEAVYWATER.getSource(), 100);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DeuteriumExtractorBlockEntity be) {
        if (level instanceof ServerLevel server) be.update(server, pos, state);
    }

    private void update(ServerLevel world, BlockPos pos, BlockState state) {

        this.updateConnections(world, pos);

        if (hasPower() && hasEnoughWater() && tanks[1].getMaxFill() > tanks[1].getFill()) {
            int convert = Math.min(tanks[1].getMaxFill(), tanks[0].getFill()) / 50;
            convert = Math.min(convert, tanks[1].getMaxFill() - tanks[1].getFill());

            tanks[0].setFill(tanks[0].getFill() - convert * 50); //dividing first, then multiplying, will remove any rounding issues
            tanks[1].setFill(tanks[1].getFill() + convert);
            power -= this.getMaxEnergyStored() / 20;
        }

        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            this.trySubscribe(tanks[0].getTankType(), world, at, dir);
            if (tanks[1].getFill() > 0) this.tryProvide(tanks[1], world, at, dir);
        }

        // networkPackNT(50)
        setChanged();
        world.sendBlockUpdated(pos, state, state, 3);
    }

    protected void updateConnections(ServerLevel world, BlockPos pos) {
        for (Direction dir : Direction.values())
            this.trySubscribe(world, pos.getX() + dir.getStepX(), pos.getY() + dir.getStepY(), pos.getZ() + dir.getStepZ(), dir);
    }

    public boolean hasPower() {
        return power >= this.getMaxEnergyStored() / 20;
    }

    public boolean hasEnoughWater() {
        return tanks[0].getFill() >= 100;
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.power = nbt.getLong("power");
        tanks[0].readFromNBT(nbt, "water");
        tanks[1].readFromNBT(nbt, "heavyWater");
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putLong("power", power);
        tanks[0].writeToNBT(nbt, "water");
        tanks[1].writeToNBT(nbt, "heavyWater");
    }

    // --- Energie (IEnergyReceiverMK2) ---

    @Override public long getEnergyStored() { return power; }
    @Override public long getMaxEnergyStored() { return 10_000; }
    @Override public void setEnergyStored(long energy) { this.power = energy; }
    @Override public long getReceiveSpeed() { return getMaxEnergyStored(); }
    @Override public Priority getPriority() { return Priority.NORMAL; }
    @Override public boolean canReceive() { return power < getMaxEnergyStored(); }
    @Override public boolean canConnectEnergy(Direction side) { return true; }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long received = Math.min(getMaxEnergyStored() - power, maxReceive);
        if (!simulate && received > 0) power += received;
        return received;
    }

    // --- Fluessigkeiten ---

    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }
    @Override public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER || cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_CONNECTOR) return heCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        heCap.invalidate();
    }
    //?} elif neoforge {
    /*@Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.HBM_ENERGY_RECEIVER || cap == com.hbm_m.platform.HbmCap.HBM_ENERGY_CONNECTOR) return heCap.cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        heCap.invalidate();
    }
    *///?}
}
