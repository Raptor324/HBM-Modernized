package com.hbm_m.main;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.handler.HTTPHandler;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ConfigSyncS2CPacket;
import com.hbm_m.platform.PlayerPersistentData;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ModEventHandler {

	private static final int CURSEFORGE_ORANGE = 0xFF9900;
	private static final int BOOSTY_ORANGE = 0xF15F2C;
	private static final String NBT_JOIN_MOTD_SHOWN = "hbm_m_join_motd_shown";

	private ModEventHandler() {
	}

	public static void register() {
		PlayerEvent.PLAYER_JOIN.register(ModEventHandler::onPlayerJoin);
	}

	private static void onPlayerJoin(Player player) {
		if (player.level().isClientSide()) {
			return;
		}
		// Синхронизация серверного конфига клиенту при входе (независимо от MOTD).
		if (player instanceof ServerPlayer sp) {
			ConfigSyncS2CPacket.sendTo(sp);
		}
		if (!ModClothConfig.get().enableMOTD) {
			return;
		}

		String modVersion = Platform.getMod(RefStrings.MODID).getVersion();
		String mcVersion = Platform.getMinecraftVersion();

		player.sendSystemMessage(Component.translatable(
				"message.hbm_m.loaded",
				Component.translatable("message.hbm_m.modernized").withStyle(ChatFormatting.YELLOW),
				modVersion,
				mcVersion
		));

		CompoundTag persistent = PlayerPersistentData.get(player);
		if (!persistent.getBoolean(NBT_JOIN_MOTD_SHOWN)) {
			sendSupportMessage(player);
			persistent.putBoolean(NBT_JOIN_MOTD_SHOWN, true);
		}

		if (HTTPHandler.modrinthUpdate != null) {
			sendUpdateMessages(player);
		}
	}

	private static void sendSupportMessage(Player player) {
		MutableComponent line = Component.translatable("message.hbm_m.support_pitch")
				.withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));

		line.append(Component.literal(" "))
				.append(Component.translatable("message.hbm_m.button_boosty")
						.withStyle(Style.EMPTY.withColor(BOOSTY_ORANGE).withUnderlined(true)
								.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, HTTPHandler.BOOSTY_URL))));

		line.append(Component.literal(" "))
				.append(Component.translatable("message.hbm_m.button_crypto")
						.withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withUnderlined(true)
								.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, HTTPHandler.SUPPORT_PAGE_URL))));

		player.sendSystemMessage(line);
	}

	private static void sendUpdateMessages(Player player) {
		HTTPHandler.VersionUpdate modrinth = HTTPHandler.modrinthUpdate;
		HTTPHandler.VersionUpdate curseforge = HTTPHandler.curseforgeUpdate;

		player.sendSystemMessage(
				Component.translatable("message.hbm_m.new_version", modrinth.versionNumber)
						.withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
		);

		MutableComponent downloadLine = Component.translatable("message.hbm_m.download_now")
				.withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));

		downloadLine.append(Component.translatable("message.hbm_m.button_modrinth")
				.withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withUnderlined(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, modrinth.downloadUrl))));

		if (curseforge != null) {
			downloadLine.append(Component.literal(" "))
					.append(Component.translatable("message.hbm_m.button_curseforge")
							.withStyle(Style.EMPTY.withColor(CURSEFORGE_ORANGE).withUnderlined(true)
									.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, curseforge.downloadUrl))));
		}

		player.sendSystemMessage(downloadLine);
	}
}
