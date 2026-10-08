package com.hbm_m.main;

import com.hbm_m.network.ConfigSyncS2CPacket;

import dev.architectury.event.events.common.PlayerEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ModEventHandler {

	private ModEventHandler() {
	}

	public static void register() {
		PlayerEvent.PLAYER_JOIN.register(ModEventHandler::onPlayerJoin);
		// Original ModEventHandler.onPlayerTick (START): PermaSyncPacket, Abschnitt POLLUTION
		dev.architectury.event.events.common.TickEvent.PLAYER_PRE.register(player -> {
			if (player instanceof ServerPlayer sp) com.hbm_m.network.PollutionSyncPacket.sendTo(sp);
		});
	}

	private static void onPlayerJoin(Player player) {
		if (player.level().isClientSide()) {
			return;
		}
		// Синхронизация серверного конфига клиенту при входе.
		// MOTD теперь чисто клиентский (ClientMotdHandler, client.json -> enableMOTD).
		if (player instanceof ServerPlayer sp) {
			ConfigSyncS2CPacket.sendTo(sp);

			// Original ModEventHandler.onPlayerLogin: Enten-Hinweis (PlayerInformPacket, 30 000 ms)
			net.minecraft.nbt.CompoundTag perDat = com.hbm_m.platform.PlayerPersistentData.get(sp).getCompound(Player.PERSISTED_NBT_TAG);
			if (com.hbm_m.config.MobConfig.enableDucks && !perDat.getBoolean("hasDucked"))
				com.hbm_m.network.InfoToastPacket.sendTo(sp, net.minecraft.network.chat.Component.literal("Press O to Duck!"), 600, com.hbm_m.network.InfoToastPacket.ID_DUCK, 0xFFFFFF);
		}
	}
}
