package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.MachineIntakeBlockEntity;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Original: {@code getLoopedSound(NTMSounds.ELECTRIC_MOTOR_LOOP, x, y, z, 0.25F, 10F, 1.0F, 20)}, solange der Lufteinlass laeuft. */
public final class IntakeLoopSoundFactory {

    private static final double RANGE = 10.0D;
    private static final float VOLUME = 0.25F;

    private IntakeLoopSoundFactory() {}

    public static Object create(MachineIntakeBlockEntity intake) {
        BlockPos pos = intake.getBlockPos();
        return new AbstractTickableSoundInstance(ModSounds.MOTOR.get(), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.volume = VOLUME;
                this.pitch = 1F;
                this.attenuation = Attenuation.LINEAR;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                if (!(level.getBlockEntity(pos) instanceof MachineIntakeBlockEntity live)
                        || live.getEnergyStored() < MachineIntakeBlockEntity.MAX_POWER / 20) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, VOLUME * (1D - d / RANGE));
            }
        };
    }
}
