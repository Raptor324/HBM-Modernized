package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Original {@code TileEntitySoyuzLauncher#createAudioLoop}: {@code getLoopedSound("hbm:block.soyuzReady", x, y, z, 2.0F, 100F, 1.0F)},
 * laeuft waehrend des Countdowns.
 */
public final class SoyuzReadyLoopSoundFactory {

    private static final double RANGE = 100.0D;

    private SoyuzReadyLoopSoundFactory() {}

    public static Object create(SoyuzLauncherBlockEntity launcher) {
        BlockPos pos = launcher.getBlockPos();
        return new AbstractTickableSoundInstance(HbmSoundsNT.get("hbm:block.soyuzReady"), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX();
                this.y = pos.getY();
                this.z = pos.getZ();
                this.looping = true;
                this.delay = 0;
                this.volume = 1F;
                this.pitch = 1F;
                this.attenuation = Attenuation.NONE;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                if (!(level.getBlockEntity(pos) instanceof SoyuzLauncherBlockEntity live) || !live.isStarting() || live.getCountdown() <= 0) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, 1D - d / RANGE);
            }
        };
    }
}
