package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IBlockMutator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockMutatorBulkie implements IBlockMutator {

    protected BlockState blockState;

    public BlockMutatorBulkie(Block block) {
        this(block.defaultBlockState());
    }

    public BlockMutatorBulkie(BlockState blockState) {
        this.blockState = blockState;
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos) {
        if (!blockState.isSolidRender(explosion.level, pos)) return;
        Vec3 vec = new Vec3(pos.getX() + 0.5 - explosion.x, pos.getY() + 0.5 - explosion.y, pos.getZ() + 0.5 - explosion.z);
        if (vec.length() >= explosion.size - 0.5) {
            explosion.level.setBlock(pos, blockState, 3);
        }
    }

    @Override public void mutatePost(ExplosionVNT explosion, BlockPos pos) { }
}
