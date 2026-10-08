package com.hbm_m.block.decorations;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.decorations.DecoBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 1:1 {@code DecoBlock} mit {@code TileEntityDecoBlock} und {@code RenderDecoBlock}: {@code boxcar} (Gueterwagen)
 * und {@code boat} (Duchess Gambit). Ein Block, das grosse Modell zeichnet der Blockentity-Renderer.
 * {@code FACING} = Meta: beim Setzen die Blickrichtung ({@code onBlockPlacedBy}), Standardzustand {@code UP} steht fuer
 * Meta 0, die {@code EntityBoxcar}/{@code EntityDuchessGambit} beim Aufschlag hinterlassen (Wagen steht dann senkrecht).
 */
public class DecoModelTEBlock extends BaseEntityBlock {

    public enum Kind { BOXCAR, BOAT }

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public final Kind kind;

    public DecoModelTEBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecoBlockEntity(pos, state);
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<DecoModelTEBlock> CODEC = com.hbm_m.platform.BlockCodecs.unsupported(DecoModelTEBlock.class);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
