package com.hbm_m.client.overlay;

import com.hbm_m.item.armor.ModPowerArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Оверлей V.A.T.S. (Vault-Tec Assisted Targeting System) для силовой брони.
 * Показывает красный интерфейс с информацией о целях.
 * В оригинале VATS работает через RenderOverhead.renderTag() - добавляет красные полоски над мобами.
 */
public class OverlayVATS {

    // Состояние VATS
    private static boolean vatsActive = false;
    private static long vatsActivatedTime = 0;

    /**
     * Активирует VATS режим
     */
    public static void activateVATS() {
        vatsActive = true;
        vatsActivatedTime = System.currentTimeMillis();
    }

    /**
     * Деактивирует VATS режим
     */
    public static void deactivateVATS() {
        vatsActive = false;
    }

    /**
     * Проверяет, активен ли VATS
     */
    public static boolean isVATSActive() {
        return vatsActive;
    }

    /**
     * Рендерит тег VATS над мобом (красные полоски здоровья)
     */
    public static void renderVATSTag(net.minecraft.world.entity.LivingEntity entity, double x, double y, double z, boolean thermalVision) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || !isVATSActive()) {
            return;
        }

        // Проверяем, носит ли игрок силовую броню с VATS
        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            return;
        }

        var chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestplate.getItem() instanceof ModPowerArmorItem armorItem) || !armorItem.getSpecs().hasVats) {
            return;
        }

        // Создаем строку с красными полосками, представляющими здоровье
        int maxBars = (int) Math.min(entity.getMaxHealth(), 100);
        int filledBars = (int) Math.ceil(entity.getHealth() * maxBars / entity.getMaxHealth());

        StringBuilder barBuilder = new StringBuilder();
        barBuilder.append(ChatFormatting.RED);

        for (int i = 0; i < maxBars; i++) {
            if (i == filledBars) {
                barBuilder.append(ChatFormatting.RESET);
            }
            barBuilder.append("|");
        }

        // Рендерим тег над мобом
        renderTag(entity, x, y, z, barBuilder.toString(), thermalVision);
    }

    /**
     * Вспомогательный метод для рендеринга тега (упрощенная версия)
     */
    private static void renderTag(net.minecraft.world.entity.LivingEntity entity, double x, double y, double z, String text, boolean thermalVision) {
        Minecraft mc = Minecraft.getInstance();

        // Простая реализация - рисуем текст над сущностью
        // В оригинале это было сложнее с OpenGL
        if (mc.level != null && mc.player != null) {
            double distance = mc.player.distanceTo(entity);
            if (distance < 32.0D) { // Показываем только близких мобов
                // В реальной реализации здесь должен быть код для рендеринга 3D текста
                // Пока просто логируем для отладки
                System.out.println("VATS: " + entity.getName().getString() + " - " + text);
            }
        }
    }

    public static void onRenderOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.options.hideGui || !vatsActive) {
            return;
        }

        // Проверяем, носит ли игрок силовую броню с VATS
        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            deactivateVATS();
            return;
        }

        var chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestplate.getItem() instanceof ModPowerArmorItem armorItem) || !armorItem.getSpecs().hasVats) {
            deactivateVATS();
            return;
        }

        // Автоматически деактивируем через 10 секунд
        if (System.currentTimeMillis() - vatsActivatedTime > 10000) {
            deactivateVATS();
            return;
        }

        // Рендерим красный оверлей по всему экрану (полупрозрачный)
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0x40FF0000); // Более прозрачный красный

        // Добавляем текст VATS
        String vatsText = "V.A.T.S. ACTIVE";
        int textWidth = mc.font.width(vatsText);
        int textX = screenWidth / 2 - textWidth / 2;
        int textY = screenHeight / 2 - 20;

        guiGraphics.drawString(mc.font, vatsText, textX, textY, 0xFFFFFF);
    }

    public static final net.minecraftforge.client.gui.overlay.IGuiOverlay VATS_OVERLAY = OverlayVATS::onRenderOverlay;
}
