package com.hbm_m.client.sound;

import com.hbm_m.block.entity.machines.MachineChungusBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Клиентский цикл звука турбины Chungus — вынесен из {@link MachineChungusBlockEntity}, чтобы
 * серверный класс BE не содержал в bytecode ссылок на {@link AbstractTickableSoundInstance}.
 * Pitch/Volume зависят от Flywheel-Spin (0..1), wie bei {@code TurbineLoopSoundFactory}.
 */
public final class ChungusLoopSoundFactory {

    private static final double MAX_AUDIBLE_DISTANCE = 48.0;

    private ChungusLoopSoundFactory() {}

    public static Object create(MachineChungusBlockEntity turbine, SoundEvent sound) {
        BlockPos pos = turbine.getBlockPos();
        return new AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.attenuation = Attenuation.LINEAR;
                applySpin(turbine.getSpin());
            }

            private void applySpin(double spin) {
                float spinNum = (float) Math.min(1.0D, spin * 2.0D);
                this.volume = 0.25F + spinNum * 0.75F;
                this.pitch = 0.5F + spinNum * 0.5F;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) {
                    this.stop();
                    return;
                }

                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof MachineChungusBlockEntity live) || live.getSpin() <= 0.0D) {
                    this.stop();
                    return;
                }

                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null || player.distanceToSqr(this.x, this.y, this.z) > MAX_AUDIBLE_DISTANCE * MAX_AUDIBLE_DISTANCE) {
                    this.stop();
                    return;
                }

                applySpin(live.getSpin());
            }
        };
    }
}
