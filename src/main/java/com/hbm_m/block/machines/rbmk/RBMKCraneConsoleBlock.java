package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.RBMKCraneConsoleBlockEntity;
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

public class RBMKCraneConsoleBlock extends RBMKColumnBlock {

    public RBMKCraneConsoleBlock(Properties props) { super(props); }

    // Crane console has no GUI (it's flown by keybind, not menu) - only the RBMK linking tool
    // and a shift-click rotation cycle are handled here, bypassing RBMKColumnBlock's default
    // "open MenuProvider" behavior entirely.
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RBMKCraneConsoleBlockEntity crane)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof RBMKToolItem) {
            RBMKToolItem.linkCrane(held, level, crane, player);
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            crane.cycleCraneRotation();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKCraneConsoleBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RBMK_CRANE_CONSOLE_BE.get(), RBMKCraneConsoleBlockEntity::tick);
    }
}
