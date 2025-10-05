package com.hbm_m.sound;

// Звук, воспроизводимый Ассемблером во время работы.
// Использует AbstractTickableSoundInstance для управления воспроизведением и остановкой звука.

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AssemblerSoundInstance extends AbstractTickableSoundInstance {

    public AssemblerSoundInstance(BlockPos pos) {
        super(ModSounds.ASSEMBLER_OPERATE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    @Override
    public void tick() {
    }
}