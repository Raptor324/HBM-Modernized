package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.PWRControllerBlockEntity;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Original: {@code getLoopedSound(REACTOR_GEIGER_LOOP, x, y, z, 1F, 10F, 1.0F, 20)} solange Brennstoff geladen ist. */
public final class PWRLoopSoundFactory {

    private static final double RANGE = 10.0D;

    private PWRLoopSoundFactory() {}

    public static Object create(PWRControllerBlockEntity pwr) {
        BlockPos pos = pwr.getBlockPos();
        return new AbstractTickableSoundInstance(ModSounds.REACTOR_LOOP.get(), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.volume = 1F;
                this.pitch = 1F;
                this.attenuation = Attenuation.LINEAR;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof PWRControllerBlockEntity live) || live.amountLoaded <= 0) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                // AudioWrapper.getVolume: linear bis zur Reichweite
                this.volume = (float) Math.max(0D, 1D - d / RANGE);
            }
        };
    }
}
