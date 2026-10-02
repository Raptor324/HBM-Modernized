package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.MachineAutosawBlockEntity;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Original: {@code createAudioLoop} = {@code getLoopedSound(ENGINE_LOOP, x, y, z, 1F, 10F, 1F + rand * 0.1F, 10)}, laeuft solange an und nicht pausiert. */
public final class AutosawLoopSoundFactory {

    private static final double RANGE = 10.0D;

    private AutosawLoopSoundFactory() {}

    public static Object create(MachineAutosawBlockEntity saw) {
        BlockPos pos = saw.getBlockPos();
        return new AbstractTickableSoundInstance(HbmSoundsNT.get("hbm:block.engine"), SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.volume = 1F;
                this.pitch = 1F + RandomSource.create().nextFloat() * 0.1F;
                this.attenuation = Attenuation.LINEAR;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) { this.stop(); return; }
                if (!(level.getBlockEntity(pos) instanceof MachineAutosawBlockEntity live) || !live.isOn || live.isSuspended) { this.stop(); return; }
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) { this.stop(); return; }
                double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                this.volume = (float) Math.max(0D, 1D - d / RANGE);
            }
        };
    }
}
