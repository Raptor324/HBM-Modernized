package com.hbm_m.item.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
// Material больше не используется в 1.20.1, проверяем по свойствам блока

/**
 * Обработчик звуков силовой брони.
 * Портировано из оригинального steppy() метода из ArmorFSB.java
 */
public class PowerArmorSoundHandler {

    private static final String NEXT_STEP_DISTANCE_KEY = "hbm_nextStepDistance";

    /**
     * Воспроизводит звук шага для силовой брони.
     * Аналог оригинального steppy() метода.
     *
     * @param player Игрок
     * @param soundName Имя звука (ResourceLocation.toString())
     */
    public static void playStepSound(Player player, String soundName) {
        try {
            // Используем рефлексию для доступа к приватным полям Entity
            // В 1.20.1 эти поля могут иметь другие имена, но логика та же
            float nextStepDistance = getNextStepDistance(player);
            float distanceWalkedOnStepModified = getDistanceWalkedOnStepModified(player);

            // Инициализируем сохраненное значение если оно не установлено
            if (player.getPersistentData().getFloat(NEXT_STEP_DISTANCE_KEY) == 0) {
                player.getPersistentData().putFloat(NEXT_STEP_DISTANCE_KEY, nextStepDistance);
            }

            // Проверяем блок под ногами
            Level level = player.level();
            BlockPos pos = new BlockPos(
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY() - 0.2D - player.getMyRidingOffset()),
                (int) Math.floor(player.getZ())
            );

            var blockState = level.getBlockState(pos);

            // Если блок не воздух и пора воспроизвести звук
            if (!blockState.isAir() &&
                player.getPersistentData().getFloat(NEXT_STEP_DISTANCE_KEY) <= distanceWalkedOnStepModified) {

                // Воспроизводим звук
                SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.tryParse(soundName));
                if (soundEvent != null) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }

            // Обновляем сохраненное значение
            player.getPersistentData().putFloat(NEXT_STEP_DISTANCE_KEY, nextStepDistance);

        } catch (Exception e) {
            // Игнорируем ошибки рефлексии
        }
    }

    /**
     * Воспроизводит звук прыжка.
     */
    public static void playJumpSound(Player player, String soundName) {
        if (soundName != null && !soundName.isEmpty()) {
            Level level = player.level();
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.tryParse(soundName));
            if (soundEvent != null) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    /**
     * Воспроизводит звук падения.
     */
    public static void playFallSound(Player player, String soundName) {
        if (soundName != null && !soundName.isEmpty()) {
            Level level = player.level();
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.tryParse(soundName));
            if (soundEvent != null) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    /**
     * Получает nextStepDistance через рефлексию.
     * В 1.20.1 поле может называться по-другому.
     */
    private static float getNextStepDistance(Player player) {
        try {
            // Пробуем получить через рефлексию
            var field = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(
                net.minecraft.world.entity.Entity.class, "nextStepDistance");
            return (Float) field.get(player);
        } catch (Exception e) {
            // Fallback: используем значение по умолчанию
            return 0.6F; // Стандартное значение nextStepDistance
        }
    }

    /**
     * Получает distanceWalkedOnStepModified через рефлексию.
     * В 1.20.1 поле может называться по-другому.
     */
    private static float getDistanceWalkedOnStepModified(Player player) {
        try {
            // Пробуем получить через рефлексию
            var field = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(
                net.minecraft.world.entity.Entity.class, "distanceWalkedOnStepModified");
            return (Float) field.get(player);
        } catch (Exception e) {
            // Fallback: используем текущее пройденное расстояние
            return player.walkDistO;
        }
    }
}
