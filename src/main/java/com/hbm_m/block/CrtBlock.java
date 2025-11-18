package com.hbm_m.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CrtBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Реальный маленький хитбокс тостера (в пикселях 0-16)
    private static final VoxelShape SHAPE_NORTH = Block.box(2, 0, 3, 14, 12, 13);   // 12×12×10
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 0, 3, 14, 12, 13);
    private static final VoxelShape SHAPE_EAST  = Block.box(3, 0, 2, 13, 12, 14);
    private static final VoxelShape SHAPE_WEST  = Block.box(3, 0, 2, 13, 12, 14);
    // Зона, по которой можно кликать (ломать и открывать GUI) — почти полный блок
    private static final VoxelShape INTERACTION_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public CrtBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // Коллизия и визуальный хитбокс (F3+B) — маленький тостер
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx); // то же самое, что и визуальный
    }

    // Зона клика — большой, чтобы ломался без танцев с бубном
    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INTERACTION_SHAPE;
    }

    // Чтобы свет проходил и не было тёмных углов
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    // FACING
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    // Если у тебя будет BlockEntity и GUI — раскомменти и допиши
    /*
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ToasterBlockEntity toaster) {
                NetworkHooks.openScreen((ServerPlayer) player, toaster, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    */
}