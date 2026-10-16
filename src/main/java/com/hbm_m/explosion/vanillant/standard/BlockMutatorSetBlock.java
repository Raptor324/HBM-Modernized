package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IBlockMutator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Аналог атрибутов {@code ExAttrib.LAVA_V}/{@code ExAttrib.LAVA_R} 1.7.10
 * ({@code ExplosionNT}): каждый блок, разрушенный взрывом, заменяется заданным
 * состоянием (в оригинале — вулканической/радиоактивной лавой). Используется
 * магмовыми каналами и камерами ядра вулкана и лавовыми сгустками шрапнели.
 */
public class BlockMutatorSetBlock implements IBlockMutator {

    private final Supplier<BlockState> state;

    public BlockMutatorSetBlock(Supplier<BlockState> state) {
        this.state = state;
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos) {
    }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {
        if (explosion.level.getBlockState(pos).isAir()) {
            explosion.level.setBlock(pos, this.state.get(), 3);
        }
    }
}
