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
 * Client-Teil von {@code TESirenPacket} + {@code SoundLoopSiren}: je Sirene hoechstens ein Klang, Lautstaerke
 * {@code 2 - 2 * Abstand / Reichweite} ohne eigene Daempfung; LOOP wiederholt, PASS/SOUND spielen einmal je Ausloesung.
 */
public final class SirenSoundClient {

    private static final Map<BlockEntity, Siren> SOUNDS = new WeakHashMap<>();

    private SirenSoundClient() { }

    /**
     * @param key     Klangschluessel ({@code hbm:alarm.*}), {@code null} = keine Kassette
     * @param range   Original {@code TrackType.getVolume()}
     * @param loop    LOOP-Typ
     * @param active  Original {@code m.active}
     * @param restart PASS/SOUND: neue Ausloesung (Original schickt "aus" und gleich wieder "an")
     */
    public static void update(BlockEntity be, String key, int range, boolean loop, boolean active, boolean restart) {
        Siren sound = SOUNDS.get(be);
        if (sound != null && sound.isStopped()) { SOUNDS.remove(be); sound = null; }

        if (restart && sound != null) {
            sound.end();
            SOUNDS.remove(be);
            sound = null;
        }

        if (active && key != null) {
            if (sound == null) {
                SoundEvent ev = HbmSoundsNT.get(key);
                if (ev == null) return;
                sound = new Siren(ev, key, be, loop);
                sound.intendedVolume = range;
                SOUNDS.put(be, sound);
                Minecraft.getInstance().getSoundManager().play(sound);
            } else {
                if (!sound.key.equals(key)) {
                    // Track switched, stop and restart
                    sound.end();
                    SoundEvent ev = HbmSoundsNT.get(key);
                    if (ev == null) { SOUNDS.remove(be); return; }
                    sound = new Siren(ev, key, be, loop);
                    SOUNDS.put(be, sound);
                    Minecraft.getInstance().getSoundManager().play(sound);
                }
                sound.intendedVolume = range;
                sound.loop = loop;
            }
        } else if (!active && loop && sound != null) {
            sound.end();
            SOUNDS.remove(be);
        }
    }

    private static class Siren extends AbstractTickableSoundInstance {

        final String key;
        final BlockEntity be;
        float intendedVolume = 10.0F;
        boolean loop;

        Siren(SoundEvent ev, String key, BlockEntity be, boolean loop) {
            super(ev, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.key = key;
            this.be = be;
            this.loop = loop;
            this.looping = loop;
            this.delay = 0;
            this.attenuation = Attenuation.NONE;
            this.x = be.getBlockPos().getX() + 0.5;
            this.y = be.getBlockPos().getY() + 0.5;
            this.z = be.getBlockPos().getZ() + 0.5;
            this.volume = 0F;
        }

        void end() { this.stop(); }

        @Override
        public void tick() {
            if (be.isRemoved()) { this.stop(); return; }
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                float f = (float) Math.sqrt(player.distanceToSqr(x, y, z));
                this.volume = Math.max(0F, (f / intendedVolume) * -2 + 2);
            } else {
                this.volume = intendedVolume;
            }
            this.looping = loop;
        }
    }
}
