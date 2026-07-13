package com.hbm_m.block.machines;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.blockentity.machines.MachineFoundryOutletBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Port of the 1.7.10 FoundryOutlet block.
 * FACING = pour direction; the spout body is attached to the opposite side
 * (where the channel connects). Original bounds: 6px wide, 8px high, 6px deep.
 */
public class MachineFoundryOutletBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Original AABBs: e.g. pour-north → body at the south side (z 10..16)
    private static final Map<Direction, VoxelShape> SHAPES = ImmutableMap.of(
            Direction.NORTH, Block.box(5, 0, 10, 11, 8, 16),
            Direction.SOUTH, Block.box(5, 0,  0, 11, 8,  6),
            Direction.EAST,  Block.box(0, 0,  5,  6, 8, 11),
            Direction.WEST,  Block.box(10, 0, 5, 16, 8, 11)
    );

    public MachineFoundryOutletBlock(Properties props) {
        super(props);
        this.registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
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
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPES.get(s.getValue(FACING));
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    /** Original: right-click (no sneak) toggles the redstone inversion. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineFoundryOutletBlockEntity outlet)) return InteractionResult.PASS;

        outlet.invertRedstone = !outlet.invertRedstone;
        outlet.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        player.displayClientMessage(Component.literal(outlet.invertRedstone
                ? "Outlet: closed by default, opens with redstone"
                : "Outlet: open by default, closes with redstone"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFoundryOutletBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
