package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 port of {@code TileEntityRBMKCooler}.
 *
 * <p>Consumes 50 mB of cold coolant per tick and removes 200 heat from every RBMK column in the
 * 5x5 area centred on this one, turning the cold coolant into warm coolant. The neighbour cache is
 * rebuilt every 60 ticks.</p>
 *
 * <p>Two things were missing before: the tanks carried no fluid type at all (CE types them
 * {@code PERFLUOROMETHYL_COLD} in and {@code PERFLUOROMETHYL} out), and the column never joined the
 * fluid network or exposed a handler - so nothing could put coolant in or take the warm coolant
 * out, and the cooler was inert no matter how it was plumbed.</p>
 */
public class RBMKCoolerBlockEntity extends RBMKColumnBlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardTransceiverMK2 {

    public final FluidTank coldTank = new FluidTank(ModFluids.PERFLUOROMETHYL_COLD.getSource(), 4_000);
    public final FluidTank hotTank  = new FluidTank(ModFluids.PERFLUOROMETHYL.getSource(), 4_000);

    /** Heat removed on the last cooling tick - what the console's cooler readout shows. */
    public int lastCooled = 0;

    private int timer = 0;
    private final RBMKColumnBlockEntity[] neighborCache = new RBMKColumnBlockEntity[25];

    private static final int    COOLANT_PER_TICK = 50;
    private static final double HEAT_REDUCTION   = 200.0;
    private static final int    CACHE_INTERVAL   = 60;

    public RBMKCoolerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_COOLER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKCoolerBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;

        if (be.timer <= 0) {
            be.timer = CACHE_INTERVAL;
            for (int i = 0; i < 25; i++) {
                int dx = -2 + i / 5;
                int dz = -2 + i % 5;
                BlockEntity te = level.getBlockEntity(pos.offset(dx, 0, dz));
                be.neighborCache[i] = te instanceof RBMKColumnBlockEntity c ? c : null;
            }
        } else {
            be.timer--;
        }

        if (be.coldTank.getFill() >= COOLANT_PER_TICK
                && be.hotTank.getMaxFill() - be.hotTank.getFill() >= COOLANT_PER_TICK) {

            be.coldTank.setFill(be.coldTank.getFill() - COOLANT_PER_TICK);
            be.hotTank.setFill(be.hotTank.getFill() + COOLANT_PER_TICK);

            int cooled = 0;
            for (RBMKColumnBlockEntity neighbor : be.neighborCache) {
                if (neighbor == null || neighbor.isRemoved()) continue;
                double before = neighbor.heat;
                neighbor.heat -= HEAT_REDUCTION;
                if (neighbor.heat < 20) neighbor.heat = 20;
                int delta = (int) (before - neighbor.heat);
                if (delta > 0) {
                    cooled += delta;
                    neighbor.setChanged();
                }
            }
            be.lastCooled = cooled;
            be.setChanged();
        } else {
            be.lastCooled = 0;
        }

        be.exchangeFluids(level);
    }

    /**
     * Cold coolant is subscribed from directly underneath the column; the warm coolant leaves
     * through the top, plus the sides and bottom of a loader sitting under the column - the same
     * output geometry the boiler channel uses.
     */
    private void exchangeFluids(Level level) {
        BlockPos pos = getBlockPos();
        trySubscribe(coldTank.getTankType(), level, pos.below(), Direction.DOWN);

        if (hotTank.getFill() <= 0) return;
        for (com.mojang.datafixers.util.Pair<BlockPos, Direction> target : getOutputPos(level)) {
            tryProvide(hotTank, level, target.getFirst(), target.getSecond());
        }
    }

    private java.util.List<com.mojang.datafixers.util.Pair<BlockPos, Direction>> getOutputPos(Level level) {
        BlockPos pos = getBlockPos();
        int h = RBMKDials.getColumnHeight(level);
        java.util.List<com.mojang.datafixers.util.Pair<BlockPos, Direction>> out = new java.util.ArrayList<>();
        out.add(com.mojang.datafixers.util.Pair.of(pos.above(h + 1), Direction.UP));

        BlockPos loader = null;
        if (level.getBlockState(pos.below()).is(com.hbm_m.block.ModBlocks.RBMK_LOADER.get())) {
            loader = pos.below();
        } else if (level.getBlockState(pos.below(2)).is(com.hbm_m.block.ModBlocks.RBMK_LOADER.get())) {
            loader = pos.below(2);
        }
        if (loader != null) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP) continue;
                out.add(com.mojang.datafixers.util.Pair.of(loader.relative(dir), dir));
            }
        }
        return out;
    }

    // ─── MK2 fluid network ───────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()       { return new FluidTank[] { coldTank, hotTank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { coldTank }; }
    @Override public FluidTank[] getSendingTanks()   { return new FluidTank[] { hotTank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    //? if forge {
    /** Bottom face takes cold coolant, every other face hands out the warm coolant. */
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap,
            @org.jetbrains.annotations.Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            if (side == Direction.DOWN || side == null) return coldTank.getForgeFluidCapability().cast();
            return hotTank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.COOLER; }

    
    @Override
    public CompoundTag getNBTForConsole() {
        CompoundTag d = new CompoundTag();
        d.putInt("cooled",  lastCooled);
        d.putInt("cryo",    coldTank.getFill());
        d.putInt("maxCryo", coldTank.getMaxFill());
        d.putInt("hot",     hotTank.getFill());
        d.putInt("maxHot",  hotTank.getMaxFill());
        return d;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        coldTank.writeToNBT(tag, "t0");
        hotTank.writeToNBT(tag, "t1");
        tag.putInt("lastCooled", lastCooled);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        coldTank.readFromNBT(tag, "t0");
        hotTank.readFromNBT(tag, "t1");
        lastCooled = tag.getInt("lastCooled");
    }
}
