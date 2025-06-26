package com.hbm_m.client.overlay;

import com.hbm_m.item.GeigerCounterItem;
import com.hbm_m.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class GeigerOverlay {
    public static float clientPlayerRadiation = 0.0f;
    public static float clientTotalEnvironmentRadiation = 0.0f; // Новое поле

    // Заменено на современный способ создания ResourceLocation
    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/gui/overlay_misc.png");

    public static void onRenderOverlay(GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        MainRegistry.LOGGER.debug("GeigerOverlay: onRenderOverlay called.");
        boolean hasGeiger = false;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof GeigerCounterItem) {
                hasGeiger = true;
                MainRegistry.LOGGER.debug("GeigerOverlay: Found Geiger Counter in inventory.");
                break;
            }
        }

        if (!hasGeiger) {
            if (player.getMainHandItem().getItem() instanceof GeigerCounterItem) {
                hasGeiger = true;
                MainRegistry.LOGGER.debug("GeigerOverlay: Found Geiger Counter in main hand.");
            } else if (player.getOffhandItem().getItem() instanceof GeigerCounterItem) {
                hasGeiger = true;
                MainRegistry.LOGGER.debug("GeigerOverlay: Found Geiger Counter in off hand.");
            }
        }

        MainRegistry.LOGGER.debug("GeigerOverlay: hasGeiger = {}", hasGeiger);
        if (!hasGeiger) return;

        float radPerSec = player.isCreative() || player.isSpectator() ? 0.0f : GeigerOverlay.clientTotalEnvironmentRadiation; // Используем новое поле
        float maxRad = 50.0f;
        float barFrac = Math.min(radPerSec / maxRad, 1.0f);

        int x = 4;
        int y = screenHeight - 28;

        guiGraphics.blit(OVERLAY, x, y, 0, 0, 150, 24, 256, 256);
        int fill = (int) (barFrac * 138);
        if (fill > 0) {
            guiGraphics.blit(OVERLAY, x + 6, y + 6, 6, 6, fill, 12, 256, 256);
        }
        guiGraphics.blit(OVERLAY, x + 134, y + 4, 150, 0, 18, 18, 256, 256);

        // Отображаем "Общее радиационное заражение среды"
        String totalEnvRadText = String.format("%.1f RAD/s", GeigerOverlay.clientTotalEnvironmentRadiation);
        guiGraphics.drawString(mc.font, totalEnvRadText, x + 25, y + 8, 0xFFFFFF, false);

        // Отображаем "Уровень радиоактивного заражения игрока"
        String playerRadText = String.format("%.1f RAD", GeigerOverlay.clientPlayerRadiation);
        guiGraphics.drawString(mc.font, playerRadText, x + 25, y + 18, 0xFFFFFF, false);
    }
}