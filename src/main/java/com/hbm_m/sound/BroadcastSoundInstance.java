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
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
@Environment(EnvType.CLIENT)*///?}
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
public class BroadcastSoundInstance extends AbstractTickableSoundInstance {

    public BroadcastSoundInstance(BlockPos pos) {
        super(ModSounds.BROADCAST_RANDOM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
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
        // Leere Implementierung - Lebenszyklus wird extern verwaltet
    }
}
