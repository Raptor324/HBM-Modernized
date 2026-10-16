package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 port of TileEntityRBMKInlet.
 * Floor-level block (NOT an RBMK column). Accepts water and feeds it into the
 * reasimWater of adjacent RBMK columns when the ReaSim boiler dial is active.
 */
public class RBMKSteamInletBlockEntity extends BaseHbmBlockEntity {

    public final FluidTank waterTank = new FluidTank(32_000);

    public RBMKSteamInletBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_STEAM_INLET_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKSteamInletBlockEntity be) {
        if (level.isClientSide) return;

        // Distribute water to adjacent RBMK columns (horizontal directions only)
        for (Direction dir : new Direction[]{ Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof RBMKColumnBlockEntity col) {
                int give = Math.min(col.MAX_WATER - col.reasimWater, be.waterTank.getFill());
                if (give > 0) {
                    col.reasimWater += give;
                    be.waterTank.setFill(be.waterTank.getFill() - give);
                    be.setChanged();
                }
            }
        }
    }

    // ─── NBT / Sync ──────────────────────────────────────────────────────────

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        waterTank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        waterTank.readFromNBT(tag, "tank");
    }
}
