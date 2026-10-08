package com.hbm_m.client.sound;

import java.util.Map;
import java.util.WeakHashMap;

import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Allgemeiner Ersatz fuer das {@code AudioWrapper}-Muster der Originalmaschinen ({@code createAudioLoop}/
 * {@code keepAlive}): ein an die Blockentitaet gebundener Schleifenklang, der laeuft, solange {@code active} gemeldet
 * wird und der Spieler naeher als {@code range} Bloecke steht.
 */
public final class MachineLoopSoundClient {

    private static final Map<BlockEntity, Loop> SOUNDS = new WeakHashMap<>();

    private MachineLoopSoundClient() { }

    public static void tick(BlockEntity be, String key, boolean active, float volume, float pitch, double range) {
        Player me = Minecraft.getInstance().player;
        Loop loop = SOUNDS.get(be);
        boolean run = active && me != null
                && me.distanceToSqr(be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 0.5, be.getBlockPos().getZ() + 0.5) < range * range;

        if (run) {
            if (loop == null || loop.isStopped()) {
                SoundEvent ev = HbmSoundsNT.get(key);
                if (ev == null) return;
                loop = new Loop(ev, be, volume, pitch);
                SOUNDS.put(be, loop);
                Minecraft.getInstance().getSoundManager().play(loop);
            }
            loop.alive = 20;
        } else if (loop != null) {
            loop.alive = 0;
        }
    }

    private static class Loop extends AbstractTickableSoundInstance {

        private final BlockEntity be;
        int alive = 20;

        Loop(SoundEvent ev, BlockEntity be, float volume, float pitch) {
            super(ev, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.be = be;
            this.looping = true;
            this.delay = 0;
            this.volume = volume;
            this.pitch = pitch;
            this.x = be.getBlockPos().getX() + 0.5;
            this.y = be.getBlockPos().getY() + 0.5;
            this.z = be.getBlockPos().getZ() + 0.5;
        }

        @Override
        public void tick() {
            if (be.isRemoved() || alive-- <= 0) this.stop();
        }
    }
}
