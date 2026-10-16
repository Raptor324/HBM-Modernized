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
 * 1:1 port of TileEntityRBMKOutlet.
 * Floor-level block (NOT an RBMK column). Collects reasimSteam from adjacent
 * RBMK columns and exports it to the fluid network.
 */
public class RBMKSteamOutletBlockEntity extends BaseHbmBlockEntity {

    public final FluidTank steamTank = new FluidTank(32_000);

    public RBMKSteamOutletBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_STEAM_OUTLET_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKSteamOutletBlockEntity be) {
        if (level.isClientSide) return;

        // Collect reasimSteam from adjacent RBMK columns (horizontal only)
        for (Direction dir : new Direction[]{ Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof RBMKColumnBlockEntity col) {
                int take = Math.min(be.steamTank.getMaxFill() - be.steamTank.getFill(), col.reasimSteam);
                if (take > 0) {
                    col.reasimSteam -= take;
                    be.steamTank.setFill(be.steamTank.getFill() + take);
                    be.setChanged();
                }
            }
        }
    }

    // ─── NBT / Sync ──────────────────────────────────────────────────────────

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        steamTank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        steamTank.readFromNBT(tag, "tank");
    }
}
