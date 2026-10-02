package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockBiomeStone} ({@code stone_biome}, Metadaten DESERT/WOODLAND als
 * {@code stone_biome_desert}/{@code stone_biome_woodland}): oben/unten das Deckbild, an den Seiten die Schichtkante
 * ({@code _layer}), ausser es liegt derselbe Stein darueber - dann das normale Seitenbild ({@link #COVERED}).
 */
public class BlockBiomeStone extends Block {

    public static final BooleanProperty COVERED = BooleanProperty.create("covered");

    public BlockBiomeStone(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(COVERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COVERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(COVERED, ctx.getLevel().getBlockState(ctx.getClickedPos().above()).is(this));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (dir == Direction.UP) return state.setValue(COVERED, neighbor.is(this));
        return state;
    }
}
