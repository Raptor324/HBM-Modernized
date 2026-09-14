package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.RadioboxBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Port of {@code Radiobox} (1.7.10 Original). */
public class RadioboxBlock extends BaseEntityBlock {

    public RadioboxBlock(Properties properties) { super(properties); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioboxBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RADIOBOX_BE.get(),
                (lvl, pos, st, be) -> RadioboxBlockEntity.tick(lvl, pos, st, (RadioboxBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof RadioboxBlockEntity be)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == ModItems.BATTERY_SPARK.get()) {
            if (be.activateInfinite(held)) {
                player.displayClientMessage(Component.literal("Infinite power activated"), true);
            }
            return InteractionResult.CONSUME;
        }

        be.toggleOn(player);
        player.displayClientMessage(Component.literal(be.isOn() ? "On" : "Off"), true);
        return InteractionResult.CONSUME;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof RadioboxBlockEntity be)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() == ModItems.BATTERY_SPARK.get()) {
            if (be.activateInfinite(held)) {
                player.displayClientMessage(Component.literal("Infinite power activated"), true);
            }
            return InteractionResult.CONSUME;
        }

        be.toggleOn(player);
        player.displayClientMessage(Component.literal(be.isOn() ? "On" : "Off"), true);
        return InteractionResult.CONSUME;
        }
    *///?}


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RadioboxBlock> CODEC = simpleCodec(RadioboxBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
