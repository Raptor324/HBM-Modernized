package com.hbm_m.sound;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class ClientSoundManager {
    
    // Ключ теперь String, чтобы хранить "координаты_типЗвука"
    private static final Map<String, AbstractTickableSoundInstance> ACTIVE_SOUNDS = new ConcurrentHashMap<>();
    
    private static String getKey(BlockPos pos, String type) {
        return pos.asLong() + "_" + type;
    }

    public static void updateDoorSound(BlockPos pos, String soundType, boolean isMoving, Supplier<AbstractTickableSoundInstance> loopSoundSupplier) {
        String key = getKey(pos, soundType);
        if (isMoving) {
            ACTIVE_SOUNDS.compute(key, (k, existing) -> {
                // На повторном заходе/перезагрузке чанка или re-login инстанс может
                // остаться в map, но SoundManager уже выгрузил его. Проверяем через
                // isActive() — иначе loop не возобновляется.
                if (existing == null || existing.isStopped()
                        || !Minecraft.getInstance().getSoundManager().isActive(existing)) {
                    AbstractTickableSoundInstance newSound = loopSoundSupplier.get();
                    Minecraft.getInstance().getSoundManager().play(newSound);
                    return newSound;
                }
                return existing;
            });
        } else {
            stopSpecificSound(pos, soundType);
        }
    }

    /**
     * Raw-версия для общего кода: позволяет передавать Supplier без упоминания
     * клиентских типов в сигнатурах common-классов.
     */
    @SuppressWarnings("unchecked")
    public static void updateDoorSoundRaw(BlockPos pos, String soundType, boolean isMoving, Supplier<?> loopSoundSupplier) {
        updateDoorSound(pos, soundType, isMoving, (Supplier<AbstractTickableSoundInstance>) loopSoundSupplier);
    }
    
    public static void playOneShotSound(BlockPos pos, SoundEvent sound, float volume) {
        if (sound == null) return;
        SimpleSoundInstance soundInstance = new SimpleSoundInstance(
            sound, SoundSource.BLOCKS, volume, 1.0f, RandomSource.create(),
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5
        );
        Minecraft.getInstance().getSoundManager().play(soundInstance);
    }
    
    public static void stopSpecificSound(BlockPos pos, String soundType) {
        AbstractTickableSoundInstance existingSound = ACTIVE_SOUNDS.remove(getKey(pos, soundType));
        if (existingSound != null) {
            Minecraft.getInstance().getSoundManager().stop(existingSound);
        }
    }

    public static void stopSound(BlockPos pos) {
        // Останавливаем все возможные типы звуков для этой позиции
        stopSpecificSound(pos, "loop1");
        stopSpecificSound(pos, "loop2");
        stopSpecificSound(pos, "machine"); // для совместимости
    }
    
    public static void clearAll() {
        ACTIVE_SOUNDS.values().forEach(sound -> Minecraft.getInstance().getSoundManager().stop(sound));
        ACTIVE_SOUNDS.clear();
    }

    // Метод для старых машин
    public static void updateSound(BlockEntity be, boolean shouldBePlaying, Supplier<? extends AbstractTickableSoundInstance> soundSupplier) {
        updateDoorSound(be.getBlockPos(), "machine", shouldBePlaying, (Supplier<AbstractTickableSoundInstance>) soundSupplier);
    }
}