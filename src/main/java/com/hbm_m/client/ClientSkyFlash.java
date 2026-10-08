package com.hbm_m.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

/** Client-Gegenstueck zu 1.7 {@code world.lastLightningBolt = n}: Himmelsblitz fuer n Ticks. */
public final class ClientSkyFlash {

    private ClientSkyFlash() { }

    public static void flash(Level level, int ticks) {
        if (level instanceof ClientLevel cl) cl.setSkyFlashTime(ticks);
    }
}
