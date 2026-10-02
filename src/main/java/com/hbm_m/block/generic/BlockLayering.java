package com.hbm_m.block.generic;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockLayering} ({@code foam_layer}, {@code sand_boron_layer}, {@code leaves_layer}):
 * Schichten wie Schnee (Hoehe 2/16 je Stufe, Metadaten &amp; 7 = {@link SnowLayerBlock#LAYERS} - 1), aber ohne Schmelzen,
 * haelt auf RBMK-/ZIRNOX-Truemmern, Laub, voller Schicht derselben Art und festen Bloecken (nicht auf Eis), wirft
 * nichts ab. Laub- und Schaumschicht sind immer ersetzbar, sonst nur unter voller Hoehe.
 */
public class BlockLayering extends SnowLayerBlock {

    public BlockLayering(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.below());
        Block block = below.getBlock();
        if (block == ModBlocks.RBMK_DEBRIS.get() || block instanceof com.hbm_m.block.machines.rbmk.RBMKDebrisBurningBlock
                || block instanceof com.hbm_m.block.machines.rbmk.RBMKDebrisRadiatingBlock || block instanceof com.hbm_m.block.machines.rbmk.RBMKDebrisDigammaBlock
                || block instanceof com.hbm_m.block.machines.MachineZirnoxDestroyedBlock)
            return true;
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE) return false;
        if (block instanceof LeavesBlock) return true;
        if (block == this && below.getValue(LAYERS) == 8) return true;
        return below.isSolidRender(world, pos.below()) && below.blocksMotion();
    }

    /** updateTick im Original auskommentiert: kein Schmelzen. */
    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) { }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        if (this == ModBlocks.LEAVES_LAYER.get() || this == ModBlocks.FOAM_LAYER.get()) return true;
        if (ctx.getItemInHand().is(this.asItem()) && state.getValue(LAYERS) < 8) return super.canBeReplaced(state, ctx);
        return state.getValue(LAYERS) < 8 && ctx.getItemInHand().isEmpty();
    }
}
