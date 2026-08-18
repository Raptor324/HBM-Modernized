package com.hbm_m.block.generic;

import com.hbm_m.powerarmor.ModArmorFSBPowered;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * Port of {@code HEVBattery} (1.7.10 Original) - a single-use charging pad, not a chargeable
 * item/block. Right-clicking while wearing FSB-powered armor tops up every {@code ModArmorFSBPowered}
 * piece worn by +150,000 charge (capped to its max), then the block deletes itself.
 */
public class HevBatteryBlock extends Block {

    private static final long CHARGE_AMOUNT = 150_000L;
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 6, 10);

    public HevBatteryBlock(Properties properties) { super(properties); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean charged = false;
        for (ItemStack armorStack : player.getInventory().armor) {
            if (armorStack.getItem() instanceof ModArmorFSBPowered fsb) {
                fsb.chargeBattery(armorStack, CHARGE_AMOUNT);
                charged = true;
            }
        }

        if (charged) {
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.removeBlock(pos, false);
        }

        return InteractionResult.CONSUME;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean charged = false;
        for (ItemStack armorStack : player.getInventory().armor) {
            if (armorStack.getItem() instanceof ModArmorFSBPowered fsb) {
                fsb.chargeBattery(armorStack, CHARGE_AMOUNT);
                charged = true;
            }
        }

        if (charged) {
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.removeBlock(pos, false);
        }

        return InteractionResult.CONSUME;
        }
    *///?}

}
