package com.hbm_m.powerarmor;

import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.packets.PowerArmorDashPacket;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Обработчик системы движения силовой брони.
 * Реализует step height и dash count с пакетной синхронизацией.
 */
@Mod.EventBusSubscriber
public class PowerArmorMovementHandler {

    /**
     * Обработка движения игрока - установка step height для силовой брони
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Player player = event.player;

        // Проверяем, носит ли игрок силовую броню
        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            // Сбрасываем step height до значения по умолчанию, если игрок не носит броню
            if (player.maxUpStep() > 0.6F) {
                player.setMaxUpStep(0.6F);
            }
            return;
        }

        // Получаем нагрудник как контроллер сета
        var chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestplate.getItem() instanceof ModPowerArmorItem armorItem)) {
            return;
        }

        var specs = armorItem.getSpecs();

        // Устанавливаем step height
        float stepHeight = specs.stepHeight;
        if (stepHeight > 0) {
            player.setMaxUpStep(Math.max(0.6F, stepHeight)); // Минимум 0.6F (значение по умолчанию)
        }

        // Dash система реализована в ModConfigKeybindHandler и performDash()
    }

    /**
     * Выполняет рывок (dash) для игрока в силовой броне
     */
    public static void performDash(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Проверяем, носит ли игрок силовую броню с dash
        if (!ModPowerArmorItem.hasFSBArmor(player)) return;

        var chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestplate.getItem() instanceof ModPowerArmorItem armorItem)) return;

        var specs = armorItem.getSpecs();
        if (specs.dashCount <= 0) return;

        // Получаем направление взгляда игрока
        Vec3 lookDirection = player.getLookAngle();

        // Вычисляем скорость рывка (в зависимости от dash count)
        double dashSpeed = 1.5 + (specs.dashCount * 0.5); // Базовая скорость + бонус от dash count

        // Применяем импульс
        Vec3 dashVelocity = lookDirection.scale(dashSpeed);
        player.setDeltaMovement(dashVelocity.x, Math.max(dashVelocity.y, 0.2), dashVelocity.z); // Минимум вверх 0.2

        // Синхронизируем движение с клиентами, отслеживающими этого игрока
        ModPacketHandler.INSTANCE.send(
            PacketDistributor.TRACKING_ENTITY.with(() -> player),
            new PowerArmorDashPacket(player.getId(), dashVelocity)
        );

        // TODO: Добавить кулдаун для dash
        // TODO: Потреблять энергию при dash
    }
}

