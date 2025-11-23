package com.hbm_m.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ClientSoundManager {
    
    // Хранит активные звуки для каждой позиции блока.
    private static final Map<BlockPos, AbstractTickableSoundInstance> ACTIVE_SOUNDS = new ConcurrentHashMap<>();
    
    /**
     * СТАРЫЙ МЕТОД - для обратной совместимости с другими блоками.
     * Используется для простых машин, где звук зациклен пока машина работает.
     */
    public static void updateSound(BlockEntity be, boolean shouldBePlaying,
                               Supplier<? extends AbstractTickableSoundInstance> soundSupplier) {
        BlockPos pos = be.getBlockPos();
        
        if (shouldBePlaying) {
            // Если машина должна играть, но звука нет, создаем и запускаем.
            ACTIVE_SOUNDS.computeIfAbsent(pos, key -> {
                AbstractTickableSoundInstance newSound = soundSupplier.get();
                Minecraft.getInstance().getSoundManager().play(newSound);
                return newSound;
            });
        } else {
            // Если машина НЕ должна играть, но звук есть, останавливаем его.
            AbstractTickableSoundInstance existingSound = ACTIVE_SOUNDS.remove(pos);
            if (existingSound != null) {
                Minecraft.getInstance().getSoundManager().stop(existingSound);
            }
        }
    }

    
    /**
     * НОВЫЙ МЕТОД - для дверей с зацикленным звуком движения.
     * Управляет зацикленным звуком (loop) и разовыми звуками начала/конца.
     * 
     * @param pos - позиция блока
     * @param isMoving - true если дверь в процессе движения (state 2 или 3)
     * @param loopSoundSupplier - поставщик зацикленного звука движения
     * @param volume - громкость
     */
    public static void updateDoorSound(BlockPos pos, boolean isMoving, Supplier<AbstractTickableSoundInstance> loopSoundSupplier) {
        if (isMoving) {
            // Дверь движется - запускаем зацикленный звук если его нет
            ACTIVE_SOUNDS.computeIfAbsent(pos, key -> {
                AbstractTickableSoundInstance newSound = loopSoundSupplier.get();
                Minecraft.getInstance().getSoundManager().play(newSound);
                return newSound;
            });
        } else {
            // Дверь остановилась - останавливаем зацикленный звук
            stopSound(pos);
        }
    }
    
    /**
     * Воспроизводит разовый звук (start/stop).
     * НЕ добавляется в ACTIVE_SOUNDS, так как это не зацикленный звук.
     */
    public static void playOneShotSound(BlockPos pos, SoundEvent sound, float volume) {
        if (sound == null) return;
        
        SimpleSoundInstance soundInstance = new SimpleSoundInstance(
            sound,
            SoundSource.BLOCKS,
            volume,
            1.0f,
            RandomSource.create(),
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5
        );
        
        Minecraft.getInstance().getSoundManager().play(soundInstance);
    }
    
    /**
     * Останавливает звук в указанной позиции.
     */
    public static void stopSound(BlockPos pos) {
        AbstractTickableSoundInstance existingSound = ACTIVE_SOUNDS.remove(pos);
        if (existingSound != null) {
            Minecraft.getInstance().getSoundManager().stop(existingSound);
        }
    }
    
    /**
     * Очистка всех звуков (вызывается при выходе из мира).
     */
    public static void clearAll() {
        ACTIVE_SOUNDS.values().forEach(sound -> 
            Minecraft.getInstance().getSoundManager().stop(sound)
        );
        ACTIVE_SOUNDS.clear();
    }
}
