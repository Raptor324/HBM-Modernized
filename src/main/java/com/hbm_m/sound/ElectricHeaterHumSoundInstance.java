package com.hbm_m.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/**
 * Гул работающего электрического нагревателя — порт
 * {@code TileEntityHeaterElectric.createAudioLoop} (оригинал: ELECTRIC_HUM_LOOP,
 * громкость 0.25, радиус 7.5, питч 1.0; attenuation LINEAR по радиусу).
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?}
public class ElectricHeaterHumSoundInstance extends AbstractTickableSoundInstance {

    public ElectricHeaterHumSoundInstance(BlockPos pos) {
        super(ModSounds.ELECTRIC_HUM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        this.volume = 0.25F;
        this.pitch = 1.0F;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    @Override
    public void tick() {
        // Жизненный цикл управляется через ClientSoundManager.updateSound
    }
}
