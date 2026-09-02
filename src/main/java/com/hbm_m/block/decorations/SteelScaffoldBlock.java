package com.hbm_m.block.decorations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт {@code BlockScaffold} (1.7.10): стальные леса — панель 2/16..14/16,
 * ориентированная по оси: axis=y лежит горизонтально (пол/потолок),
 * axis=x/z — вертикальная стена. Оригинальный OBJ: {@code models/block/scaffold.obj}.
 */
public class SteelScaffoldBlock extends RotatedPillarBlock {

    private static final double MIN = 2.0 / 16.0;
    private static final double MAX = 14.0 / 16.0;

    private static final VoxelShape SHAPE_X = Shapes.box(MIN, 0, 0, MAX, 1, 1);
    private static final VoxelShape SHAPE_Y = Shapes.box(0, MIN, 0, 1, MAX, 1);
    private static final VoxelShape SHAPE_Z = Shapes.box(0, 0, MIN, 1, 1, MAX);

    public SteelScaffoldBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }
}
