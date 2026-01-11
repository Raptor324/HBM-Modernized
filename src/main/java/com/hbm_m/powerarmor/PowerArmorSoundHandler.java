package com.hbm_m.powerarmor;

import com.hbm_m.sound.ModSounds;

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
    private static final String LAST_STEP_SOUND_KEY = "hbm_lastStepSound";
    private static final long STEP_SOUND_COOLDOWN = 300; // 300ms между звуками

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

            // Проверяем cooldown с учетом скорости движения
            long currentTime = System.currentTimeMillis();
            long lastStepSound = player.getPersistentData().getLong(LAST_STEP_SOUND_KEY);

            // Вычисляем скорость движения
            double speed = Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x +
                                   player.getDeltaMovement().z * player.getDeltaMovement().z);

            // Интервал зависит от скорости: быстрая ходьба - меньший интервал, медленная - больший
            long speedAdjustedCooldown;
            if (speed < 0.005) {
                // Полная остановка - очень большой интервал или не воспроизводить вообще
                speedAdjustedCooldown = 2000; // 2 секунды
            } else if (speed < 0.02) {
                // Очень медленная ходьба - увеличенный интервал
                speedAdjustedCooldown = (long) (STEP_SOUND_COOLDOWN * (1.0 + (0.02 - speed) / 0.02 * 1.5));
            } else {
                // Нормальная и быстрая ходьба - уменьшение интервала
                speedAdjustedCooldown = (long) (STEP_SOUND_COOLDOWN * (1.0 - Math.min((speed - 0.02) / 0.13, 1.0) * 0.67));
            }

            // Ограничение минимального интервала
            if (speedAdjustedCooldown < 100) speedAdjustedCooldown = 100;


            if ((currentTime - lastStepSound) > speedAdjustedCooldown) {


                // Получаем уровень для воспроизведения звука
                Level level = player.level();

                // Воспроизводим звук - используем ModSounds для зарегистрированных звуков
                SoundEvent soundEvent = getSoundEvent(soundName);

                if (soundEvent != null) {

                    // Попробуем другой способ воспроизведения звука
                    if (level.isClientSide) {
                        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(soundEvent, 1.0F)
                        );
                    } else {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }

                    // Обновляем время последнего воспроизведения
                    player.getPersistentData().putLong(LAST_STEP_SOUND_KEY, currentTime);
                } else {

                }
            }

            // Обновляем сохраненное значение
            player.getPersistentData().putFloat(NEXT_STEP_DISTANCE_KEY, nextStepDistance);

        } catch (Exception e) {

            System.err.println("Error playing step sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Воспроизводит звук прыжка.
     */
    public static void playJumpSound(Player player, String soundName) {
        SoundEvent soundEvent = getSoundEvent(soundName);
        if (soundEvent != null) {
            Level level = player.level();
            if (level.isClientSide) {
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(soundEvent, 1.0F)
                );
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    /**
     * Воспроизводит звук падения.
     */
    public static void playFallSound(Player player, String soundName) {
        SoundEvent soundEvent = getSoundEvent(soundName);
        if (soundEvent != null) {
            Level level = player.level();
            if (level.isClientSide) {
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(soundEvent, 1.0F)
                );
            } else {
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

    /**
     * Получает SoundEvent из ModSounds по имени.
     * Поддерживает основные звуки силовой брони.
     */
    private static SoundEvent getSoundEvent(String soundName) {
        if (soundName == null) return null;

        // Убираем префикс моды если есть
        if (soundName.startsWith("hbm_m:")) {
            soundName = soundName.substring(6);
        }

        SoundEvent result = switch (soundName) {
            case "step.powered" -> com.hbm_m.sound.ModSounds.STEP_POWERED.get();
            case "step.metal" -> com.hbm_m.sound.ModSounds.STEP_METAL.get();
            case "step.iron_jump" -> com.hbm_m.sound.ModSounds.STEP_IRON_JUMP.get();
            case "step.iron_land" -> com.hbm_m.sound.ModSounds.STEP_IRON_LAND.get();
            // Можно добавить другие звуки по мере необходимости
            default -> null;
        };

        return result;
    }
}
