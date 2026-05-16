package com.hbm.items.tool;

import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemDigammaDiagnostic extends Item {

	public ItemDigammaDiagnostic(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide()) {
			level.playSound(null, player.blockPosition(), ModSounds.CLICK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
			float rads = PlayerHandler.getPlayerRads(player);
			player.sendSystemMessage(Component.literal(
					String.format("[Digamma Diagnostic] Accumulated Radiation: %.2f mDRX", rads)).withStyle(ChatFormatting.RED));
		}
		return InteractionResultHolder.success(player.getItemInHand(hand));
	}
}