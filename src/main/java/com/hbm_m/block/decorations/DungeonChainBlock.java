package com.hbm_m.block.decorations;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code BlockChain} ({@code dungeon_chain}, "Metal Chain"): haengt an der Decke ({@code FACING=up}, Meta 0,
 * gekreuzte Flaechen) oder an einer Wand (Meta 2-5, flach 0.05 vor der Wand). Leiter, ohne Kollision. {@code END}
 * ist das untere Kettenende (Textur {@code chain_end}, Auswahlbox ab 0.25), wenn darunter weder eine tragende
 * Oberseite noch eine gleich ausgerichtete Kette ist.
 */
public class DungeonChainBlock extends Block {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", d -> d != Direction.DOWN);
    public static final BooleanProperty END = BooleanProperty.create("end");

    private static final double F = 0.125;

    public DungeonChainBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(END, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, END);
    }

    /** Die Wand, an der eine Kette mit dieser Ausrichtung haengt, liegt entgegen {@code facing}. */
    private static boolean wallSolid(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    private static boolean ceilingSolid(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        LevelReader level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction side = ctx.getClickedFace();
        Direction f = Direction.UP;

        if (side.getAxis().isHorizontal() && wallSolid(level, pos, side)) f = side;

        if (f == Direction.UP) {
            BlockState above = level.getBlockState(pos.above());
            if (above.is(this)) return withEnd(level, pos, defaultBlockState().setValue(FACING, above.getValue(FACING)));
            if (ceilingSolid(level, pos)) return withEnd(level, pos, defaultBlockState());
            for (Direction d : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST }) {
                if (wallSolid(level, pos, d)) f = d;
            }
        }

        BlockState state = defaultBlockState().setValue(FACING, f);
        return canSurvive(state, level, pos) ? withEnd(level, pos, state) : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction f = state.getValue(FACING);
        BlockState above = level.getBlockState(pos.above());
        if (above.is(this) && above.getValue(FACING) == f) return true;
        if (f == Direction.UP) return ceilingSolid(level, pos);
        return wallSolid(level, pos, f);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return withEnd(level, pos, state);
    }

    private BlockState withEnd(LevelReader level, BlockPos pos, BlockState state) {
        BlockState below = level.getBlockState(pos.below());
        boolean supported = below.isFaceSturdy(level, pos.below(), Direction.UP)
                || (below.is(this) && below.getValue(FACING) == state.getValue(FACING));
        return state.setValue(END, !supported);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        double minY = state.getValue(END) ? 0.25 : 0.0;
        return switch (state.getValue(FACING)) {
            case NORTH -> Shapes.box(3 * F, minY, 1.0 - F, 5 * F, 1.0, 1.0);
            case SOUTH -> Shapes.box(3 * F, minY, 0.0, 5 * F, 1.0, F);
            case WEST -> Shapes.box(1.0 - F, minY, 3 * F, 1.0, 1.0, 5 * F);
            case EAST -> Shapes.box(0.0, minY, 3 * F, F, 1.0, 5 * F);
            default -> Shapes.box(3 * F, minY, 3 * F, 5 * F, 1.0, 5 * F);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        Direction f = state.getValue(FACING);
        return f == Direction.UP ? state : state.setValue(FACING, rotation.rotate(f));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction f = state.getValue(FACING);
        return f == Direction.UP ? state : state.rotate(mirror.getRotation(f));
    }
}
