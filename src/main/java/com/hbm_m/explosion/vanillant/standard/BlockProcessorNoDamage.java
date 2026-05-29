package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm_m.explosion.vanillant.interfaces.IBlockProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Iterator;

public class BlockProcessorNoDamage implements IBlockProcessor {

    protected IBlockMutator convert;

    public BlockProcessorNoDamage() { }

    public BlockProcessorNoDamage withBlockEffect(IBlockMutator convert) {
        this.convert = convert;
        return this;
    }

    @Override
    public void process(ExplosionVNT explosion, Level level, double x, double y, double z, HashSet<BlockPos> affectedBlocks) {

        for (BlockPos pos : affectedBlocks) {
            BlockState state = level.getBlockState(pos);

            if (!state.isAir()) {
                if (this.convert != null) this.convert.mutatePre(explosion, state, pos);
            }
        }

        if (this.convert != null) {
            for (BlockPos pos : affectedBlocks) {
                BlockState state = level.getBlockState(pos);

                if (state.isAir()) {
                    this.convert.mutatePost(explosion, pos);
                }
            }
        }

        explosion.compat.getToBlow().clear(); //tricks the standard SFX to not do the block damage particles
    }
}
