package com.hbm_m.block.decorations;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.decorations.SkeletonHolderBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockSkeletonHolder} ({@code skeleton_holder}, "Oh, that's a dead guy"): Skelett, das einen Gegenstand
 * haelt. Mit leerer Hand nehmen, mit Gegenstand hineinlegen; beim Abbau faellt der Gegenstand heraus. Die Ausrichtung
 * ist die Blickrichtung um 90 Grad gegen den Uhrzeigersinn gedreht (Blick S -> Meta 5 Ost usw.).
 */
public class SkeletonHolderBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SkeletonHolderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getCounterClockWise());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SkeletonHolderBlockEntity(pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return swap(level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return swap(level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private static InteractionResult swap(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof SkeletonHolderBlockEntity holder)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (holder.getItem().isEmpty() && !held.isEmpty()) {
            if (!level.isClientSide) {
                holder.setItem(held.copy());
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else if (!holder.getItem().isEmpty() && held.isEmpty()) {
            if (!level.isClientSide) {
                player.setItemInHand(hand, holder.getItem().copy());
                holder.setItem(ItemStack.EMPTY);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof SkeletonHolderBlockEntity holder && !holder.getItem().isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, holder.getItem().copy()));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<SkeletonHolderBlock> CODEC = simpleCodec(SkeletonHolderBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
