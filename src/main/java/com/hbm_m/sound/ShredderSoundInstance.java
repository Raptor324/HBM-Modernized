package com.hbm_m.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Звук, воспроизводимый Шреддером во время работы.
 * Использует кастомный звук из мода (как Advanced Assembler использует MOTOR).
 * Использует AbstractTickableSoundInstance для управления воспроизведением и остановкой звука.
 */
@OnlyIn(Dist.CLIENT)
public class ShredderSoundInstance extends AbstractTickableSoundInstance {

    public ShredderSoundInstance(BlockPos pos) {
        super(ModSounds.SHREDDER.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    @Override
    public void tick() {
        // Пустая реализация - жизненный цикл управляется извне через ClientSoundManager
    }
}

