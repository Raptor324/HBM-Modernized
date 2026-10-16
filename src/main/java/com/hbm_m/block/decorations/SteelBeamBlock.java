package com.hbm_m.block.decorations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт стальной балки ({@code DecoBlock}/steel_beam, 1.7.10): вертикальная
 * колонна сечением 2x2 px ({@code setBlockBounds(7f, 0, 7f, 9f, 1, 9f)}),
 * ориентируется по оси (в оригинале всегда вертикальна — OBJ без поворота).
 */
public class SteelBeamBlock extends RotatedPillarBlock {

    private static final double MIN = 7.0 / 16.0;
    private static final double MAX = 9.0 / 16.0;

    private static final VoxelShape SHAPE_Y = Shapes.box(MIN, 0, MIN, MAX, 1, MAX);
    private static final VoxelShape SHAPE_X = Shapes.box(0, MIN, MIN, 1, MAX, MAX);
    private static final VoxelShape SHAPE_Z = Shapes.box(MIN, MIN, 0, MAX, MAX, 1);

    public SteelBeamBlock(Properties props) {
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
