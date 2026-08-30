package com.hbm_m.block.decorations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.hbm_m.item.ModItems;

/**
 * Порт {@code steel_wall} (DecoBlock, 1.7.10): тонкая стальная панель 2/16,
 * прижатая к стороне {@code FACING}. Хитбокс вращается вместе с моделью
 * (в оригинале — setBlockBoundsBasedOnState с meta 2=south, 3=north, 4=east, 5=west).
 * Отвёрткой панель циклически поворачивается (аналог onScrew из оригинала).
 */
public class SteelWallBlock extends Block {

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
        // панель прижимается к стороне, смотрящей на игрока (как meta 2-5 по yaw в оригинале)
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    /** Отвёртка циклически поворачивает панель (N→E→S→W), как onScrew оригинала. */
    private InteractionResult hbmOnUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!isScrewdriver(player.getItemInHand(hand))) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean isScrewdriver(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() == ModItems.SCREWDRIVER.get()
                || stack.getItem() == ModItems.SCREWDRIVER_DESH.get();
    }
}
