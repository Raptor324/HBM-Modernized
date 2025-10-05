package com.hbm_m.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
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

    public static void updateSound(BlockEntity be, boolean shouldBePlaying, Supplier<AbstractTickableSoundInstance> soundSupplier) {
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
                // Это не скомпилируется, так как stop() недоступен.
                // Вместо этого мы просто перестаем его хранить,
                // и сборщик мусора его удалит. Звук остановится сам,
                // так как его больше никто не "тикает".
                // В новых версиях Minecraft управление звуками происходит иначе,
                // и нам нужно явно его остановить, но через SoundManager.
                Minecraft.getInstance().getSoundManager().stop(existingSound);
            }
        }
    }
}
