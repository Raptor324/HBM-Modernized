package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IBlockMutator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMutatorFire implements IBlockMutator {

    @Override public void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos) { }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {
        BlockState state = explosion.level.getBlockState(pos);
        BlockPos below = pos.below();
        BlockState belowState = explosion.level.getBlockState(below);

        if (state.isAir() && belowState.isSolidRender(explosion.level, pos.below()) && explosion.level.getRandom().nextInt(3) == 0) {
            explosion.level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
        }
    }
}
