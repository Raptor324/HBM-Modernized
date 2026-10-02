package com.hbm_m.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** 1:1 {@code com.hbm.items.tool.RedstoneSword}: legt beim Rechtsklick Redstone aus (14 Haltbarkeit). */
public class RedstoneSword extends SwordItem {

    //Pridenauer you damn bastard.

    public RedstoneSword(Tier material) {
        super(material, 4, -2.4F, new Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), itemStack)) {
            return InteractionResult.FAIL;
        } else {
            if (world.isEmptyBlock(pos)) {
                world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, world.random.nextFloat() * 0.4F + 0.8F);
                if (!world.isClientSide) world.setBlockAndUpdate(pos, Blocks.REDSTONE_WIRE.defaultBlockState());
            }

            if (player != null) itemStack.hurtAndBreak(14, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }
}
