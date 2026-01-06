package com.hbm_m.event;

import com.hbm_m.item.armor.ModPowerArmorItem;
import com.hbm_m.item.armor.PowerArmorSoundHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Обработчик событий для звуков силовой брони.
 * Портировано из оригинальных handleTick, handleJump, handleFall методов ArmorFSB.java
 */
@Mod.EventBusSubscriber
public class PowerArmorSoundEventHandler {

    /**
     * Обработка ходьбы - аналог handleTick из ArmorFSB
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        // Проверяем полный сет FSB
        if (!ModPowerArmorItem.hasFSBArmor(player)) return;

        // Получаем нагрудник как контроллер сета
        var chestStack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;

        var specs = armorItem.getSpecs();

        // Проверка на специальные игроки (аналог ShadyUtil)
        boolean shouldPlayStepSound = true;
        // TODO: добавить проверку для специальных игроков если нужно

        // Звук шага
        if (shouldPlayStepSound && specs.stepSound != null && player.onGround()) {
            PowerArmorSoundHandler.playStepSound(player, specs.stepSound);
        }
    }

    /**
     * Обработка прыжка - аналог handleJump из ArmorFSB
     */
    @SubscribeEvent
    public static void onPlayerJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        if (!ModPowerArmorItem.hasFSBArmor(player)) return;

        var chestStack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;

        var specs = armorItem.getSpecs();

        if (specs.jumpSound != null) {
            PowerArmorSoundHandler.playJumpSound(player, specs.jumpSound);
        }
    }

    /**
     * Обработка падения - аналог handleFall из ArmorFSB
     */
    @SubscribeEvent
    public static void onPlayerFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        if (!ModPowerArmorItem.hasFSBArmor(player)) return;

        var chestStack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;

        var specs = armorItem.getSpecs();

        // Hard Landing - AOE урон при падении
        if (specs.hasHardLanding && event.getDistance() > 10) {
            performHardLanding(player, event.getDistance());
        }

        // Звук падения
        if (specs.fallSound != null) {
            PowerArmorSoundHandler.playFallSound(player, specs.fallSound);
        }
    }

    /**
     * Выполняет Hard Landing эффект - AOE урон вокруг игрока при падении.
     * Портировано из оригинального ArmorFSB.java
     */
    private static void performHardLanding(Player player, float fallDistance) {
        var level = player.level();
        var entities = level.getEntities(player, player.getBoundingBox().inflate(3, 0, 3));

        for (var entity : entities) {
            if (entity == player) continue; // Не дамагим самого игрока
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity) continue; // Не дамагим дроп

            // Вычисляем расстояние
            var vec = entity.position().subtract(player.position());
            double distance = vec.length();

            if (distance < 3) {
                // Интенсивность урона зависит от расстояния
                double intensity = 3 - distance;
                float damage = (float) (intensity * 10);

                // Наносим урон
                entity.hurt(level.damageSources().playerAttack(player), damage);

                // Отбрасываем сущность
                if (intensity > 0) {
                    var motionVec = vec.normalize().scale(-2 * intensity);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(motionVec.x, 0.1D * intensity, motionVec.z));
                }
            }
        }
    }
}

