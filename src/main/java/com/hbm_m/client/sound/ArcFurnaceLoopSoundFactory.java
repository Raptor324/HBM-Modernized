package com.hbm_m.client.sound;

import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Original {@code getLoopedSound}: Deckelmotor {@code hbm:door.wgh_start} (0,75, Reichweite 15) und Brummen
 * {@code hbm:block.electricHum} (1,5, Tonhoehe 0,75) des grossen Lichtbogenofens. Gestoppt wird ueber den
 * {@code ClientSoundManager}.
 */
public final class ArcFurnaceLoopSoundFactory {

    private ArcFurnaceLoopSoundFactory() {}

    public static Object create(BlockPos pos, String sound, float volume, float pitch) {
        return new AbstractTickableSoundInstance(HbmSoundsNT.get(sound), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.volume = volume;
                this.pitch = pitch;
                this.attenuation = SoundInstance.Attenuation.LINEAR;
            }

            @Override
            public void tick() { }
        };
    }
}
