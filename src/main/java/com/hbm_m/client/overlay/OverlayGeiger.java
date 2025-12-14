package com.hbm_m.client.overlay;

// Оверлей для геигер-счетчика. Показывает уровень радиации игрока и окружающей среды,
// если геигер-счетчик есть в инвентаре игрока. Использует кэширование для оптимизации проверки инвентаря.
// Рендерит прогресс-бар для радиации игрока и иконки/текст для радиации окружающей среды.
// Использует IGuiOverlay из Forge для интеграции с GUI игры.
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.custom.radiation_meter.ItemGeigerCounter;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class OverlayGeiger {

    public static float clientPlayerRadiation = 0.0f;
    public static float clientTotalEnvironmentRadiation = 0.0f;

    private static long lastInventoryCheckTime = 0;
    private static boolean hasGeigerCached = false;
    private static final long INVENTORY_CHECK_INTERVAL = 1000;

    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/misc/overlay_misc.png");

    public static void onRenderOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInventoryCheckTime > INVENTORY_CHECK_INTERVAL) {
            lastInventoryCheckTime = currentTime; // Сбрасываем таймер
            
            // Выполняем медленную проверку и обновляем кэш
            if (player != null) {
                hasGeigerCached = checkForGeiger(player);
            } else {
                hasGeigerCached = false; // Если игрока нет, то и счетчика нет
            }
        }
        
        // 2. БЫСТРАЯ ПРОВЕРКА КЭША (КАЖДЫЙ КАДР)
        // Если кэш говорит, что счетчика нет, просто выходим.
        if (!hasGeigerCached || player == null || mc.options.hideGui) {
            return;
        }

        float environmentRad = clientTotalEnvironmentRadiation;

        // 3. Данные для полосы прогресса
        float playerRadForBar = clientPlayerRadiation;
        float maxPlayerRadForBar = ModClothConfig.get().maxPlayerRad;
        if (maxPlayerRadForBar <= 0) maxPlayerRadForBar = 1.0f;
        int barLength = (int)(74 * Math.min(playerRadForBar / maxPlayerRadForBar, 1.0f));


        // ЛОГИКА РЕНДЕРА
        int posX = 3;
        int posY = screenHeight - 20;

        // Рендер фона
        guiGraphics.blit(OVERLAY, posX, posY, 0, 0, 94, 18);
        
        // Рендер заполнения полосы (использует barLength, который зависит от радиации игрока)
        if (barLength > 0) {
            guiGraphics.blit(OVERLAY, posX + 1, posY + 1, 1, 19, barLength, 16);
        }

        // Рендер иконки и текста
        if (!player.isCreative() && !player.isSpectator()) {
            // Если да, то рендерим текст и иконки, показывающие опасность среды
            
            // Рендер иконки (используем environmentRad)
            if (environmentRad >= 25) {
                guiGraphics.blit(OVERLAY, posX + 94 + 2, posY, 36, 36, 18, 18);
            } else if (environmentRad >= 10) {
                guiGraphics.blit(OVERLAY, posX + 94 + 2, posY, 18, 36, 18, 18);
            } else if (environmentRad >= 2.5) {
                guiGraphics.blit(OVERLAY, posX + 94 + 2, posY, 0, 36, 18, 18);
            }

            // Рендер текста (используем environmentRad)
            if (environmentRad > 1000) {
                guiGraphics.drawString(mc.font, Component.literal(">1000 RAD/s"), posX, posY - 10, 0xFF0000);
            } else if (environmentRad >= 1) {
                guiGraphics.drawString(mc.font, Component.literal(((int) Math.round(environmentRad)) + " RAD/s"), posX, posY - 10, 0xFFFF00);
            } else if (environmentRad > 0) {
                guiGraphics.drawString(mc.font, Component.literal("<1 RAD/s"), posX, posY - 10, 0x00FF00);
            }
        }
    }

    private static boolean checkForGeiger(LocalPlayer player) {
        // Проверяем руки в первую очередь
        if (player.getMainHandItem().getItem() instanceof ItemGeigerCounter || player.getOffhandItem().getItem() instanceof ItemGeigerCounter) {
            return true;
        }
        // Затем остальной инвентарь
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ItemGeigerCounter) {
                return true;
            }
        }
        return false;
    }

    public static final IGuiOverlay GEIGER_HUD_OVERLAY = OverlayGeiger::onRenderOverlay;
}