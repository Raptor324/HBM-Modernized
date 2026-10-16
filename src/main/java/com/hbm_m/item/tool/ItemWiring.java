package com.hbm_m.item.tool;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.network.PylonBaseBlockEntity;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт ItemWiring (1.7.10): проводка ЛЭП. ПКМ по первому пилону запоминает его,
 * ПКМ по второму — создаёт соединение (сообщения об ошибках как в оригинале).
 */
public class ItemWiring extends Item {

    private static final String TAG_X = "hbmWireX";
    private static final String TAG_Y = "hbmWireY";
    private static final String TAG_Z = "hbmWireZ";

    public ItemWiring(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof PylonBaseBlockEntity pylon)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();

        if (!hasStart(stack)) {
            PlatformHooks.editItemTag(stack, tag -> {
                tag.putInt(TAG_X, pos.getX());
                tag.putInt(TAG_Y, pos.getY());
                tag.putInt(TAG_Z, pos.getZ());
            });
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("chat.hbm_m.wire_start"), false);
            }
        } else if (!level.isClientSide) {
            BlockPos start = getStart(stack);
            BlockEntity startTe = level.getBlockEntity(start);
            if (startTe instanceof PylonBaseBlockEntity first) {
                PylonBaseBlockEntity.tryConnect(player, first, pylon);
            } else {
                player.displayClientMessage(Component.translatable("chat.hbm_m.wire_error"), false);
            }
            clearStart(stack);
        }
        player.swing(InteractionHand.MAIN_HAND);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean hasStart(ItemStack stack) {
        return PlatformHooks.hasItemTag(stack) && PlatformHooks.getItemTag(stack).contains(TAG_Y);
    }

    private static BlockPos getStart(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = PlatformHooks.getItemTag(stack);
        return new BlockPos(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z));
    }

    private static void clearStart(ItemStack stack) {
        PlatformHooks.editItemTag(stack, tag -> {
            tag.remove(TAG_X);
            tag.remove(TAG_Y);
            tag.remove(TAG_Z);
        });
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        addTooltip(stack, tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addTooltip(stack, tooltip);
    }
    *///?}

    private void addTooltip(ItemStack stack, List<Component> tooltip) {
        if (hasStart(stack)) {
            BlockPos start = getStart(stack);
            tooltip.add(Component.literal("Wire start x: " + start.getX()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Wire start y: " + start.getY()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Wire start z: " + start.getZ()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.hbm_m.wiring"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide || !hasStart(stack) || !(entity instanceof Player player)) return;
        BlockPos start = getStart(stack);
        double dist = Math.sqrt(player.distanceToSqr(start.getX() + 0.5, start.getY() + 0.5, start.getZ() + 0.5));
        player.displayClientMessage(
                Component.literal(stack.getHoverName().getString() + ": " + (int) dist + "m"), true);
    }
}
