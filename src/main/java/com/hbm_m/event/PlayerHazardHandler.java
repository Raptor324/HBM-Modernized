package com.hbm_m.event;

import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.HazardType;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import dev.architectury.event.events.common.TickEvent;

/**
 * Обработчик для применения эффектов опасностей (радиация, пирофорность) к игроку на основе его инвентаря.
 * Эффекты применяются каждый тик на сервере, если игрок не в креативе или режиме наблюдателя.
 */
public class PlayerHazardHandler {

    /**
     * Регистрация обработчика события.
     * Вызывается один раз при инициализации мода.
     */
    public static void init() {
        TickEvent.PLAYER_POST.register(PlayerHazardHandler::onPlayerTick);
    }

    private static void onPlayerTick(Player player) {
        // Выполняем только на сервере
        if (player.level().isClientSide) {
            return;
        }

        // Не применяем эффекты в креативе или режиме наблюдателя
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        // Инициализируем суммарные уровни опасностей 
        float totalRadiation = 0;
        float totalIgnition = 0;

        // Сканируем основной инвентарь, броню и предмет во второй руке
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                totalRadiation += HazardSystem.getHazardLevelFromStack(stack, HazardType.RADIATION) * stack.getCount();
                totalIgnition += HazardSystem.getHazardLevelFromStack(stack, HazardType.PYROPHORIC) * stack.getCount();
            }
        }

        // Применяем накопленные эффекты 

        // Применение радиации (когда будет система здоровья/радиации)
        if (totalRadiation > 0) {
            // ContaminationUtil.contaminate(player, ..., totalRadiation / 20f);
            // MainRegistry.LOGGER.debug("Player " + player.getName().getString() + " is being irradiated with " + totalRadiation + " RAD/s");
        }

        // Применение поджога
        if (totalIgnition > 0) {
            // Устанавливаем время горения игрока. Уровень опасности используется как длительность в секундах.
            player.setSecondsOnFire((int) totalIgnition);
        }
    }
}