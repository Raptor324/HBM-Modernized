package com.hbm_m.api.energy;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

public class ConverterBlock extends BaseEntityBlock {

    public ConverterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    // --- ВЗАИМОДЕЙСТВИЕ ---
    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    private InteractionResult hbmOnUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() == ModItems.SCREWDRIVER.get() || stack.getItem() == ModItems.SCREWDRIVER_DESH.get()) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ConverterBlockEntity converter) {

                    if (player.isShiftKeyDown()) {
                        converter.cycleMode();
                        player.sendSystemMessage(Component.literal("§b[Converter] §fMode: §e" + converter.getModeName()));
                    } else {
                        converter.cycleLimit();
                        long limit = converter.getCurrentLimit();
                        String limitText = (limit == Integer.MAX_VALUE) ? "MAX" : String.format("%,d", limit);
                        player.sendSystemMessage(Component.literal("§e[Converter] §fTransfer Rate: §a" + limitText + " HE/t"));
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        //? if < 1.21.1 {
        return super.use(state, level, pos, player, hand, hit);
        //?} else {
        /*return InteractionResult.PASS;
        *///?}
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConverterBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.CONVERTER_BE.get(), ConverterBlockEntity::serverTick);
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<ConverterBlock> CODEC = simpleCodec(ConverterBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}