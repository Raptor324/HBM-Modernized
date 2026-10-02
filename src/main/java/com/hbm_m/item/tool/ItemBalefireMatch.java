package com.hbm_m.item.tool;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.tool.ItemBalefireMatch}: wie Feuerzeug, setzt Balefire (256 Haltbarkeit). */
public class ItemBalefireMatch extends Item {

    public ItemBalefireMatch(Properties properties) {
        super(properties.durability(256));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());

        if (player != null && !player.mayUseItemAt(pos, ctx.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        } else {
            if (world.isEmptyBlock(pos)) {
                world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, world.random.nextFloat() * 0.4F + 0.8F);
                if (!world.isClientSide) world.setBlockAndUpdate(pos, ModBlocks.BALEFIRE.get().defaultBlockState());
            }
            if (player != null) stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }
}
