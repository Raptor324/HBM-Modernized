package com.hbm_m.blockentity.machines;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachineFoundryOutletBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the 1.7.10 TileEntityFoundrySlagtap (extends TileEntityFoundryOutlet). Instead of pouring
 * into an {@link com.hbm_m.api.block.ICrucibleAcceptor} below, it dumps excess/overflow material as
 * a world-placed {@link SlagBlockEntity} puddle - used as the foundry's pressure-relief valve.
 */
public class MachineFoundrySlagtapBlockEntity extends MachineFoundryOutletBlockEntity {

    private static final int POUR_RANGE = 4;

    public MachineFoundrySlagtapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_SLAGTAP_BE.get(), pos, state);
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (!passesFilter(stack.type)) return false;
        if (isClosed()) return false;
        Direction facing = getBlockState().hasProperty(MachineFoundryOutletBlock.FACING)
                ? getBlockState().getValue(MachineFoundryOutletBlock.FACING) : Direction.NORTH;
        return side == facing.getOpposite();
    }

    @Override
    public @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        for (int i = 0; i <= POUR_RANGE; i++) {
            BlockPos scan = pos.below(i);
            var block = level.getBlockState(scan).getBlock();

            if (level.getBlockState(scan).isAir()) continue;

            if (block == ModBlocks.SLAG_DYNAMIC.get()) {
                BlockEntity be = level.getBlockEntity(scan);
                if (be instanceof SlagBlockEntity slag) {
                    return slag.tryAdd(stack);
                }
                return stack;
            }

            // solid, non-slag ground found: place a fresh puddle on top of it (or right here if we're already above it)
            BlockPos placeAt = scan.above();
            if (!level.getBlockState(placeAt).isAir()) return stack;

            level.setBlock(placeAt, ModBlocks.SLAG_DYNAMIC.get().defaultBlockState(), 3);
            if (level.getBlockEntity(placeAt) instanceof SlagBlockEntity slag) {
                return slag.tryAdd(stack);
            }
            return stack;
        }

        // nothing but air down to range: place directly below the outlet
        BlockPos placeAt = pos.below(POUR_RANGE);
        if (!level.getBlockState(placeAt).isAir()) return stack;
        level.setBlock(placeAt, ModBlocks.SLAG_DYNAMIC.get().defaultBlockState(), 3);
        if (level.getBlockEntity(placeAt) instanceof SlagBlockEntity slag) {
            return slag.tryAdd(stack);
        }
        return stack;
    }
}
