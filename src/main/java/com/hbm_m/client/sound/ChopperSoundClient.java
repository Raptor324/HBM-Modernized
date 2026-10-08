package com.hbm_m.client.sound;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import com.hbm_m.entity.mob.EntityHunterChopper;
import com.hbm_m.entity.projectile.EntityChopperMine;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Client-Teil des Original-Tricks {@code ModEventHandlerClient.onPlaySound}: der Server spielt stumme Platzhalter
 * ({@code misc.nullChopper/nullCrashing/nullMine}, Lautstaerke 10), der Client haengt daraufhin einen an die Entity
 * gebundenen Schleifenklang an ({@code MovingSoundChopper/Crashing/ChopperMine}, Lautstaerke 10). Hier direkt aus dem
 * Client-Tick: gestartet, sobald der Platzhalter hoerbar waere (160 Bloecke).
 */
public final class ChopperSoundClient {

    private static final Map<Entity, LoopSound> FLYING = new WeakHashMap<>();
    private static final Map<Entity, LoopSound> CRASHING = new WeakHashMap<>();
    private static final Map<Entity, LoopSound> MINE = new WeakHashMap<>();

    private ChopperSoundClient() { }

    public static void tickChopper(EntityHunterChopper chopper) {
        if (!chopper.getIsDying()) {
            ensure(FLYING, chopper, "hbm:entity.chopperFlyingLoop", e -> ((EntityHunterChopper) e).getIsDying());
        } else {
            ensure(CRASHING, chopper, "hbm:entity.chopperCrashingLoop", e -> false);
        }
    }

    private static final Map<Entity, LoopSound> PLANE = new WeakHashMap<>();

    /** Original {@code EntityC130}: AudioWrapper {@code entity.bomberLoop} (Lautstaerke 2, 250 Bloecke), solange es lebt. */
    public static void tickPlane(com.hbm_m.entity.logic.EntityPlaneBase plane, String key) {
        if (plane.getSyncedHealth() <= 0) return;
        LoopSound sound = PLANE.get(plane);
        if (sound != null && !sound.isStopped()) return;

        Player player = Minecraft.getInstance().player;
        if (player == null || player.distanceToSqr(plane) > 250 * 250) return;

        SoundEvent ev = HbmSoundsNT.get(key);
        if (ev == null) return;
        sound = new LoopSound(ev, plane, e -> ((com.hbm_m.entity.logic.EntityPlaneBase) e).getSyncedHealth() <= 0);
        sound.setVolume(2.0F);
        PLANE.put(plane, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    public static void tickMine(EntityChopperMine mine) {
        ensure(MINE, mine, "hbm:entity.chopperMineLoop", e -> false);
    }

    private static void ensure(Map<Entity, LoopSound> map, Entity entity, String key, Predicate<Entity> stopIf) {
        LoopSound sound = map.get(entity);
        if (sound != null && !sound.isStopped()) return;

        Player player = Minecraft.getInstance().player;
        if (player == null || player.distanceToSqr(entity) > 160 * 160) return;

        SoundEvent ev = HbmSoundsNT.get(key);
        if (ev == null) return;
        sound = new LoopSound(ev, entity, stopIf);
        map.put(entity, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static class LoopSound extends AbstractTickableSoundInstance {

        private final Entity entity;
        private final Predicate<Entity> stopIf;

        LoopSound(SoundEvent ev, Entity entity, Predicate<Entity> stopIf) {
            super(ev, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.entity = entity;
            this.stopIf = stopIf;
            this.looping = true;
            this.delay = 0;
            this.volume = 10.0F;
            this.pitch = 1.0F;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
        }

        void setVolume(float v) { this.volume = v; }

        @Override
        public void tick() {
            if (entity.isRemoved() || !entity.isAlive() || stopIf.test(entity)) {
                this.stop();
                return;
            }
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
        }
    }
}
