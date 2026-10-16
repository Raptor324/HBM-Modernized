package com.hbm_m.block.network;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.network.PylonBaseBlockEntity;
import com.hbm_m.blockentity.network.RedConnectorBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт ConnectorRedWire / ConnectorRedWireSuper (1.7.10).
 * FACING — сторона, которой коннектор прижат к опоре/машине.
 */
public class RedConnectorBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final Supplier<? extends BlockEntityType<? extends PylonBaseBlockEntity>> type;
    private final int maxRange;
    //? if > 1.20.1 {
    /*private final com.mojang.serialization.MapCodec<RedConnectorBlock> codec;
    *///?}

    public RedConnectorBlock(Properties properties, Supplier<? extends BlockEntityType<? extends PylonBaseBlockEntity>> type, int maxRange) {
        super(properties);
        this.type = type;
        this.maxRange = maxRange;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));
        //? if > 1.20.1 {
        /*this.codec = simpleCodec(p -> new RedConnectorBlock(p, type, maxRange));
        *///?}
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    //? if > 1.20.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return codec; }
    *///?}

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // В оригинале meta = стороне, по которой кликнули; FACING указывает на опору.
        return this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /** Куб 6/16..10/16, вытянутый к опоре (setBlockBounds из оригинала). */
    private static VoxelShape shape(Direction facing) {
        double min = 5.0 / 16.0, max = 11.0 / 16.0;
        return switch (facing) {
            case DOWN -> Block.box(min, 0, min, max, min, max);
            case UP -> Block.box(min, max, min, max, 16, max);
            case NORTH -> Block.box(min, min, 0, max, max, min);
            case SOUTH -> Block.box(min, min, max, max, max, 16);
            case WEST -> Block.box(0, min, min, min, max, max);
            case EAST -> Block.box(max, min, min, 16, max, max);
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction dir = state.getValue(FACING);
        return level.getBlockState(pos.relative(dir)).isSolidRender(level, pos.relative(dir));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (facing == state.getValue(FACING) && !state.canSurvive(level, currentPos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof PylonBaseBlockEntity pylon) {
                PylonBaseBlockEntity.tick(lvl, pos, st, pylon);
            }
        };
    }

    private void addTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.hbm_m.connection_type")
                .append(Component.translatable("tooltip.hbm_m.connection_single").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.hbm_m.connection_range")
                .append(Component.literal(maxRange + "m").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        addTooltip(tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addTooltip(tooltip);
    }
    *///?}
}
