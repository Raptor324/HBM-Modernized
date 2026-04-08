package com.hbm_m.block.machines;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.entity.machines.FluidDuctBlockEntity;
import com.hbm_m.item.IItemFluidIdentifier;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidDuctItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/**
 * Fluid duct: multipart blockstate + Forge OBJ visibility on {@code pipe_neo.obj}. Fluid type lives in the block entity.
 * <p>
 * {@link PipeRenderShape} follows NEO pipe rules (isolated / single-axis / complex + octants). Arms use corrected mapping
 * (south → {@code pZ}, north → {@code nZ}) vs the swapped render in NEO’s {@code RenderPipe}.
 * <p>
 * Fluid identifier: normal click sets fluid on one duct; <b>sneak (Shift)</b> + click paints the connected network of the
 * same block type (depth-capped). Vanilla does not expose Ctrl on the server; use sneak for recursive mode.
 */
public class FluidDuctBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final EnumProperty<PipeRenderShape> SHAPE =
            EnumProperty.create("shape", PipeRenderShape.class);

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.of(
            Direction.NORTH, NORTH, Direction.SOUTH, SOUTH,
            Direction.WEST, WEST, Direction.EAST, EAST,
            Direction.UP, UP, Direction.DOWN, DOWN);

    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARM_SHAPES = ImmutableMap.of(
            Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4),
            Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16),
            Direction.WEST, Block.box(0, 4, 4, 4, 12, 12),
            Direction.EAST, Block.box(12, 4, 4, 16, 12, 12),
            Direction.UP, Block.box(4, 12, 4, 12, 16, 12),
            Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12));

    private static final int IDENTIFIER_NETWORK_LIMIT = 512;

    private final PipeStyle pipeStyle;

    public FluidDuctBlock(Properties properties, PipeStyle pipeStyle) {
        super(properties);
        this.pipeStyle = pipeStyle;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false)
                .setValue(SHAPE, PipeRenderShape.ISOLATED));
    }

    public PipeStyle getPipeStyle() {
        return pipeStyle;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, SHAPE);
    }

    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
            @Nonnull CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
                shape = Shapes.or(shape, ARM_SHAPES.get(dir));
            }
        }
        return shape;
    }

    /** Full connection + render shape for a duct at {@code pos} (block there must be this block type). */
    public BlockState getConnectionState(LevelAccessor level, BlockPos pos) {
        BlockState self = level.getBlockState(pos);
        Block blk = self.getBlock();
        if (!(blk instanceof FluidDuctBlock duct)) {
            return self;
        }
        boolean north = duct.canConnectTo(level, pos, pos.relative(Direction.NORTH), Direction.NORTH);
        boolean south = duct.canConnectTo(level, pos, pos.relative(Direction.SOUTH), Direction.SOUTH);
        boolean east = duct.canConnectTo(level, pos, pos.relative(Direction.EAST), Direction.EAST);
        boolean west = duct.canConnectTo(level, pos, pos.relative(Direction.WEST), Direction.WEST);
        boolean up = duct.canConnectTo(level, pos, pos.relative(Direction.UP), Direction.UP);
        boolean down = duct.canConnectTo(level, pos, pos.relative(Direction.DOWN), Direction.DOWN);
        PipeRenderShape shape = PipeRenderShape.fromConnections(north, south, east, west, up, down);
        return duct.defaultBlockState()
                .setValue(NORTH, north).setValue(SOUTH, south).setValue(EAST, east)
                .setValue(WEST, west).setValue(UP, up).setValue(DOWN, down)
                .setValue(SHAPE, shape);
    }

    @Override
    public BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction facing,
            @Nonnull BlockState facingState, @Nonnull LevelAccessor level,
            @Nonnull BlockPos currentPos, @Nonnull BlockPos facingPos) {
        BlockState self = level.getBlockState(currentPos);
        if (self.getBlock() instanceof FluidDuctBlock duct) {
            return duct.getConnectionState(level, currentPos);
        }
        return state;
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
            @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        BlockState newState = getConnectionState(level, pos);
        if (!newState.equals(state)) {
            level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        }
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos myPos, BlockPos neighborPos, Direction direction) {
        BlockState neighborState = level.getBlockState(neighborPos);
        Block selfBlock = level.getBlockState(myPos).getBlock();

        if (neighborState.getBlock() == selfBlock && neighborState.getBlock() instanceof FluidDuctBlock) {
            BlockEntity myBe = level.getBlockEntity(myPos);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (myBe instanceof FluidDuctBlockEntity myDuct && neighborBe instanceof FluidDuctBlockEntity neighborDuct) {
                return myDuct.getFluidType() == neighborDuct.getFluidType();
            }
            return true;
        }

        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be != null) {
            return be.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).isPresent();
        }
        return false;
    }

    /** Refresh this duct and same-block neighbors (after fluid / connection change). */
    public static void refreshAdjacentDucts(Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        Block b = st.getBlock();
        if (!(b instanceof FluidDuctBlock duct)) {
            return;
        }
        Set<BlockPos> update = new HashSet<>();
        update.add(pos);
        for (Direction d : Direction.values()) {
            update.add(pos.relative(d));
        }
        for (BlockPos p : update) {
            if (level.getBlockState(p).getBlock() != b) {
                continue;
            }
            BlockState next = duct.getConnectionState(level, p);
            level.setBlock(p, next, Block.UPDATE_CLIENTS);
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof FluidDuctBlockEntity dbe) {
                dbe.syncFluidToClients();
            }
        }
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
            @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof IItemFluidIdentifier idItem)) {
            return InteractionResult.PASS;
        }
        Fluid fluid = idItem.getType(level, pos, stack);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean recursive = player.isShiftKeyDown();
        Block startBlock = state.getBlock();
        if (!(startBlock instanceof FluidDuctBlock)) {
            return InteractionResult.PASS;
        }
        if (recursive) {
            applyIdentifierRecursive(level, pos, fluid, startBlock);
        } else {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluidDuctBlockEntity duct) {
                duct.setFluidType(fluid);
                level.setBlock(pos, getConnectionState(level, pos), Block.UPDATE_CLIENTS);
                refreshAdjacentDucts(level, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void applyIdentifierRecursive(Level level, BlockPos start, Fluid fluid, Block ductBlock) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && visited.size() < IDENTIFIER_NETWORK_LIMIT) {
            BlockPos p = queue.poll();
            BlockState st = level.getBlockState(p);
            if (st.getBlock() != ductBlock) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                if (!st.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
                    continue;
                }
                BlockPos n = p.relative(dir);
                if (visited.contains(n)) {
                    continue;
                }
                BlockState ns = level.getBlockState(n);
                if (ns.getBlock() != ductBlock) {
                    continue;
                }
                if (!ns.getValue(PROPERTY_BY_DIRECTION.get(dir.getOpposite()))) {
                    continue;
                }
                visited.add(n);
                queue.add(n);
            }
        }
        for (BlockPos p : visited) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof FluidDuctBlockEntity duct) {
                duct.setFluidTypeSilent(fluid);
            }
        }
        FluidDuctBlock fd = (FluidDuctBlock) ductBlock;
        for (BlockPos p : visited) {
            level.setBlock(p, fd.getConnectionState(level, p), Block.UPDATE_CLIENTS);
        }
        for (BlockPos p : visited) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof FluidDuctBlockEntity duct) {
                duct.syncFluidToClients();
            }
        }
        // Neighbors outside the BFS may need new connection flags
        Set<BlockPos> border = new HashSet<>();
        for (BlockPos p : visited) {
            for (Direction d : Direction.values()) {
                border.add(p.relative(d));
            }
        }
        for (BlockPos p : border) {
            if (visited.contains(p)) {
                continue;
            }
            if (level.getBlockState(p).getBlock() == ductBlock) {
                level.setBlock(p, fd.getConnectionState(level, p), Block.UPDATE_CLIENTS);
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof FluidDuctBlockEntity dbe) {
                    dbe.syncFluidToClients();
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new FluidDuctBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level,
            @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof FluidDuctBlockEntity duct) {
                FluidDuctBlockEntity.tick(lvl, pos, st, duct);
            }
        };
    }

    @Override
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nonnull
    @Override
    public ItemStack getCloneItemStack(@Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        ItemStack stack = new ItemStack(getDuctItem());
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidDuctBlockEntity ductBe
                && ductBe.getFluidType() != Fluids.EMPTY) {
            FluidDuctItem.setFluidType(stack, ductBe.getFluidType());
        }
        return stack;
    }

    private net.minecraft.world.item.Item getDuctItem() {
        return switch (pipeStyle) {
            case NEO -> ModItems.FLUID_DUCT.get();
            case COLORED -> ModItems.FLUID_DUCT_COLORED.get();
            case SILVER -> ModItems.FLUID_DUCT_SILVER.get();
        };
    }

    @Override
    public List<ItemStack> getDrops(@Nonnull BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack drop = new ItemStack(getDuctItem());
        if (be instanceof FluidDuctBlockEntity ductBe
                && ductBe.getFluidType() != Fluids.EMPTY) {
            FluidDuctItem.setFluidType(drop, ductBe.getFluidType());
        }
        return List.of(drop);
    }
}
