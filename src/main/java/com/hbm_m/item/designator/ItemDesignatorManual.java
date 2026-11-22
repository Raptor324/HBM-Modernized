package com.hbm_m.item.designator;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.platform.PlatformHooks;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.hbm_m.platform.StackNbt;

/**
 * Manual designator: right-click opens GUI to set target X/Z with buttons.
 * Port from 1.7.10 ItemDesingatorManual. GUI is opened client-side only.
 */
public class ItemDesignatorManual extends Item implements IDesignatorItem, ITooltipProvider {

    public ItemDesignatorManual(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.hbm_m.client.DesignatorClient.openScreen(player));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (StackNbt.has(stack) && StackNbt.read(stack).contains("xCoord")) {
            tooltip.add(Component.translatable("tooltip.hbm_m.designator.target"));
            tooltip.add(Component.literal("X: " + StackNbt.read(stack).getInt("xCoord")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Z: " + StackNbt.read(stack).getInt("zCoord")).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.hbm_m.designator.no_target"));
        }
    }

    @Override
    public boolean isReady(Level level, ItemStack stack, int x, int y, int z) {
        var tag = StackNbt.read(stack);
        return tag != null && tag.contains("xCoord") && tag.contains("zCoord");
    }

    @Override
    public Vec3 getCoords(Level level, ItemStack stack, int x, int y, int z) {
        var tag = StackNbt.read(stack);
        if (tag == null || !tag.contains("xCoord")) return Vec3.ZERO;
        return new Vec3(tag.getInt("xCoord"), 0, tag.getInt("zCoord"));
    }
}
