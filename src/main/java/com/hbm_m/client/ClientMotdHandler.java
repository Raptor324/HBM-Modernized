package com.hbm_m.client;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.handler.HTTPHandler;
import com.hbm_m.lib.RefStrings;

import com.hbm_m.platform.PlatformHooks;

import dev.architectury.platform.Platform;

import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Клиентский MOTD при входе в мир (одиночная игра и мультиплеер).
 * Решение о показе принимает клиент через свой конфиг (client.json -> enableMOTD),
 * сервер эти сообщения не отправляет.
 */
public final class ClientMotdHandler {

	private static final int CURSEFORGE_ORANGE = 0xFF9900;
	private static final int BOOSTY_ORANGE = 0xF15F2C;
	/** Сообщения показываются один раз за подключение к миру (не на каждую смену измерения). */
	private static boolean shownThisSession = false;

	private ClientMotdHandler() {
	}

	public static void register() {
		dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
			if (shownThisSession) {
				return;
			}
			shownThisSession = true;
			if (!ModClothConfig.get().enableMOTD) {
				return;
			}
			showMotd(player, !isSupportShown());
		});
		dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> shownThisSession = false);
	}

	/**
	 * Флаг «сообщение о поддержке уже показано» живёт в {@code config/hbm_m/support_shown.flag}
	 * и переживает перезапуски игры (аналог старого серверного NBT-флага).
	 */
	private static boolean isSupportShown() {
		try {
			return java.nio.file.Files.exists(getFlagFile());
		} catch (Exception e) {
			return false;
		}
	}

	private static void markSupportShown() {
		try {
			java.nio.file.Files.createDirectories(getFlagFile().getParent());
			java.nio.file.Files.writeString(getFlagFile(), "1");
		} catch (Exception e) {
			com.hbm_m.main.MainRegistry.LOGGER.warn("Failed to persist MOTD support flag", e);
		}
	}

	private static java.nio.file.Path getFlagFile() {
		return PlatformHooks.getConfigDir().resolve("hbm_m").resolve("support_shown.flag");
	}

	private static void showMotd(LocalPlayer player, boolean firstTime) {
		String modVersion = Platform.getMod(RefStrings.MODID).getVersion();
		String mcVersion = Platform.getMinecraftVersion();

		player.displayClientMessage(Component.translatable(
				"message.hbm_m.loaded",
				Component.translatable("message.hbm_m.modernized").withStyle(ChatFormatting.YELLOW),
				modVersion,
				mcVersion
		), false);

		if (firstTime) {
			sendSupportMessage(player);
			markSupportShown();
		}

		if (HTTPHandler.modrinthUpdate != null) {
			sendUpdateMessages(player);
		}
	}

	private static void sendSupportMessage(LocalPlayer player) {
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

		player.displayClientMessage(line, false);
	}

	private static void sendUpdateMessages(LocalPlayer player) {
		HTTPHandler.VersionUpdate modrinth = HTTPHandler.modrinthUpdate;
		HTTPHandler.VersionUpdate curseforge = HTTPHandler.curseforgeUpdate;

		player.displayClientMessage(
				Component.translatable("message.hbm_m.new_version", modrinth.versionNumber)
						.withStyle(ChatFormatting.YELLOW),
				false
		);

		MutableComponent downloadLine = Component.translatable("message.hbm_m.download_now")
				.withStyle(ChatFormatting.YELLOW);

		downloadLine.append(Component.translatable("message.hbm_m.button_modrinth")
				.withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withUnderlined(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, modrinth.downloadUrl))));

		if (curseforge != null) {
			downloadLine.append(Component.literal(" "))
					.append(Component.translatable("message.hbm_m.button_curseforge")
							.withStyle(Style.EMPTY.withColor(CURSEFORGE_ORANGE).withUnderlined(true)
									.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, curseforge.downloadUrl))));
		}

		player.displayClientMessage(downloadLine, false);
	}
}
