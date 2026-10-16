package com.hbm_m.block.network;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code com.hbm.blocks.network.BlockConveyor} (1.7.10 Original) - the basic single-lane
 * conveyor. Sneak-screwdriver-clicking while bent RIGHT swaps the block into a
 * {@link ConveyorLiftBlock} instead of cycling back to straight, exactly like the original
 * (the entry point into the vertical lift/chute chain).
 */
public class ConveyorBlock extends ConveyorBendableBlock {

    public ConveyorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected BlockState onScrewSneak(Level level, BlockPos pos, BlockState state, Player player) {
        if (state.getValue(BEND) == ConveyorBend.RIGHT) {
            level.setBlock(pos, ModBlocks.CONVEYOR_LIFT.get().defaultBlockState().setValue(ConveyorBlockBase.FACING, state.getValue(FACING)), 3);
            return state; // block already swapped directly; caller's setBlock would be a redundant no-op on the new block
        }
        return super.onScrewSneak(level, pos, state, player);
    }
}
