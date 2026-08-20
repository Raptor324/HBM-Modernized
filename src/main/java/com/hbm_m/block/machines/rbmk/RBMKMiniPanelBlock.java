package com.hbm_m.block.machines.rbmk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1 port of the original's {@code com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase} - the shared
 * base for all 8 RBMK control-room panels (gauge, indicator, lever, numitron, graph, terminal,
 * keypad and the two display blocks).
 *
 * <p>These are <em>not</em> reactor columns: the original is a single wall-mounted block, three
 * quarters of a block deep, whose remaining quarter is the recessed face the devices are mounted
 * on. This port previously derived them from {@link RBMKColumnBlock}, which made every panel a
 * full four-block column with no facing at all - so the panel renderers, which position each unit
 * relative to that recessed face, had nothing correct to anchor to.</p>
 *
 * <p>The original stores the facing in the block metadata as a {@code ForgeDirection} ordinal
 * (NORTH=2, SOUTH=3, WEST=4, EAST=5, see {@code lib/Library}); {@link #FACING} holds the same
 * direction here - the way the panel's face points. The bounds below are
 * {@code setBlockBoundsBasedOnState} converted to sixteenths:</p>
 *
 * <pre>
 *   POS_X / EAST   meta 5   0,0,0 -> 12,16,16
 *   POS_Z / SOUTH  meta 3   0,0,0 -> 16,16,12
 *   NEG_X / WEST   meta 4   4,0,0 -> 16,16,16
 *   NEG_Z / NORTH  meta 2   0,0,4 -> 16,16,16
 * </pre>
 *
 * <p>Placement matches {@code onBlockPlacedBy}, which maps the player's yaw quadrant 0/1/2/3 to
 * meta 2/5/3/4 - i.e. the face always turns towards the player, the same result vanilla gets from
 * {@code getHorizontalDirection().getOpposite()}.</p>
 */
public abstract class RBMKMiniPanelBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE_EAST  = Block.box(0,  0, 0, 12, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0,  0, 0, 16, 16, 12);
    private static final VoxelShape SHAPE_WEST  = Block.box(4,  0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_NORTH = Block.box(0,  0, 4, 16, 16, 16);

    protected RBMKMiniPanelBlock(Properties props) {
        super(props);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case EAST  -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    /**
     * The block body comes from the (facing-rotated) block model, while the devices mounted on its
     * face are drawn by the block entity renderer - the original splits the same way, with an
     * ISBRH box for the body plus a TESR for the units.
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override public boolean useShapeForLightOcclusion(BlockState state) { return true; }
    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
}
