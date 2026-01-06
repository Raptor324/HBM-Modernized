package com.hbm_m.client.overlay;

import com.hbm_m.item.armor.ModPowerArmorItem;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Оверлей тепловизора для силовой брони.
 * Инвертирует цвета экрана для имитации тепловизора.
 */
public class OverlayThermal {

    // Состояние тепловизора
    private static boolean thermalActive = false;

    /**
     * Активирует тепловизор
     */
    public static void activateThermal() {
        thermalActive = true;
    }

    /**
     * Деактивирует тепловизор
     */
    public static void deactivateThermal() {
        thermalActive = false;
    }

    /**
     * Проверяет, активен ли тепловизор
     */
    public static boolean isThermalActive() {
        return thermalActive;
    }

    public static void onRenderOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.options.hideGui || !thermalActive) {
            return;
        }

        // Проверяем, носит ли игрок силовую броню с тепловизором
        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            deactivateThermal();
            return;
        }

        var chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestplate.getItem() instanceof ModPowerArmorItem armorItem) || !armorItem.getSpecs().hasThermal) {
            deactivateThermal();
            return;
        }

        // Для тепловизора можно использовать инверсию цветов через шейдеры
        // Пока что просто добавим зеленый оттенок как индикатор работы
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Рендерим полупрозрачный зеленый оверлей
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0x4000FF00); // Полупрозрачный зеленый

        // Добавляем текст тепловизора
        String thermalText = "THERMAL VISION";
        int textWidth = mc.font.width(thermalText);
        int textX = 10;
        int textY = 10;

        guiGraphics.drawString(mc.font, thermalText, textX, textY, 0x00FF00);

        RenderSystem.disableBlend();

        // TODO: Реализовать настоящую инверсию цветов через шейдеры
        // Для полной реализации нужно создать шейдер, который инвертирует цвета
    }

    public static final net.minecraftforge.client.gui.overlay.IGuiOverlay THERMAL_OVERLAY = OverlayThermal::onRenderOverlay;
}

