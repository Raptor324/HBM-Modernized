package com.hbm_m.block.generic;

import com.hbm_m.blockentity.generic.ObjTesterBlockEntity;

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
 * 1:1 {@code TestObjTester} (obj_tester): Testblock, nur vom Blockentity-Renderer gezeichnet (Pony mit Horn).
 * Ausrichtung wie Original-Meta 5/3/4/2 beim Setzen (wird vom Renderer nicht gelesen).
 */
public class TestObjTesterBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TestObjTesterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Original: i 0/1/2/3 -> Meta 5 (Ost) / 3 (Sued) / 4 (West) / 2 (Nord). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        int i = net.minecraft.util.Mth.floor(ctx.getRotation() * 4.0F / 360.0F + 0.5D) & 3;
        Direction d = switch (i) {
            case 0 -> Direction.EAST;
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            default -> Direction.NORTH;
        };
        return defaultBlockState().setValue(FACING, d);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ObjTesterBlockEntity(pos, state);
    }
}
