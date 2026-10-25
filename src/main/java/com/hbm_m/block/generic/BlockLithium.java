package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockLithium} ({@code block_lithium}, Leuchtfeuer-Sockel): beruehrt Wasser -&gt;
 * Block weg und Explosion Staerke 15 (mit Blockschaden, ohne Feuer); unter freiem Himmel raucht er.
 */
public class BlockLithium extends Block {

    public BlockLithium(Properties properties) {
        super(properties);
    }

    private static boolean touchesWater(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        for (Direction d : Direction.values()) {
            if (world.getFluidState(pos.relative(d)).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    private static void react(Level world, BlockPos pos) {
        world.destroyBlock(pos, false);
        world.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, false, Level.ExplosionInteraction.BLOCK);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (touchesWater(world, pos)) react(world, pos);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        if (oldState.getBlock() != this && touchesWater(world, pos)) react(world, pos);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        if (world.isRainingAt(pos.above())) {
            float ox = rand.nextFloat();
            float oz = rand.nextFloat();
            world.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + ox, pos.getY() + 1, pos.getZ() + oz, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx);
    }
}
