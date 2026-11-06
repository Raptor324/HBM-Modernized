package com.hbm_m.sound;


import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
// Звук, воспроизводимый Ассемблером во время работы.
// Использует AbstractTickableSoundInstance для управления воспроизведением и остановкой звука.

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
@OnlyIn(Dist.CLIENT)
public class AssemblerSoundInstance extends AbstractTickableSoundInstance {

    private final BlockPos origin;

    public AssemblerSoundInstance(BlockPos pos) {
        super(ModSounds.ASSEMBLER_OPERATE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
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