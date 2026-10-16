package com.hbm_m.block.machines;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFoundryChannelBlockEntity;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.util.CrucibleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MachineFoundryChannelBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;

    private static final Map<Direction, BooleanProperty> PROP = ImmutableMap.of(
            Direction.NORTH, NORTH, Direction.EAST, EAST,
            Direction.SOUTH, SOUTH, Direction.WEST, WEST);

    private static final VoxelShape CENTER = box(5, 0, 5, 11, 2, 11);
    private static final Map<Direction, VoxelShape> CONNECTED = ImmutableMap.of(
            Direction.EAST,  Shapes.or(box(10,0,5,16,2,11), box(10,0,5,16,8,6), box(10,0,10,16,8,11)),
            Direction.WEST,  Shapes.or(box(0,0,5,6,2,11),   box(0,0,5,6,8,6),   box(0,0,10,6,8,11)),
            Direction.SOUTH, Shapes.or(box(5,0,10,11,2,16), box(5,0,10,6,8,16),  box(10,0,10,11,8,16)),
            Direction.NORTH, Shapes.or(box(5,0,0,11,2,6),   box(5,0,0,6,8,6),    box(10,0,0,11,8,6)));
    private static final Map<Direction, VoxelShape> CLOSED = ImmutableMap.of(
            Direction.EAST,  box(10,0,5,11,8,11), Direction.WEST,  box(5,0,5,6,8,11),
            Direction.SOUTH, box(5,0,10,11,8,11), Direction.NORTH, box(5,0,5,11,8,6));

    public MachineFoundryChannelBlock(Properties props) {
        super(props);
        this.registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(NORTH, EAST, SOUTH, WEST); }

    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) { return connectionState(ctx.getLevel(), ctx.getClickedPos()); }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState nb, LevelAccessor level, BlockPos pos, BlockPos nbPos) {
        return dir.getAxis().isHorizontal() ? connectionState(level, pos) : state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        BlockState next = connectionState(level, pos);
        if (!next.equals(state)) level.setBlock(pos, next, UPDATE_CLIENTS);
    }

    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return buildShape(s); }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return buildShape(s); }
    @Override public VoxelShape getOcclusionShape(BlockState s, BlockGetter l, BlockPos p) { return buildShape(s); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    private BlockState connectionState(LevelAccessor level, BlockPos pos) {
        return defaultBlockState()
                .setValue(NORTH, canConnect(level, pos, Direction.NORTH))
                .setValue(EAST,  canConnect(level, pos, Direction.EAST))
                .setValue(SOUTH, canConnect(level, pos, Direction.SOUTH))
                .setValue(WEST,  canConnect(level, pos, Direction.WEST));
    }

    /**
     * 1:1 порт FoundryChannel.canConnectTo: канал соединяется только с foundry_channel,
     * foundry_mold и foundry_outlet/foundry_slagtap. В оригинале условие для
     * outlet/slagtap — {@code meta == dir.ordinal()}, где dir — направление от канала
     * к соседу; в порту FACING аутлета играет роль этого meta, поэтому FACING == dir.
     * Бассейн (basin) и тигель в наборе НЕ числятся (оригинальное поведение).
     */
    private boolean canConnect(LevelAccessor level, BlockPos pos, Direction dir) {
        if (dir.getAxis().isVertical()) return false;
        BlockPos nb = pos.relative(dir);
        BlockState nbState = level.getBlockState(nb);
        Block b = nbState.getBlock();
        if (b instanceof MachineFoundryOutletBlock) { // включает foundry_slagtap
            return nbState.hasProperty(MachineFoundryOutletBlock.FACING)
                    && nbState.getValue(MachineFoundryOutletBlock.FACING) == dir;
        }
        return b instanceof MachineFoundryChannelBlock
                || b instanceof MachineFoundryMoldBlock;
    }

    private VoxelShape buildShape(BlockState s) {
        VoxelShape shape = CENTER;
        for (Direction d : PROP.keySet())
            shape = Shapes.or(shape, s.getValue(PROP.get(d)) ? CONNECTED.get(d) : CLOSED.get(d));
        return shape;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFoundryChannelBlockEntity(pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        return handleUse(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleUse(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    /**
     * Оригинал FoundryChannel.onBlockActivated: только совок — вычерпать расплав
     * в шлак-предметы; прочие клики игнорируются. Оверлея у канала в оригинале нет.
     */
    private InteractionResult handleUse(BlockState state, Level level, BlockPos pos,
                                        Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineFoundryChannelBlockEntity channel)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof ShovelItem)) return InteractionResult.PASS;

        if (channel.amount > 0 && channel.type != null) {
            ItemStack scrap = CrucibleUtil.createScrap(new MaterialStack(channel.type, channel.amount));
            if (!scrap.isEmpty()) {
                if (!player.addItem(scrap)) {
                    level.addFreshEntity(new ItemEntity(level,
                            pos.getX() + 0.5, pos.getY() + 0.5F, pos.getZ() + 0.5, scrap));
                }
            }
            channel.amount = 0;
            channel.type = null;
            channel.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
        return InteractionResult.SUCCESS;
    }

    /** Оригинал FoundryChannel.breakBlock: шлак остатка расплава. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineFoundryChannelBlockEntity channel
                && channel.amount > 0 && channel.type != null) {
            ItemStack scrap = CrucibleUtil.createScrap(new MaterialStack(channel.type, channel.amount));
            if (!scrap.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 0.5F, pos.getZ() + 0.5, scrap));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FOUNDRY_CHANNEL_BE.get(),
                MachineFoundryChannelBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFoundryChannelBlock> CODEC = simpleCodec(MachineFoundryChannelBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
