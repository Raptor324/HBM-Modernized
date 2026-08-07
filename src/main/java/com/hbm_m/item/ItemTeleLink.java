package com.hbm_m.item;

import java.util.List;

import com.hbm_m.blockentity.machines.MachineTeleporterBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Port of {@code ItemTeleLink} (1.7.10 Original). Sneak-right-click any block records its position
 * into the item's NBT; right-click a {@code machine_teleporter} block applies the saved position as
 * that teleporter's destination.
 */
public class ItemTeleLink extends Item {

    public ItemTeleLink(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return useAsLinker(context);
        }
        return recordPosition(context);
    }

    private InteractionResult recordPosition(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("dim", context.getLevel().dimension().location().toString());

        Level level = context.getLevel();
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal("[TeleLink] Set teleporter exit to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ".")
                            .withStyle(ChatFormatting.AQUA), false);
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult useAsLinker(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (!(level.getBlockEntity(pos) instanceof MachineTeleporterBlockEntity teleporter)) {
            return recordPosition(context);
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("x")) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.literal("[TeleLink] No destination set!").withStyle(ChatFormatting.RED), false);
            }
            return InteractionResult.FAIL;
        }

        teleporter.setTarget(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"),
                tag.contains("dim") ? tag.getString("dim") : "minecraft:overworld");

        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal("[TeleLink] Teleporter's destination has been set!").withStyle(ChatFormatting.AQUA), false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("x")) {
            tooltip.add(Component.literal("X: " + tag.getInt("x")));
            tooltip.add(Component.literal("Y: " + tag.getInt("y")));
            tooltip.add(Component.literal("Z: " + tag.getInt("z")));
            tooltip.add(Component.literal("D: " + tag.getString("dim")));
        } else {
            tooltip.add(Component.literal("Select exit location first!").withStyle(ChatFormatting.RED));
        }
    }
}
