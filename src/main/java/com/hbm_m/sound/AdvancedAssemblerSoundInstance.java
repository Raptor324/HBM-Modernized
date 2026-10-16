package com.hbm_m.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}
@OnlyIn(Dist.CLIENT)
public class AdvancedAssemblerSoundInstance extends AbstractTickableSoundInstance {

    private final BlockPos origin;

    // Конструктор теперь принимает только BlockPos, так как ему больше не нужно знать о BlockEntity.
    public AdvancedAssemblerSoundInstance(BlockPos pos) {
        super(ModSounds.MOTOR.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());

        this.looping = true;
        this.delay = 0;
        this.relative = false; // Звук находится в мировых координатах
        this.attenuation = Attenuation.LINEAR;
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

    // Жизненный цикл управляется извне (AdvancedAssemblerClientTicker);
    // здесь только перепривязка позиции - корабль Sable плывёт.
    @Override
    public void tick() {
        updatePosition();
    }
}
