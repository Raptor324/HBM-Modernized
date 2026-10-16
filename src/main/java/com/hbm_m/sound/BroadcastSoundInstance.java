package com.hbm_m.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/**
 * Permanenter Loop-Sound des Broadcasters, solange der Block existiert.
 * Lebenszyklus wird extern ueber ClientSoundManager/ClientSoundBootstrap verwaltet.
 */
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}
@OnlyIn(Dist.CLIENT)
public class BroadcastSoundInstance extends AbstractTickableSoundInstance {

    private final BlockPos origin;

    public BroadcastSoundInstance(BlockPos pos) {
        super(ModSounds.BROADCAST_RANDOM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = false;
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

    @Override
    public void tick() {
        updatePosition();
    }
}
