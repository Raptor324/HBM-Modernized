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

public class ItemDigamma extends Item {

	/** Ticks until half-life / rate at which Digamma radiation is applied each tick. */
	private final int digamma;

	public ItemDigamma(int digamma, Properties properties) {
		super(properties);
		//obacht! the particle's digamma value is "ticks until half life" while the superclass' interpretation is "simply add flat value"
		this.digamma = digamma;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		super.inventoryTick(stack, level, entity, slotId, isSelected);

		if (!level.isClientSide() && entity instanceof Player player) {
			float digammaRate = 1F / (float) digamma;
			// Digamma radiation applied as accumulated radiation (no dedicated Digamma system yet)
			PlayerHandler.incrementPlayerRads(player, digammaRate);

			// Kill player if accumulated radiation exceeds lethal Digamma threshold (100 mDRX)
			if (PlayerHandler.getPlayerRads(player) >= 100F) {
				player.hurt(level.damageSources().generic(), Float.MAX_VALUE);
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);

		float halfLifeSeconds = digamma / 20F;
		float drxPerSecond = ((int) ((1000F / digamma) * 200F)) / 10F;

		tooltip.add(Component.translatable("trait.hlParticle", "1.67*10³⁴a").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("trait.hlPlayer", halfLifeSeconds + "s").withStyle(ChatFormatting.RED));
		tooltip.add(Component.empty());
		tooltip.add(Component.translatable("trait.digamma").withStyle(ChatFormatting.RED));
		tooltip.add(Component.literal(drxPerSecond + "mDRX/s").withStyle(ChatFormatting.DARK_RED));
		tooltip.add(Component.translatable("trait.drop").withStyle(ChatFormatting.RED));
	}

}