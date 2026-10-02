package com.hbm_m.client.sound;

import java.util.Map;
import java.util.WeakHashMap;

import com.hbm_m.entity.projectile.EntityMeteor;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Client-Teil von {@code EntityMeteor}: der Schleifenklang {@code hbm:entity.meteoriteFallingLoop} (Lautstaerke 1,
 * Tonhoehe 0.9-1.1) startet, sobald der Spieler naeher als 210 Bloecke ist, und folgt dem Meteor.
 */
public final class MeteorSoundClient {

    private static final Map<EntityMeteor, FallingSound> SOUNDS = new WeakHashMap<>();

    private MeteorSoundClient() { }

    public static void tick(EntityMeteor meteor) {
        FallingSound sound = SOUNDS.get(meteor);

        if (sound == null) {
            sound = new FallingSound(meteor, 0.9F + meteor.level().random.nextFloat() * 0.2F);
            SOUNDS.put(meteor, sound);
        }

        if (!Minecraft.getInstance().getSoundManager().isActive(sound)) {
            if (sound.isStopped()) {
                sound = new FallingSound(meteor, sound.getPitch());
                SOUNDS.put(meteor, sound);
            }
            Player player = Minecraft.getInstance().player;
            if (player != null && player.distanceToSqr(meteor.getX(), meteor.getY(), meteor.getZ()) < 210 * 210) {
                Minecraft.getInstance().getSoundManager().play(sound);
            }
        }
    }

    public static void stop(EntityMeteor meteor) {
        FallingSound sound = SOUNDS.remove(meteor);
        if (sound != null) sound.finish();
    }

    private static class FallingSound extends AbstractTickableSoundInstance {

        private final EntityMeteor meteor;

        FallingSound(EntityMeteor meteor, float pitch) {
            super(HbmSoundsNT.get("hbm:entity.meteoriteFallingLoop"), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.meteor = meteor;
            this.looping = true;
            this.delay = 0;
            this.volume = 1F;
            this.pitch = pitch;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            update();
        }

        private void update() {
            this.x = meteor.getX();
            this.y = meteor.getY() + meteor.getBbHeight() / 2;
            this.z = meteor.getZ();
        }

        void finish() {
            this.stop();
        }

        @Override
        public void tick() {
            if (meteor.isRemoved()) {
                this.stop();
                return;
            }
            update();
        }
    }
}
