package com.hbm_m.sound;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
@OnlyIn(Dist.CLIENT)
public class ClientSoundManager {
    
    // Ключ теперь String, чтобы хранить "координаты_типЗвука"
    private static final Map<String, AbstractTickableSoundInstance> ACTIVE_SOUNDS = new ConcurrentHashMap<>();
    
    private static String getKey(BlockPos pos, String type) {
        return pos.asLong() + "_" + type;
    }

    public static void updateDoorSound(BlockPos pos, String soundType, boolean isMoving, Supplier<AbstractTickableSoundInstance> loopSoundSupplier) {
        updateDoorSound(pos, soundType, isMoving, loopSoundSupplier, IClientSoundAccess.DEFAULT_AUDIBILITY_RANGE);
    }

    /**
     * Distance-gated funnel for ALL machine/door loops: while the local player is
     * beyond {@code maxDistance} the loop is not created (and an existing one is
     * stopped). Beyond that range the sound is inaudible anyway, but on machine
     * farms thousands of far-away tickers kept OpenAL channels alive and stalled
     * the render thread in {@code SoundEngine.play}. Range is honored in world
     * coordinates (Sable ships relocate blocks, so raw BE position would not do).
     * Machines with a non-default hearing radius (turbofan) pass their own range.
     */
    public static void updateDoorSound(BlockPos pos, String soundType, boolean isMoving,
                                       Supplier<AbstractTickableSoundInstance> loopSoundSupplier, double maxDistance) {
        if (!isAudible(pos, maxDistance)) {
            if (isMoving) {
                stopSpecificSound(pos, soundType);
            }
            return;
        }
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
        updateSound(be, shouldBePlaying, soundSupplier, IClientSoundAccess.DEFAULT_AUDIBILITY_RANGE);
    }

    public static void updateSound(BlockEntity be, boolean shouldBePlaying,
                                   Supplier<? extends AbstractTickableSoundInstance> soundSupplier, double maxDistance) {
        updateDoorSound(be.getBlockPos(), "machine", shouldBePlaying, (Supplier<AbstractTickableSoundInstance>) soundSupplier, maxDistance);
    }

    /** Player-distance test in world coordinates (Sable ship positions converted). */
    private static boolean isAudible(BlockPos pos, double maxDistance) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.player.LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return false;
        }
        net.minecraft.world.phys.Vec3 world = com.hbm_m.compat.sable.SableCompat.toWorld(
                mc.level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        return player.distanceToSqr(world.x, world.y, world.z) < maxDistance * maxDistance;
    }
}