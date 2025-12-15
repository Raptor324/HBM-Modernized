package com.hbm_m.block.custom.machines.armormod.event;

// Этот класс отвечает за корректировку здоровья игрока при смене брони с модификациями.
import com.hbm_m.lib.RefStrings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArmorModificationServerEvents {

    // Создаем "очередь" игроков, которых нужно проверить.
    // Используем Set, чтобы избежать дубликатов UUID.
    
    private static final Set<UUID> playersToUpdate = new HashSet<>();

    /**
     * ШАГ 1: Ловим смену брони и "помечаем" игрока.
     * Этот метод больше не исправляет здоровье напрямую.
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        // Убеждаемся, что изменился именно слот БРОНИ (а не руки)
        // и что сущность - это игрок.
        if (event.getSlot().getType() == EquipmentSlot.Type.ARMOR && event.getEntity() instanceof Player) {
            // Добавляем UUID игрока в нашу очередь на проверку.
            playersToUpdate.add(event.getEntity().getUUID());
        }
    }

    /**
     * ШАГ 2: В конце каждого серверного тика проверяем нашу очередь.
     * Этот метод выполняет реальную проверку здоровья.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Мы хотим работать только в конце тика, когда все изменения уже применились.
        if (event.phase == TickEvent.Phase.END) {
            // Если в нашей очереди есть игроки...
            if (!playersToUpdate.isEmpty()) {
                // ...проходимся по каждому из них.
                // Используем toArray, чтобы избежать ConcurrentModificationException при удалении.
                for (UUID playerUUID : playersToUpdate.toArray(new UUID[0])) {
                    // Находим игрока на сервере по его UUID.
                    ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUUID);
                    // Если игрок онлайн...
                    if (player != null) {
                        // ...выполняем нашу проверку. К этому моменту getMaxHealth() уже будет правильным.
                        if (player.getHealth() > player.getMaxHealth()) {
                            player.setHealth(player.getMaxHealth());
                        }
                    }
                }
                // Очищаем очередь, чтобы не проверять тех же игроков снова.
                playersToUpdate.clear();
            }
        }
    }
}