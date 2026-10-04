package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Original: {@code getLoopedSound("hbm:block.turbinegasRunning", x, y, z, 1F, 20F, 2F, 20)}; Lautstaerke 2,
 * Tonhoehe {@code 0.55 + 0.1 * rpm / 10}; laeuft, solange die Drehzahl mindestens 10 ist und die Turbine nicht anlaeuft.
 */
public final class TurbineGasLoopSoundFactory {

    private static final double RANGE = 20.0D;

    private TurbineGasLoopSoundFactory() {}

    public static Object create(MachineTurbineGasBlockEntity turbine) {
        BlockPos pos = turbine.getBlockPos();
        return new AbstractTickableSoundInstance(HbmSoundsNT.get("hbm:block.turbinegasRunning"), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.volume = 0.01F;
                this.pitch = 2.0F;
                this.attenuation = Attenuation.LINEAR;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                if (!(level.getBlockEntity(pos) instanceof MachineTurbineGasBlockEntity live) || live.rpm < 10 || live.state == -1) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, 2F * (1D - d / RANGE));
                this.pitch = (float) (0.55 + 0.1 * live.rpm / 10);
            }
        };
    }
}
