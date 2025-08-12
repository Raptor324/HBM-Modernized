package com.hbm_m.sound;

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
        // MainRegistry.LOGGER.info("CLIENT ({}): Received START packet.", pos);
        // Используем computeIfAbsent для атомарной и безопасной операции
        ACTIVE_ASSEMBLER_SOUNDS.computeIfAbsent(pos, key -> {
            // MainRegistry.LOGGER.info("CLIENT ({}): Sound not found in map. Starting new sound instance.", key);
            AssemblerSoundInstance sound = new AssemblerSoundInstance(key);
            Minecraft.getInstance().getSoundManager().play(sound);
            return sound;
        });
    }

    public static void stopAssemblerSound(BlockPos pos) {
        // MainRegistry.LOGGER.info("CLIENT ({}): Received STOP packet.", pos);
        AssemblerSoundInstance sound = ACTIVE_ASSEMBLER_SOUNDS.get(pos);
        if (sound != null) {
            sound.stopSound(); // stopSound теперь сам вызовет onSoundStopped
        } else {
            // MainRegistry.LOGGER.warn("CLIENT ({}): Tried to stop a sound that was not playing.", pos);
        }
    }

    /**
     * Новый метод, который вызывается из AssemblerSoundInstance,
     * когда звук останавливается по любой причине.
     */
    public static void onSoundStopped(BlockPos pos) {
        if (ACTIVE_ASSEMBLER_SOUNDS.remove(pos) != null) {
            // MainRegistry.LOGGER.info("CLIENT ({}): Sound instance stopped and was removed from the active map.", pos);
        }
    }
}