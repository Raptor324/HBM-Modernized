package com.hbm_m.item.custom.radiation_meter;

// Предмет-дозиметр для измерения радиации в окружающей среде.
// Показывает уровень радиации в чате при использовании и издает звуки щелчков в зависимости от уровня радиации.

import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.sounds.GeigerSoundPacket;
import com.hbm_m.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;


public class ItemDosimeter extends AbstractRadiationMeterItem {

    private static final Random RANDOM = new Random();
    private int soundTickCounter = 0;

    public ItemDosimeter(Properties pProperties) {
        super(pProperties);
    }
    /** Локальный метод для получения цвета, ограниченный зеленым и желтым.
     * @param rads Уровень радиации
     * @return Код цвета ("§a" или "§e")
     */

    private String getDosimeterRadColor(float rads) {
        if (rads == 0.0f) return "§a"; // Зеленый для низких значений
        return "§6"; // Оранжевый для всех остальных
    }

    /**
     * Создает и форматирует сообщение для вывода в чат.
     * Если радиация >= 3.6, выводит ">3.6 RAD/s".
     * @param data Данные об измеренной радиации.
     * @return Компонент сообщения для отправки игроку.
     */

    @Nonnull
    @Override
    protected Component createUsageMessage(RadiationData data) {
        float totalEnvironmentRad = data.getTotalEnvironmentRad();
        String envRadStr;
        String colorCode = getDosimeterRadColor(totalEnvironmentRad);

        // Проверяем уровень радиации
        if (totalEnvironmentRad < 3.6f) {
            // Если радиация в пределах нормы для прибора, показываем точное значение
            envRadStr = colorCode + String.format("%.1f RAD/s", totalEnvironmentRad);
        } else {
            // Если радиация превышает порог, показываем "зашкаливание"
            // Используем getRadColor для соответствующего цвета и ключ локализации для текста
            envRadStr = colorCode + Component.translatable("item.hbm_m.meter.rads_over_limit", "3.6").getString();
        }
        
        String titleString = "\n§6===== ☢ " + Component.translatable("item.hbm_m.meter.dosimeter.name").getString() + " ☢ =====\n";
        MutableComponent message = Component.translatable("item.hbm_m.meter.title_format", titleString);
        
        // 3. Добавляем единственную строку данных. Теперь она не будет иметь лишнего отступа или "RAD/c"
        message.append(Component.translatable("item.hbm_m.meter.env_rads", envRadStr));

        return message;
    }

    /**
     * Обработчик тиков в инвентаре для проигрывания звуков.
     * Скопирован из GeigerCounterItem и вызывает измененный метод playDosimeterTickSound.
     */
    @Override
    public void inventoryTick(@Nonnull ItemStack pStack, @Nonnull Level pLevel, @Nonnull Entity pEntity, int pSlotId, boolean pIsSelected) {
        if (!pLevel.isClientSide() && pEntity instanceof ServerPlayer serverPlayer && serverPlayer.isAlive()) {
            soundTickCounter++;
            final int SOUND_INTERVAL_TICKS = 5; // Звук проигрывается раз в 5 тиков (четверть секунды)
            if (soundTickCounter >= SOUND_INTERVAL_TICKS) {
                soundTickCounter = 0;

                RadiationData data = measureRadiation(pLevel, serverPlayer);
                float totalEnvironmentRads = data.getTotalEnvironmentRad();

                // Вызываем специфичный для дозиметра метод проигрывания звука
                playDosimeterTickSound(serverPlayer, totalEnvironmentRads);
            }
        }
    }

    /**
     * Проигрывает звук тика дозиметра в зависимости от уровня радиации.
     * Логика основана на GeigerCounterItem, но ограничена звуками 1 и 2 уровня.
     * @param player Игрок, для которого проигрывается звук.
     * @param radiationLevel Текущий уровень радиации.
     */

    private void playDosimeterTickSound(ServerPlayer player, float radiationLevel) {
        int soundIndex = 0;
        List<Integer> soundOptions = new ArrayList<>();

        // Ограничиваем выбор звуков
        if (radiationLevel > 0) {
            // Всегда есть шанс проиграть базовый щелчок, если есть радиация
            if (radiationLevel < 10) soundOptions.add(1);
            // Более интенсивный щелчок добавляется только при радиации > 5 RAD/s
            if (radiationLevel > 5) soundOptions.add(2);
            if (radiationLevel > 15) soundOptions.add(3);

            if (!soundOptions.isEmpty()) {
                soundIndex = soundOptions.get(RANDOM.nextInt(soundOptions.size()));
            }
        } else if (RANDOM.nextInt(50) == 0) {
            soundIndex = 1; // Редкий фоновый щелчок даже без радиации
        }

        // Этот блок остается таким же, т.к. он универсален
        Optional<RegistryObject<SoundEvent>> soundRegistryObject = switch (soundIndex) {
            case 1 -> Optional.of(ModSounds.GEIGER_1);
            case 2 -> Optional.of(ModSounds.GEIGER_2);
            case 3 -> Optional.of(ModSounds.GEIGER_3);
            // Остальные кейсы просто никогда не будут вызваны
            default -> Optional.empty();
        };

        soundRegistryObject.ifPresent(regObject -> {
            SoundEvent soundEvent = regObject.get();
            if (soundEvent != null) {
                ResourceLocation soundLocation = soundEvent.getLocation();
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new GeigerSoundPacket(soundLocation, 0.4F, 1.0F));
            }
        });
    }
}