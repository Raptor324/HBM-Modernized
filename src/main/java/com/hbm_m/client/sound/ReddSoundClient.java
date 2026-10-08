package com.hbm_m.client.sound;

import java.util.Map;
import java.util.WeakHashMap;

import com.hbm_m.blockentity.machines.BatteryREDDBlockEntity;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Client-Teil von {@code TileEntityBatteryREDD.updateEntity}: Brumm-Schleife {@code block.fensuHum} (Lautstaerke 1,5,
 * Reichweite 25), solange das Rad dreht und der Spieler naeher als 30 Bloecke steht; Tonhoehe 0,5 + Tempo/15 * 1,5.
 */
public final class ReddSoundClient {

    private static final Map<BatteryREDDBlockEntity, Hum> SOUNDS = new WeakHashMap<>();

    private ReddSoundClient() { }

    public static void tick(BatteryREDDBlockEntity redd) {
        Player me = Minecraft.getInstance().player;
        Hum hum = SOUNDS.get(redd);
        boolean active = me != null && redd.prevRotation != redd.rotation
                && me.distanceToSqr(redd.getBlockPos().getX() + 0.5, redd.getBlockPos().getY() + 5.5, redd.getBlockPos().getZ() + 0.5) < 30 * 30;

        if (active) {
            float pitch = 0.5F + redd.getSpeed() / 15F * 1.5F;
            if (hum == null || hum.isStopped()) {
                SoundEvent ev = HbmSoundsNT.get("hbm:block.fensuHum");
                if (ev == null) return;
                hum = new Hum(ev, redd);
                SOUNDS.put(redd, hum);
                Minecraft.getInstance().getSoundManager().play(hum);
            }
            hum.target = pitch;
            hum.alive = 5;
        } else if (hum != null) {
            hum.alive = 0;
        }
    }

    private static class Hum extends AbstractTickableSoundInstance {

        private final BatteryREDDBlockEntity redd;
        float target = 1F;
        int alive = 5;

        Hum(SoundEvent ev, BatteryREDDBlockEntity redd) {
            super(ev, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.redd = redd;
            this.looping = true;
            this.delay = 0;
            this.volume = 1.5F;
            this.x = redd.getBlockPos().getX() + 0.5;
            this.y = redd.getBlockPos().getY() + 0.5;
            this.z = redd.getBlockPos().getZ() + 0.5;
        }

        @Override
        public void tick() {
            if (redd.isRemoved() || alive-- <= 0) {
                this.stop();
                return;
            }
            this.pitch = target;
        }
    }
}
