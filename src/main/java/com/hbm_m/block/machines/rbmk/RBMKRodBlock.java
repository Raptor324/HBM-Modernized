package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RBMKRodBlock extends RBMKColumnBlock {

    /** Whether this fuel channel has a built-in graphite moderator. */
    public final boolean moderated;

    public RBMKRodBlock(boolean moderated, Properties props) {
        super(props);
        this.moderated = moderated;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        RBMKRodBlockEntity be = new RBMKRodBlockEntity(pos, state);
        be.moderated = moderated;
        return be;
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RBMK_ROD_BE.get(), RBMKRodBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    private InteractionResult hbmOnUse(BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RBMKRodBlockEntity rod)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Insert fuel rod by right-clicking with one
        if (!held.isEmpty() && held.getItem() instanceof RBMKRodItem && rod.fuelSlot.isEmpty()) {
            rod.fuelSlot = held.copy();
            rod.fuelSlot.setCount(1);
            if (!player.isCreative()) held.shrink(1);
            rod.setChanged();
            level.playSound(player, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
            return InteractionResult.SUCCESS;
        }

        //? if < 1.21.1 {
        return super.use(state, level, pos, player, hand, hit);
        //?} else {
        /*return InteractionResult.PASS;
        *///?}
    }

    @Override
    protected void onColumnRemoved(RBMKColumnBlockEntity col, Level level, BlockPos pos) {
        if (!level.isClientSide && col instanceof RBMKRodBlockEntity rod && !rod.fuelSlot.isEmpty()) {
            boolean meltdown = rod.explodeOnBroken
                    && !RBMKDials.getMeltdownsDisabled(level)
                    && rod.fuelSlot.getItem() instanceof RBMKRodItem
                    && RBMKRodItem.getHullHeat(rod.fuelSlot) >= rod.maxHeat();
            if (meltdown) {
                rod.onMelt(level, 1);
            } else {
                Block.popResource(level, pos, rod.fuelSlot);
            }
        }
        super.onColumnRemoved(col, level, pos);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RBMKRodBlock> CODEC = simpleCodec(props -> new RBMKRodBlock(false, props));

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
