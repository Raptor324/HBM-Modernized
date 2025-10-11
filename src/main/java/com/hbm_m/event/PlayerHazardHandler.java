package com.hbm_m.event;

// Обработчик для применения эффектов опасностей (радиация, пирофорность) к игроку на основе его инвентаря.
// Эффекты применяются каждый тик на сервере, если игрок не в креативе или режиме наблюдателя.
// Используется в классе MainRegistry для регистрации на событие PlayerTickEvent.

import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.HazardType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlayerHazardHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Выполняем только на сервере и в конце тика
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
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