package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Original: {@code getLoopedSound("hbm:block.turbofanOperate", x, y, z, 1F, 50F, 1F, 20)}; Lautstaerke
 * {@code momentum / 50}, Tonhoehe {@code momentum / 200 + 0.5 + afterburner * 0.16}; laeuft, solange Schwung da ist.
 */
public final class TurbofanLoopSoundFactory {

    private static final double RANGE = 50.0D;

    private TurbofanLoopSoundFactory() {}

    public static Object create(MachineTurbofanBlockEntity turbofan) {
        BlockPos pos = turbofan.getBlockPos();
        return new AbstractTickableSoundInstance(HbmSoundsNT.get("hbm:block.turbofanOperate"), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX();
                this.y = pos.getY();
                this.z = pos.getZ();
                this.looping = true;
                this.delay = 0;
                this.volume = 0.01F;
                this.pitch = 1.0F;
                this.attenuation = Attenuation.NONE;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                if (!(level.getBlockEntity(pos) instanceof MachineTurbofanBlockEntity live) || live.momentum <= 0) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                float base = live.momentum / 50F;
                this.volume = (float) Math.max(0D, base * (1D - d / RANGE));
                this.pitch = live.momentum / 200F + 0.5F + live.afterburner * 0.16F;
            }
        };
    }
}
