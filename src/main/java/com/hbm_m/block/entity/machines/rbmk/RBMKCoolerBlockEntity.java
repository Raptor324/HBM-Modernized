package com.hbm_m.block.entity.machines.rbmk;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 port of TileEntityRBMKCooler.
 * Consumes 50 mB cold coolant per tick and removes 200 heat from every block
 * in a 5x5 area centred on this column.
 * Neighbor cache is rebuilt every 60 ticks.
 */
public class RBMKCoolerBlockEntity extends RBMKColumnBlockEntity {

    public final FluidTank coldTank = new FluidTank(4_000);  // cold coolant input
    public final FluidTank hotTank  = new FluidTank(4_000);  // warm coolant output

    private int timer = 0;
    private final RBMKColumnBlockEntity[] neighborCache = new RBMKColumnBlockEntity[25];

    private static final int  COOLANT_PER_TICK = 50;
    private static final double HEAT_REDUCTION  = 200.0;
    private static final int  CACHE_INTERVAL   = 60;

    public RBMKCoolerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_COOLER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKCoolerBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;

        // Rebuild neighbor cache every 60 ticks
        if (be.timer <= 0) {
            be.timer = CACHE_INTERVAL;
            for (int i = 0; i < 25; i++) {
                int dx = -2 + i / 5;
                int dz = -2 + i % 5;
                BlockPos np = pos.offset(dx, 0, dz);
                BlockEntity te = level.getBlockEntity(np);
                be.neighborCache[i] = te instanceof RBMKColumnBlockEntity c ? c : null;
            }
        } else {
            be.timer--;
        }

        // Cool all 25 neighbors if we have coolant and output space
        if (be.coldTank.getFill() >= COOLANT_PER_TICK
                && be.hotTank.getMaxFill() - be.hotTank.getFill() >= COOLANT_PER_TICK) {

            be.coldTank.setFill(be.coldTank.getFill() - COOLANT_PER_TICK);
            be.hotTank.setFill(be.hotTank.getFill() + COOLANT_PER_TICK);

            for (RBMKColumnBlockEntity neighbor : be.neighborCache) {
                if (neighbor != null) {
                    neighbor.heat -= HEAT_REDUCTION;
                    if (neighbor.heat < 20) neighbor.heat = 20;
                }
            }
            be.setChanged();
        }
    }

    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.COOLER; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        coldTank.writeToNBT(tag, "t0");
        hotTank.writeToNBT(tag, "t1");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        coldTank.readFromNBT(tag, "t0");
        hotTank.readFromNBT(tag, "t1");
    }
}
