package com.hbm_m.block.decorations;

import com.hbm_m.blockentity.decorations.PedestalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * Порт {@code BlockPedestal} (1.7.10) — постамент с парящим предметом.
 * ПКМ пустой рукой — взять предмет, ПКМ с предметом — выставить его.
 */
public class PedestalBlock extends BaseEntityBlock {

    public PedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return swap(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        return swap(level, pos, player);
    }
    *///?}

    private static InteractionResult swap(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();
        ItemStack current = pedestal.getItem();
        if (current.isEmpty() && !held.isEmpty()) {
            if (!level.isClientSide) {
                pedestal.setItem(held.copy());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else if (!current.isEmpty() && held.isEmpty()) {
            if (!level.isClientSide) {
                player.getInventory().placeItemBackInInventory(current.copy());
                pedestal.setItem(ItemStack.EMPTY);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PedestalBlock> CODEC = simpleCodec(PedestalBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PedestalBlockEntity pedestal && !pedestal.getItem().isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        pedestal.getItem().copy()));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
