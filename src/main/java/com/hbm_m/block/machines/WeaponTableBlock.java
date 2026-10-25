package com.hbm_m.block.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.inventory.menu.WeaponTableMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code com.hbm.blocks.machine.BlockWeaponTable}: Waffenmodifikationstisch ohne BlockEntity.
 * Texturen gun_table_top/bottom/side (Blockmodell cube_bottom_top).
 */
public class WeaponTableBlock extends Block {

    public WeaponTableBlock(Properties properties) {
        super(properties);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        return onBlockActivated(world, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        return onBlockActivated(world, pos, player);
    }
    *///?}

    private InteractionResult onBlockActivated(Level world, BlockPos pos, Player player) {

        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new WeaponTableMenu(id, inv),
                    Component.translatable("container.weaponsTable")));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
