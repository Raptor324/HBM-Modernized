package com.hbm_m.block.decorations;

import com.hbm_m.api.block.IToolable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code DecoBlock} als {@code steel_wall}: Wandplatte 2px. {@code FACING} ist die Gegenrichtung der Meta
 * (Meta = Blickrichtung beim Setzen, die Platte liegt an der Seite zum Spieler). Schraubenzieher dreht wie
 * {@code onScrew}: normal S->W->N->O, geduckt umgekehrt.
 */
public class SteelWallBlock extends Block implements IToolable {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    private static final double T = 2.0 / 16.0;

    private static final VoxelShape SHAPE_NORTH = Shapes.box(0, 0, 0, 1, 1, T);
    private static final VoxelShape SHAPE_SOUTH = Shapes.box(0, 0, 1 - T, 1, 1, 1);
    private static final VoxelShape SHAPE_WEST = Shapes.box(0, 0, 0, T, 1, 1);
    private static final VoxelShape SHAPE_EAST = Shapes.box(1 - T, 0, 0, 1, 1, 1);

    public SteelWallBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    protected VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        BlockState state = world.getBlockState(pos);
        Direction f = state.getValue(FACING);
        world.setBlock(pos, state.setValue(FACING, player.isShiftKeyDown() ? f.getCounterClockWise() : f.getClockWise()), 3);
        return true;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
