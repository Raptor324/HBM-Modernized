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
import org.jetbrains.annotations.Nullable;
import com.hbm_m.platform.PlatformHooks;

/**
 * Port of {@code ItemTeleLink} (1.7.10 Original). Sneak-right-click any block records its position
 * into the item's NBT; right-click a {@code machine_teleporter} block applies the saved position as
 * that teleporter's destination.
 */
public class ItemTeleLink extends Item implements com.hbm_m.item.ITooltipProvider {

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
        PlatformHooks.putInt(stack, "x", pos.getX());
        PlatformHooks.putInt(stack, "y", pos.getY());
        PlatformHooks.putInt(stack, "z", pos.getZ());
        PlatformHooks.putString(stack, "dim", context.getLevel().dimension().location().toString());

        Level level = context.getLevel();
        PlatformHooks.playSound(level, pos, SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1.0F, 1.0F);
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

        if (!PlatformHooks.contains(stack, "x")) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.literal("[TeleLink] No destination set!").withStyle(ChatFormatting.RED), false);
            }
            return InteractionResult.FAIL;
        }

        teleporter.setTarget(PlatformHooks.getInt(stack, "x"), PlatformHooks.getInt(stack, "y"), PlatformHooks.getInt(stack, "z"),
                PlatformHooks.contains(stack, "dim") ? PlatformHooks.getString(stack, "dim") : "minecraft:overworld");

        PlatformHooks.playSound(level, pos, SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal("[TeleLink] Teleporter's destination has been set!").withStyle(ChatFormatting.AQUA), false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (PlatformHooks.contains(stack, "x")) {
            tooltip.add(Component.literal("X: " + PlatformHooks.getInt(stack, "x")));
            tooltip.add(Component.literal("Y: " + PlatformHooks.getInt(stack, "y")));
            tooltip.add(Component.literal("Z: " + PlatformHooks.getInt(stack, "z")));
            tooltip.add(Component.literal("D: " + PlatformHooks.getString(stack, "dim")));
        } else {
            tooltip.add(Component.literal("Select exit location first!").withStyle(ChatFormatting.RED));
        }
    }
}
