package com.hbm_m.client;

import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hbm_m.handler.HTTPHandler;
import com.hbm_m.main.MainRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Клиентский «Tip of the day» (порт оригинального LoadingScreenRendererNT).
 *
 * Список подсказок тянется с нашего GitHub-репозитория ({@code tips/<lang>.txt},
 * один файл на язык, fallback — en_us), скачивается один раз в фоне при старте
 * клиента. Одна строка файла = одна подсказка, {@code $} внутри строки = перенос
 * строки (как в 1.7.10), строки с {@code #} — комментарии.
 *
 * Рисуется поверх экрана загрузки мира клиентским миксином
 * {@code LoadingScreenTipMixin} — жёлтым текстом по центру, над прогресс-надписью.
 * Система чисто клиентская: сервер ничего не отправляет.
 */
public final class ClientTipsHandler {

	/** Ванильный жёлтый (аналог EnumChatFormatting.YELLOW из 1.7.10). */
	private static final int YELLOW = 0xFFFF55;
	/** Вертикальный отступ подсказки над центром экрана (как height/2 - 4 - 60 в оригинале). */
	private static final int Y_OFFSET = 64;
	private static final int LINE_HEIGHT = 10;

	/** Дефолтная подсказка, если GitHub недоступен (как в оригинальном chooseTip()). */
	private static final String DEFAULT_TIP = "Explore! There's tons of free stuff to find.";

	private static final Random RANDOM = new Random();

	/** volatile + замена списка целиком: пишет фоновый поток, читает поток рендера. */
	public static volatile List<String> tips = new ArrayList<>();
	/** Выбранная подсказка (кэш, чтобы не менялась между кадрами загрузочного экрана). */
	private static volatile List<String> chosenTip;

	private ClientTipsHandler() {
	}

	/** Вызывается из ClientSetup#initClient() — запускает фоновую загрузку подсказок. */
	public static void fetchAsync() {
		Thread fetcher = new Thread(() -> {
			String lang = Minecraft.getInstance().getLanguageManager().getSelected();
			List<String> loaded = tryLoad(lang);
			if (loaded.isEmpty() && !HTTPHandler.TIPS_FALLBACK_LANG.equals(lang)) {
				loaded = tryLoad(HTTPHandler.TIPS_FALLBACK_LANG);
			}
			tips = loaded;
			if (loaded.isEmpty()) {
				MainRegistry.LOGGER.warn("Tip of the day: no tips loaded from {}, using the built-in default", HTTPHandler.TIPS_BASE_URL);
			} else {
				MainRegistry.LOGGER.info("Tip of the day: loaded {} tips for lang {}", loaded.size(), lang);
			}
		}, "HBM-Tips-Fetcher");
		fetcher.setDaemon(true);
		fetcher.start();
	}

	private static List<String> tryLoad(String lang) {
		try {
			URL url = URI.create(HTTPHandler.TIPS_BASE_URL + "/" + lang + ".txt").toURL();
			List<String> lines = HTTPHandler.readLines(url);
			List<String> parsed = new ArrayList<>();
			for (String line : lines) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}
				parsed.add(trimmed);
			}
			return parsed;
		} catch (IOException e) {
			return List.of();
		}
	}

	/** Подсказка для показа: случайная из загруженных либо вшитый дефолт. */
	private static List<String> chooseTip() {
		List<String> current = tips;
		String tip = current.isEmpty() ? DEFAULT_TIP : current.get(RANDOM.nextInt(current.size()));
		// $ внутри строки = принудительный перенос строки (формат оригинального гиста).
		return List.of(tip.split("\\$"));
	}

	/**
	 * Рендер поверх загрузочного экрана; вызывается из миксина на каждый кадр.
	 * Первая строка — локализуемый префикс «Tip of the day:» (message.hbm_m.tip_of_the_day).
	 */
	public static void render(GuiGraphics guiGraphics) {
		if (!com.hbm_m.config.ModClothConfig.get().enableTips) {
			return;
		}
		if (chosenTip == null) {
			chosenTip = chooseTip();
		}
		Minecraft mc = Minecraft.getInstance();
		int width = guiGraphics.guiWidth();
		int height = guiGraphics.guiHeight();

		Component prefix = Component.translatable("message.hbm_m.tip_of_the_day");
		List<String> frags = chosenTip;
		for (int i = 0; i < frags.size(); i++) {
			Component line = i == 0
					? Component.empty().append(prefix).append(" " + frags.get(i))
					: Component.literal(frags.get(i));
			int x = (width - mc.font.width(line)) / 2;
			guiGraphics.drawString(mc.font, line, x, height / 2 - Y_OFFSET + i * LINE_HEIGHT, YELLOW);
		}
	}
}
