package com.hbm_m.block.machines.rbmk;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 port of {@code RBMKDebrisDigamma} - what a meltdown leaves behind when the reactor was
 * running Digamma fuel.
 *
 * <p>It does not radiate at all; its only behaviour is spreading. {@code onBlockAdded} schedules
 * exactly ONE block update 2 ticks later; that update converts every neighbour that is a
 * different RBMK rubble subclass or corium into digamma rubble. The chain continues because each
 * newly placed block schedules its own 2-tick update - the block never reschedules itself, so the
 * spread advances exactly one block per 2 ticks.</p>
 */
public class RBMKDebrisDigammaBlock extends Block {

    public RBMKDebrisDigammaBlock(Properties props) {
        super(props);
    }

    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) level.scheduleTick(pos, this, 2);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 1.7.10 RBMKDebrisDigamma.updateTick - one conversion pass, no reschedule.
        for (Direction dir : Direction.values()) {
            BlockPos side = pos.relative(dir);
            BlockState neighbour = level.getBlockState(side);
            if (isConvertible(neighbour)) {
                level.setBlockAndUpdate(side, defaultBlockState());
            }
        }
    }

    /** Any other kind of RBMK rubble, plus corium, turns into digamma rubble on contact. */
    private static boolean isConvertible(BlockState state) {
        return state.is(ModBlocks.RBMK_DEBRIS.get())
                || state.is(ModBlocks.RBMK_DEBRIS_BURNING.get())
                || state.is(ModBlocks.RBMK_DEBRIS_RADIATING.get())
                || state.is(ModBlocks.RBMK_CORIUM.get()) || state.is(ModBlocks.CORIUM_BLOCK.get());
    }
}
