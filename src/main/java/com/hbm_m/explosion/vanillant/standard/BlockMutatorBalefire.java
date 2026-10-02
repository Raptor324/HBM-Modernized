package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IBlockMutator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code BlockMutatorBalefire}: auf festem Boden entsteht zu 1/3 Balefire-Feuer. */
public class BlockMutatorBalefire implements IBlockMutator {

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos) { }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {
        BlockState block = explosion.level.getBlockState(pos);
        BlockState block1 = explosion.level.getBlockState(pos.below());
        if (block.isAir() && block1.isSolidRender(explosion.level, pos.below()) && explosion.level.random.nextInt(3) == 0) {
            explosion.level.setBlockAndUpdate(pos, ModBlocks.BALEFIRE.get().defaultBlockState());
        }
    }
}
