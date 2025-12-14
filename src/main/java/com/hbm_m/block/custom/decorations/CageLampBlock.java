package com.hbm_m.block.custom.decorations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CageLampBlock extends Block {

    public static final DirectionProperty FACING = DirectionProperty.create("facing");

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1, 1);

    public CageLampBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.UP || clickedFace == Direction.DOWN) {
            // Для пола и потолка можно ориентировать блок в сторону, куда смотрит игрок (обратно взгляду)
            Direction horizontalFacing = context.getHorizontalDirection().getOpposite();
            return this.defaultBlockState().setValue(FACING, clickedFace).setValue(FACING, clickedFace);
        } else {
            // Для стен - блок смотрит в противоположную сторону взгляда игрока (или в сторону стены)
            return this.defaultBlockState().setValue(FACING, clickedFace);
        }
    }

    // Отключаем коллизию, возвращая пустой хитбокс
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
