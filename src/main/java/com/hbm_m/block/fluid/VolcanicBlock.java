package com.hbm_m.block.fluid;

import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * 1:1-Port von {@code VolcanicBlock} (Vulkanlava): erstarrt zu Basalt bzw. mit Glueck zu
 * Basalterzen (Schwefel, Fluorit, Asbest, Molysit, Vulkangestein nur tief im Lavastrom),
 * reagiert mit Wasser (Stein), Holz (Verseuchtes Holz), Laub (Feuer) und Diamanterz
 * (Basalt-Schwefelerz im Original-Meta 3 = Vulkanedelstein).
 */
public class VolcanicBlock extends HbmFluidBlock {

    public VolcanicBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public boolean ticksRandomly() {
        return true;
    }

    @Override
    protected void onNeighborChange(Level level, BlockPos pos, BlockState state) {
        for (Direction dir : Direction.values()) {
            BlockPos p = pos.relative(dir);
            BlockState b = getReaction(level, p);
            if (b != null) level.setBlock(p, b, 3);
        }
    }

    public BlockState getReaction(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.getFluidState().is(FluidTags.WATER)) return Blocks.STONE.defaultBlockState();
        if (s.is(BlockTags.LOGS)) return ModBlocks.WASTE_LOG.get().defaultBlockState();
        if (s.is(BlockTags.PLANKS)) return ModBlocks.WASTE_PLANKS.get().defaultBlockState();
        if (s.is(BlockTags.LEAVES)) return Blocks.FIRE.defaultBlockState();
        if (s.getBlock() == Blocks.DIAMOND_ORE || s.getBlock() == Blocks.DEEPSLATE_DIAMOND_ORE) return ModBlocks.ORE_BASALT_GEM.get().defaultBlockState();
        return null;
    }

    @Override
    public void onFluidTick(Level level, BlockPos pos, BlockState state) {
        solidifyCheck(level, pos, level.random);
    }

    @Override
    public void onRandomFluidTick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        solidifyCheck(level, pos, random);
    }

    private void solidifyCheck(Level level, BlockPos pos, RandomSource rand) {
        if (level.isClientSide) return;
        int lavaCount = 0;
        int basaltCount = 0;
        for (Direction dir : Direction.values()) {
            Block b = level.getBlockState(pos.relative(dir)).getBlock();
            if (b == this) lavaCount++;
            if (b == getBasaltForCheck()) basaltCount++;
        }
        boolean isSource = level.getFluidState(pos).isSource();
        if (((!isSource && lavaCount < 2) || (rand.nextInt(5) == 0) && lavaCount < 5) && level.getBlockState(pos.below()).getBlock() != this) {
            this.onSolidify(level, pos, lavaCount, basaltCount, rand);
        }
    }

    public Block getBasaltForCheck() {
        return ModBlocks.BASALT.get();
    }

    public void onSolidify(Level level, BlockPos pos, int lavaCount, int basaltCount, RandomSource rand) {
        int r = rand.nextInt(200);
        Block above = level.getBlockState(pos.above(10)).getBlock();
        boolean canMakeGem = lavaCount + basaltCount == 6 && lavaCount < 3 && (above == ModBlocks.BASALT.get() || above == ModBlocks.VOLCANIC_LAVA_BLOCK.get());

        if (r < 2) level.setBlock(pos, ModBlocks.ORE_BASALT_SULFUR.get().defaultBlockState(), 3);
        else if (r == 2) level.setBlock(pos, ModBlocks.ORE_BASALT_FLUORITE.get().defaultBlockState(), 3);
        else if (r == 3) level.setBlock(pos, ModBlocks.ORE_BASALT_ASBESTOS.get().defaultBlockState(), 3);
        else if (r == 4) level.setBlock(pos, ModBlocks.ORE_BASALT_MOLYSITE.get().defaultBlockState(), 3);
        else if (r < 15 && canMakeGem) level.setBlock(pos, ModBlocks.ORE_BASALT_GEM.get().defaultBlockState(), 3);
        else level.setBlock(pos, ModBlocks.BASALT.get().defaultBlockState(), 3);
    }

    @Override
    public Boolean canDisplace(BlockGetter level, BlockPos pos, BlockState target) {
        if (((FireBlock) Blocks.FIRE).getBurnOdds(target) > 0) return true;
        if (target.canBeReplaced()) return true;
        return null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        double dx, dy, dz;
        BlockState up = level.getBlockState(pos.above());
        if (up.isAir() && !up.isSolidRender(level, pos.above())) {
            if (rand.nextInt(100) == 0) {
                dx = pos.getX() + rand.nextFloat();
                dy = pos.getY() + 1.0D;
                dz = pos.getZ() + rand.nextFloat();
                level.addParticle(ParticleTypes.LAVA, dx, dy, dz, 0.0D, 0.0D, 0.0D);
                level.playLocalSound(dx, dy, dz, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
            if (rand.nextInt(200) == 0) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
        }
        if (rand.nextInt(10) == 0 && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && !level.getBlockState(pos.below(2)).blocksMotion()) {
            dx = pos.getX() + rand.nextFloat();
            dy = pos.getY() - 1.05D;
            dz = pos.getZ() + rand.nextFloat();
            level.addParticle(ParticleTypes.DRIPPING_LAVA, dx, dy, dz, 0.0D, 0.0D, 0.0D);
        }
    }
}
