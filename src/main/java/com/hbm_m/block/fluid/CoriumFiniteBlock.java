package com.hbm_m.block.fluid;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code CoriumFinite} ({@code corium_block}): endliche, extrem schwere Schmelze
 * (5 Quanten, Tickrate 30), die sich mit einer Chance je nach Explosionswiderstand durch Bloecke
 * frisst, Beruehrende anzuendet, bestrahlt und langsam zu {@code block_corium} bzw.
 * {@code block_corium_cobble} erstarrt.
 */
public class CoriumFiniteBlock extends FiniteFluidBlock {

    public CoriumFiniteBlock(Properties properties) {
        super(properties, 5, 30);
    }

    @Override
    public boolean canDisplace(Level level, BlockPos pos) {
        BlockState b = level.getBlockState(pos);
        if (b.getBlock() == this) return false;
        float res = (float) (Math.sqrt(b.getBlock().getExplosionResistance()) * 3);
        if (res < 1) return true;
        return !b.getFluidState().isEmpty() || level.random.nextInt((int) res) == 0;
    }

    @Override
    public boolean displaceIfPossible(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).getFluidState().isEmpty()) return false;
        return canDisplace(level, pos);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
        entity.setSecondsOnFire(3);
        entity.hurt(ModDamageSources.radiation(level), 2F);
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, 1F);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        super.tick(state, level, pos, rand);
        if (rand.nextInt(10) == 0 && level.getBlockState(pos).getBlock() == this && level.getBlockState(pos.below()).getBlock() != this) {
            if (rand.nextInt(3) == 0) level.setBlockAndUpdate(pos, ModBlocks.BLOCK_CORIUM.get().defaultBlockState());
            else level.setBlockAndUpdate(pos, ModBlocks.BLOCK_CORIUM_COBBLE.get().defaultBlockState());
        }
    }

    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return false;
    }
}
