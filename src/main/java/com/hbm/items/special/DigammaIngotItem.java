package com.hbm.items.special;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.radiation.PlayerHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Digamma Ingot item that emits 1 mDRX/s radiation when held in player inventory.
 */
public class DigammaIngotItem extends Item {

	public DigammaIngotItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		super.inventoryTick(stack, level, entity, slotId, isSelected);

		if (!level.isClientSide() && entity instanceof Player player) {
			// Apply 1 mDRX/s radiation (1/20 per tick, 20 ticks per second)
			PlayerHandler.incrementPlayerRads(player, 1.0F / 20.0F);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);

		tooltip.add(Component.literal("").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("trait.digamma").withStyle(ChatFormatting.RED));
		tooltip.add(Component.literal("1.0 mDRX/s").withStyle(ChatFormatting.DARK_RED));
	}

}
