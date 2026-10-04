package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.MachineVacuumDistillBlockEntity;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Original: {@code getLoopedSound("hbm:block.boiler", x, y, z, 0.25F, 15F, 1.0F, 20)}. */
public final class VacuumDistillLoopSoundFactory {

    private static final double RANGE = 15.0D;

    private VacuumDistillLoopSoundFactory() {}

    public static Object create(MachineVacuumDistillBlockEntity distill) {
        BlockPos pos = distill.getBlockPos();
        return new AbstractTickableSoundInstance(HbmSoundsNT.get("hbm:block.boiler"), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.volume = 0.01F;
                this.pitch = 1.0F;
                this.attenuation = Attenuation.LINEAR;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                if (!(level.getBlockEntity(pos) instanceof MachineVacuumDistillBlockEntity live) || !live.isAudible()) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, 0.25F * (1D - d / RANGE));
            }
        };
    }
}
