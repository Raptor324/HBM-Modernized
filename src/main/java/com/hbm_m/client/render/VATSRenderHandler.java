package com.hbm_m.client.render;

import com.hbm_m.client.overlay.OverlayVATS;
import com.hbm_m.item.armor.ModPowerArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Обработчик рендеринга для VATS системы.
 * Добавляет красные полоски здоровья над мобами когда VATS активен.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class VATSRenderHandler {

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        LocalPlayer player = Minecraft.getInstance().player;

        // Проверяем условия для VATS
        if (player == null || entity == player) {
            return;
        }

        // Проверяем, активен ли VATS и носит ли игрок силовую броню с VATS
        if (!OverlayVATS.isVATSActive()) {
            return;
        }

        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            return;
        }

        var chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestplate.getItem() instanceof ModPowerArmorItem armorItem) || !armorItem.getSpecs().hasVats) {
            return;
        }

        // Проверяем расстояние
        double distance = player.distanceTo(entity);
        if (distance > 32.0D) {
            return;
        }

        // Создаем строку с красными полосками здоровья
        int maxBars = (int) Math.min(entity.getMaxHealth(), 100);
        int filledBars = (int) Math.ceil(entity.getHealth() * maxBars / entity.getMaxHealth());

        StringBuilder barBuilder = new StringBuilder();
        barBuilder.append(net.minecraft.ChatFormatting.RED);

        for (int i = 0; i < maxBars; i++) {
            if (i == filledBars) {
                barBuilder.append(net.minecraft.ChatFormatting.RESET);
            }
            barBuilder.append("|");
        }

        // Рендерим тег над мобом
        renderVATSTag(entity, entity.getX(), entity.getY(), entity.getZ(), barBuilder.toString(), armorItem.getSpecs().hasThermal);
    }

    /**
     * Рендерит тег VATS над сущностью
     * Упрощенная версия - в оригинале использовался более сложный рендеринг
     */
    private static void renderVATSTag(LivingEntity entity, double x, double y, double z, String text, boolean thermalVision) {
        Minecraft mc = Minecraft.getInstance();

        // Простая реализация - добавляем компонент над сущностью
        // В реальной реализации здесь должен быть 3D рендеринг текста
        if (mc.level != null) {
            // Создаем компонент для отображения
            net.minecraft.network.chat.Component tagComponent = net.minecraft.network.chat.Component.literal(text);

            // Добавляем компонент к кастомному имени сущности (временное решение)
            if (entity.getCustomName() == null) {
                entity.setCustomName(tagComponent);
                // Сбрасываем через короткое время
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        entity.setCustomName(null);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
}
