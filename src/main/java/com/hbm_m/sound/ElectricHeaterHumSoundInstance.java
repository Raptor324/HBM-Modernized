package com.hbm_m.sound;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/**
 * Гул работающего электрического нагревателя — порт
 * {@code TileEntityHeaterElectric.createAudioLoop} (оригинал: ELECTRIC_HUM_LOOP,
 * громкость 0.25, радиус 7.5, питч 1.0; attenuation LINEAR по радиусу).
 */
@OnlyIn(Dist.CLIENT)
public class ElectricHeaterHumSoundInstance extends AbstractTickableSoundInstance {

    private final BlockPos origin;

    public ElectricHeaterHumSoundInstance(BlockPos pos) {
        super(ModSounds.ELECTRIC_HUM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        this.volume = 0.25F;
        this.pitch = 1.0F;
        this.origin = pos;
        updatePosition();
    }

    // Sable ships move - re-anchor in world space every tick (raw plot-grid
    // coords are millions of blocks out and vanilla mutes the loop).
    private void updatePosition() {
        net.minecraft.world.phys.Vec3 world = SableSoundPosition.project(origin, 0.5D);
        this.x = world.x;
        this.y = world.y;
        this.z = world.z;
    }

    @Override
    public void tick() {
        updatePosition();
    }
}
