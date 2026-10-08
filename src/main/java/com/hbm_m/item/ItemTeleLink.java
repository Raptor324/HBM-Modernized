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
 * 1:1 {@code ItemTeleLink} (1.7.10 Original): Rechtsklick (ohne Schleichen) auf einen beliebigen Block merkt
 * dessen Position als Ausgang, Rechtsklick auf einen {@code machine_teleporter} traegt die gemerkte Position als
 * dessen Ziel ein. Schleichen tut nichts.
 */
public class ItemTeleLink extends Item implements com.hbm_m.item.ITooltipProvider {

    public ItemTeleLink(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        // Original: nur ohne Schleichen und nur serverseitig
        if (context.getPlayer() != null && !context.getPlayer().isShiftKeyDown() && !level.isClientSide()) {
            return useAsLinker(context);
        }
        return InteractionResult.PASS;
    }

    private static void sound(UseOnContext context, String key) {
        if (context.getPlayer() == null) return;
        net.minecraft.world.entity.player.Player p = context.getPlayer();
        context.getLevel().playSound(null, p.getX(), p.getY(), p.getZ(), "hbm:item.techBoop".equals(key) ? com.hbm_m.sound.ModSounds.TOOL_TECH_BOOP.get() : com.hbm_m.sound.ModSounds.TOOL_TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private InteractionResult recordPosition(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        PlatformHooks.putInt(stack, "x", pos.getX());
        PlatformHooks.putInt(stack, "y", pos.getY());
        PlatformHooks.putInt(stack, "z", pos.getZ());
        PlatformHooks.putString(stack, "dim", context.getLevel().dimension().location().toString());

        sound(context, "hbm:item.techBleep");
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal("[TeleLink] Set teleporter exit to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ".")
                            .withStyle(ChatFormatting.AQUA), false);
            context.getPlayer().swing(context.getHand(), true);
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
            sound(context, "hbm:item.techBoop");
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.literal("[TeleLink] No destination set!").withStyle(ChatFormatting.RED), false);
            }
            return InteractionResult.PASS;
        }

        teleporter.setTarget(PlatformHooks.getInt(stack, "x"), PlatformHooks.getInt(stack, "y"), PlatformHooks.getInt(stack, "z"),
                PlatformHooks.contains(stack, "dim") ? PlatformHooks.getString(stack, "dim") : "minecraft:overworld");

        sound(context, "hbm:item.techBleep");
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal("[TeleLink] Teleporters destination has been set!").withStyle(ChatFormatting.AQUA), false);
            context.getPlayer().swing(context.getHand(), true);
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
