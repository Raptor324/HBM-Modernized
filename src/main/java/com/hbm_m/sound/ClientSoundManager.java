package com.hbm_m.sound;

// Менеджер звуков на клиенте. Управляет воспроизведением и остановкой звуков машин.
// Использует карту для отслеживания активных звуков по позициям блоков.

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ClientSoundManager {
    private static final Map<BlockPos, AssemblerSoundInstance> ACTIVE_ASSEMBLER_SOUNDS = new ConcurrentHashMap<>();

    public static void playAssemblerSound(BlockPos pos) {

        ACTIVE_ASSEMBLER_SOUNDS.computeIfAbsent(pos, key -> {
            AssemblerSoundInstance sound = new AssemblerSoundInstance(key);
            Minecraft.getInstance().getSoundManager().play(sound);
            return sound;
        });
    }

    public static void stopAssemblerSound(BlockPos pos) {
        AssemblerSoundInstance sound = ACTIVE_ASSEMBLER_SOUNDS.get(pos);
        if (sound != null) {
            sound.stopSound(); // stopSound теперь сам вызовет onSoundStopped
        }
    }

    /**
     * Новый метод, который вызывается из AssemblerSoundInstance,
     * когда звук останавливается по любой причине.
     */
    public static void onSoundStopped(BlockPos pos) {
        if (ACTIVE_ASSEMBLER_SOUNDS.remove(pos) != null) {
        }
    }
}