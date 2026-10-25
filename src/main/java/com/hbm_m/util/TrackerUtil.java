package com.hbm_m.util;

import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * {@code TrackerUtil}: erzwungene Positionssynchronisation. Das Original greift per Reflexion in den Entity-Tracker;
 * in 1.20 genuegt ein Teleport-Paket an alle Beobachter (der Client interpoliert dann wie beim Original).
 */
public class TrackerUtil {

    /** Force-teleports the given entity using the tracker */
    public static void sendTeleport(Level world, Entity e) {
        if (world instanceof ServerLevel server) {
            server.getChunkSource().broadcast(e, new ClientboundTeleportEntityPacket(e));
        }
    }
}
