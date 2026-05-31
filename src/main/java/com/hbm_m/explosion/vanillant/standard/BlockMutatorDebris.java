package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IBlockMutator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMutatorDebris implements IBlockMutator {

    protected BlockState debrisState;

    public BlockMutatorDebris(Block block) {
        this(block.defaultBlockState());
    }

    public BlockMutatorDebris(BlockState state) {
        this.debrisState = state;
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos) { }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {
        Level level = explosion.level;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.isSolidRender(level, neighbor) && neighborState != debrisState) {
                level.setBlock(pos, debrisState, Block.UPDATE_ALL);
                return;
            }
        }
    }
}
