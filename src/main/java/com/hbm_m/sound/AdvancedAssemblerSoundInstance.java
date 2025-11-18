package com.hbm_m.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdvancedAssemblerSoundInstance extends AbstractTickableSoundInstance {

    // Конструктор теперь принимает только BlockPos, так как ему больше не нужно знать о BlockEntity.
    public AdvancedAssemblerSoundInstance(BlockPos pos) {
        super(ModSounds.MOTOR.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());

        this.looping = true;
        this.delay = 0;
        this.relative = false; // Звук находится в мировых координатах
        this.attenuation = Attenuation.LINEAR;

        // Устанавливаем позицию
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    // Этот метод должен быть реализован, но он может быть пустым,
    // так как жизненный цикл теперь управляется извне.
    @Override
    public void tick() { }
}
