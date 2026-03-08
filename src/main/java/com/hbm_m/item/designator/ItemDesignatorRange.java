package com.hbm_m.item.designator;

import java.util.List;

import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Range designator: right-click in air to raycast (300 blocks) and set target at looked block.
 * Does not set target when looking at a launch pad. Port from 1.7.10 ItemDesingatorRange.
 */
public class ItemDesignatorRange extends Item implements IDesignatorItem {

    public ItemDesignatorRange(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains("xCoord")) {
            tooltip.add(Component.translatable("tooltip.hbm_m.designator.target"));
            tooltip.add(Component.literal("X: " + stack.getTag().getInt("xCoord")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Z: " + stack.getTag().getInt("zCoord")).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.hbm_m.designator.no_target"));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(300, 1f, false);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        var pos = blockHit.getBlockPos();
        var block = level.getBlockState(pos).getBlock();

        if (block == ModBlocks.LAUNCH_PAD.get() || block == ModBlocks.LAUNCH_PAD_RUSTED.get()) {
            return InteractionResultHolder.pass(stack);
        }

        var tag = stack.getOrCreateTag();
        tag.putInt("xCoord", pos.getX());
        tag.putInt("zCoord", pos.getZ());

        if (level.isClientSide()) {
            player.displayClientMessage(
                    Component.translatable("message.hbm_m.designator.position_set_xy", pos.getX(), pos.getZ()), true);
        }
        level.playSound(player, player.blockPosition(), ModSounds.TOOL_TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isReady(Level level, ItemStack stack, int x, int y, int z) {
        return stack.hasTag() && stack.getTag().contains("xCoord");
    }

    @Override
    public Vec3 getCoords(Level level, ItemStack stack, int x, int y, int z) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains("xCoord")) return Vec3.ZERO;
        return new Vec3(tag.getInt("xCoord"), 0, tag.getInt("zCoord"));
    }
}
