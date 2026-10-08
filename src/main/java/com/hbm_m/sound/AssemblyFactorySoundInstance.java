package com.hbm_m.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}

/** Original {@code TileEntityMachineAssemblyFactory.createAudioLoop}: ELECTRIC_MOTOR_LOOP, Lautstaerke 0.5, Tonhoehe 0.75. */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
public class AssemblyFactorySoundInstance extends AbstractTickableSoundInstance {

    public AssemblyFactorySoundInstance(BlockPos pos) {
        super(ModSounds.MOTOR.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        this.volume = 0.5F;
        this.pitch = 0.75F;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    @Override
    public void tick() {
    }
}
