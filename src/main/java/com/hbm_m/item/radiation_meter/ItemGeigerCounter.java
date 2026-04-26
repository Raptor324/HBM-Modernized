package com.hbm_m.item.radiation_meter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.RadiationDataPacket;
import com.hbm_m.network.sounds.GeigerSoundPacket;
import com.hbm_m.sound.ModSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

// Предмет-геигер для измерения радиации в окружающей среде и на игроке.
// Показывает уровень радиации в чате при использовании и издает звуки щелчков в зависимости от уровня радиации.
// Используется на сервере, отправляет данные на клиент для звуков и HUD.

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

public class ItemGeigerCounter extends AbstractRadiationMeterItem {

    private static final Random RANDOM = new Random();
    private int soundTickCounter = 0;

    public ItemGeigerCounter(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected Component createUsageMessage(RadiationData data) {
        // Создаем цветные строки для каждого значения, ВКЛЮЧАЯ единицы измерения
        String chunkRadStr = getRadColor(data.chunkRad()) + String.format("%.1f RAD/s", data.chunkRad());
        String envRadStr = getRadColor(data.getTotalEnvironmentRad()) + String.format("%.1f RAD/s\n", data.getTotalEnvironmentRad());
        String playerRadStr = getRadColor(data.playerRad()) + String.format("%.1f RAD", data.playerRad());
        // Форматируем процент с двумя знаками после запятой (например, "99.05%")
        String protectionPercentStr = String.format("%.2f%%", data.protectionPercent() * 100);
        // Форматируем абсолютное значение с тремя знаками после запятой (например, "2.021")
        String protectionAbsoluteStr = String.format("%.3f", data.protectionAbsolute());

        // 2. Собираем заголовок. §e - желтый цвет
        String titleString = "\n§6===== ☢ " + Component.translatable("item.hbm_m.meter.geiger_counter.name").getString() + " ☢ =====\n";
        MutableComponent message = Component.translatable("item.hbm_m.meter.title_format", titleString);

        // 3. Добавляем строки данных, используя унифицированные ключи
        message.append(Component.translatable("item.hbm_m.meter.chunk_rads", chunkRadStr));
        message.append(Component.translatable("item.hbm_m.meter.env_rads", envRadStr));
        message.append(Component.translatable("item.hbm_m.meter.player_rads", playerRadStr));
        message.append(Component.translatable("item.hbm_m.meter.protection", protectionPercentStr, protectionAbsoluteStr));

        return message;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack pStack, @NotNull Level pLevel, @NotNull Entity pEntity, int pSlotId, boolean pIsSelected) {
        if (!pLevel.isClientSide() && pEntity instanceof ServerPlayer serverPlayer && serverPlayer.isAlive()) {
            soundTickCounter++;
            final int SOUND_INTERVAL_TICKS = 5;
            if (soundTickCounter >= SOUND_INTERVAL_TICKS) {
                soundTickCounter = 0;

                RadiationData data = measureRadiation(pLevel, serverPlayer);
                float totalEnvironmentRads = data.getTotalEnvironmentRad();
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("GeigerCounter: chunkRad = {}, invRad = {}, totalEnvironmentRads = {}", 
                    data.chunkRad(), data.inventoryRad(), totalEnvironmentRads);
                }

                if (!serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
                    ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new RadiationDataPacket(data.getTotalEnvironmentRad(), data.playerRad()));
                }

                playGeigerTickSound(serverPlayer, totalEnvironmentRads);
            }
        }
    }
    
    private void playGeigerTickSound(ServerPlayer player, float radiationLevel) {
        int soundIndex = 0;
        List<Integer> soundOptions = new ArrayList<>();

        if (radiationLevel > 0) {
            if (radiationLevel < 10) soundOptions.add(1);
            if (radiationLevel > 5 && radiationLevel < 15) soundOptions.add(2);
            if (radiationLevel > 10 && radiationLevel < 20) soundOptions.add(3);
            if (radiationLevel > 15 && radiationLevel < 25) soundOptions.add(4);
            if (radiationLevel > 20 && radiationLevel < 30) soundOptions.add(5);
            if (radiationLevel > 25) soundOptions.add(6);

            if (!soundOptions.isEmpty()) {
                soundIndex = soundOptions.get(RANDOM.nextInt(soundOptions.size()));
            }
        } else if (RANDOM.nextInt(50) == 0) {
            soundIndex = 1; // Редкий фоновый щелчок
        }

        Optional<SoundEvent> sound = switch (soundIndex) {
            case 1 -> Optional.of(ModSounds.GEIGER_1.get());
            case 2 -> Optional.of(ModSounds.GEIGER_2.get());
            case 3 -> Optional.of(ModSounds.GEIGER_3.get());
            case 4 -> Optional.of(ModSounds.GEIGER_4.get());
            case 5 -> Optional.of(ModSounds.GEIGER_5.get());
            case 6 -> Optional.of(ModSounds.GEIGER_6.get());
            default -> Optional.empty();
        };

        sound.ifPresent(soundEvent -> {
            ResourceLocation soundLocation = soundEvent.getLocation();
            ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new GeigerSoundPacket(soundLocation, 0.4F, 1.0F));
        });
    }
}