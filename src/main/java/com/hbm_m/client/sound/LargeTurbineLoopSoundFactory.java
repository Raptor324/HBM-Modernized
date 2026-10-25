package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.MachineLargeTurbineBlockEntity;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Original: {@code getLoopedSound("hbm:block.largeTurbineRunning", x, y, z, 1F, 10F, 1F, 20)}; Lautstaerke
 * {@code 0.4 * Drehzahl}, Tonhoehe {@code 0.25 + 0.75 * Drehzahl} ({@code fanAcceleration / 15}), laeuft aus, bis der
 * Laeufer steht.
 */
public final class LargeTurbineLoopSoundFactory {

    private static final double RANGE = 10.0D;

    private LargeTurbineLoopSoundFactory() {}

    public static Object create(MachineLargeTurbineBlockEntity turbine) {
        BlockPos pos = turbine.getBlockPos();
        return new AbstractTickableSoundInstance(HbmSoundsNT.get("hbm:block.largeTurbineRunning"), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.volume = 0.01F;
                this.pitch = 0.25F;
                this.attenuation = Attenuation.LINEAR;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                if (!(level.getBlockEntity(pos) instanceof MachineLargeTurbineBlockEntity live) || live.fanAcceleration <= 0 && !live.isActive()) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                float turbineSpeed = live.fanAcceleration / 15F;
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, 0.4F * turbineSpeed * (1D - d / RANGE));
                this.pitch = 0.25F + 0.75F * turbineSpeed;
            }
        };
    }
}
