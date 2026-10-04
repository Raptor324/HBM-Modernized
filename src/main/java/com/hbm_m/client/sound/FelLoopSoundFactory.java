package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.MachineFelBlockEntity;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Original: {@code getLoopedSound("hbm:block.fel", x, y, z, 2.0F, 10F, 2.0F)}; Tonhoehe {@code (audioDuration - 10) / 100 + 0.5},
 * laeuft, solange {@code audioDuration > 10}.
 */
public final class FelLoopSoundFactory {

    private static final double RANGE = 10.0D;

    private FelLoopSoundFactory() {}

    public static Object create(MachineFelBlockEntity fel) {
        BlockPos pos = fel.getBlockPos();
        return new AbstractTickableSoundInstance(ModSounds.FEL_LOOP.get(), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.volume = 0.01F;
                this.pitch = 0.5F;
                this.attenuation = Attenuation.LINEAR;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                if (!(level.getBlockEntity(pos) instanceof MachineFelBlockEntity live) || live.getAudioDuration() <= 10) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, 2F * (1D - d / RANGE));
                this.pitch = (live.getAudioDuration() - 10) / 100F + 0.5F;
            }
        };
    }
}
