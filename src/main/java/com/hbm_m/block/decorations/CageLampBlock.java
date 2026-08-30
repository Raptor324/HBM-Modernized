package com.hbm_m.block.decorations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт {@code Spotlight} (1.7.10, spotlight_incandescent / halogen).
 * Лампа смотрит в сторону {@code FACING} и крепится к блоку с противоположной
 * стороны. Хитбокс — маленькая коробка у опорной грани (реплицирует
 * {@code setBlockBoundsBasedOnState} оригинала: полуразмеры 0.25/0.2/0.15
 * для cage lamp, 0.35/0.25/0.2 для flood lamp).
 */
public class CageLampBlock extends Block {

    public static final DirectionProperty FACING = DirectionProperty.create("facing");

    /** Полуразмеры {x, y, z} в базовой ориентации (как getBounds() оригинала). */
    private final float hx, hy, hz;

    public CageLampBlock(Properties props, float hx, float hy, float hz) {
        super(props);
        this.hx = hx;
        this.hy = hy;
        this.hz = hz;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // Оригинал: offset = 0.5 - dir * (0.5 - bounds), коробка центрирована в offset
        Direction dir = state.getValue(FACING);
        float dx = hx, dy = hy, dz = hz;
        switch (dir) {
            case EAST, WEST -> { dx = hz; dz = hx; }
            case UP, DOWN -> { dx = hy; dy = hz; dz = hx; }
            default -> { }
        }
        double cx = 0.5 - dir.getStepX() * (0.5 - dx);
        double cy = 0.5 - dir.getStepY() * (0.5 - dy);
        double cz = 0.5 - dir.getStepZ() * (0.5 - dz);
        return Shapes.box(cx - dx, cy - dy, cz - dz, cx + dx, cy + dy, cz + dz);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    // Оригинал: лампа осыпается, если опорная грань перестала быть solid (onNeighborBlockChange)
    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction support = state.getValue(FACING).getOpposite();
        if (dir == support && !neighborState.isFaceSturdy(level, neighborPos, support.getOpposite())) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
    }
}
