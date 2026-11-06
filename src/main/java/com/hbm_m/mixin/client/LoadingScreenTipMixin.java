package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm_m.client.ClientTipsHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;

/**
 * Порт оригинального LoadingScreenRendererNT: жёлтая подсказка «tip of the day»
 * поверх экранов, которые видны при заходе в мир — экрана подключения к серверу
 * ({@link ConnectScreen}), экрана загрузки измерения ({@link LevelLoadingScreen})
 * и экрана «Loading terrain...» ({@link ReceivingLevelScreen}).
 *
 * Сам рендер — в {@link ClientTipsHandler#render} (чисто клиентский, серверу
 * ничего не нужно). Отключается конфигом client.json -> enableTips.
 */
@Mixin({LevelLoadingScreen.class, ReceivingLevelScreen.class, ConnectScreen.class})
public class LoadingScreenTipMixin {

	@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
	private void hbm_m$renderTipOfTheDay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		ClientTipsHandler.render(guiGraphics);
	}
}
