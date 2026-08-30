package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.RBMKDisplayBlockEntity;
import com.hbm_m.item.rbmk.RBMKToolItem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1 port of {@code RBMKDisplay}: a mini-panel showing a 7x7 slice of the reactor. Its only
 * interaction is the RBMK linking tool, which sets the center of the scanned area - matching the
 * original's {@code ItemRBMKTool} display branch.
 */
public class RBMKDisplayBlock extends RBMKMiniPanelBlock {

    public RBMKDisplayBlock(Properties props) { super(props); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKDisplayBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, (BlockEntityType<T>) ModBlockEntities.RBMK_DISPLAY_BE.get(),
                (lvl, pos, st, be) -> { if (be instanceof RBMKDisplayBlockEntity d) d.tickPanel(lvl, pos); });
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        // RBMKDisplay.onScrew - same quarter-turn as the console.
        if (held.getItem() instanceof com.hbm_m.item.tools_and_armor.ScrewdriverItem
                && level.getBlockEntity(pos) instanceof RBMKDisplayBlockEntity rotatable) {
            if (!level.isClientSide) rotatable.rotate();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (held.getItem() instanceof RBMKToolItem
                && level.getBlockEntity(pos) instanceof RBMKDisplayBlockEntity display) {
            RBMKToolItem.linkDisplay(held, level, display, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RBMKDisplayBlock> CODEC = simpleCodec(RBMKDisplayBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
