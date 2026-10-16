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
	}

	private static void onPlayerJoin(Player player) {
		if (player.level().isClientSide()) {
			return;
		}
		// Синхронизация серверного конфига клиенту при входе.
		// MOTD теперь чисто клиентский (ClientMotdHandler, client.json -> enableMOTD).
		if (player instanceof ServerPlayer sp) {
			ConfigSyncS2CPacket.sendTo(sp);
		}
	}
}
