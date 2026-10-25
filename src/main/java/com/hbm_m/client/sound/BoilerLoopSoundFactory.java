package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.MachineBoilerBlockEntity;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Original: {@code getLoopedSound(BOILER_LOOP, x, y, z, 0.125F, 10F, 1F, 20)}, laeuft eine Sekunde nach dem Kochen nach. */
public final class BoilerLoopSoundFactory {

    private static final double RANGE = 10.0D;
    private static final float VOLUME = 0.125F;

    private BoilerLoopSoundFactory() {}

    public static Object create(MachineBoilerBlockEntity boiler) {
        BlockPos pos = boiler.getBlockPos();
        return new AbstractTickableSoundInstance(HbmSoundsNT.get("hbm:block.boiler"), SoundSource.BLOCKS, RandomSource.create()) {
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
                if (!(level.getBlockEntity(pos) instanceof MachineBoilerBlockEntity live) || live.hasExploded) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, VOLUME * (1D - d / RANGE));
            }
        };
    }
}
