package com.hbm_m.main;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.handler.HTTPHandler;
import com.hbm_m.lib.RefStrings;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

public final class ModEventHandler {

	private static final int CURSEFORGE_ORANGE = 0xFF9900;

	private ModEventHandler() {
	}

	public static void register() {
		PlayerEvent.PLAYER_JOIN.register(ModEventHandler::onPlayerJoin);
	}

	private static void onPlayerJoin(Player player) {
		if (player.level().isClientSide()) {
			return;
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

		if (HTTPHandler.modrinthUpdate != null) {
			sendUpdateMessages(player);
		}
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
