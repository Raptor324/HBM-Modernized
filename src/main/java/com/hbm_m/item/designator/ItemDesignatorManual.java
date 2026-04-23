package com.hbm_m.item.designator;

import java.util.List;

import com.hbm_m.api.item.IDesignatorItem;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Manual designator: right-click opens GUI to set target X/Z with buttons.
 * Port from 1.7.10 ItemDesingatorManual. GUI is opened client-side only.
 */
public class ItemDesignatorManual extends Item implements IDesignatorItem {

    public ItemDesignatorManual(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.hbm_m.client.DesignatorClient.openScreen(player));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
