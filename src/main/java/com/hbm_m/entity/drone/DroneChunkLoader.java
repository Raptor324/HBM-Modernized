package com.hbm_m.entity.drone;

import java.util.Comparator;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * Port des Chunk-Loading-Verhaltens von {@code EntityDeliveryDrone} (1.7.10 Original, dort ueber
 * die alte {@code ForgeChunkManager.Ticket}-API mit linienfoermiger Vorausladung entlang der
 * Flugbahn). Moderne Forge-1.20.1-Chunk-Ticket-API kennt keine beliebigen Chunk-Polygone mehr, nur
 * einen Radius um einen Ziel-Chunk - deshalb hier vereinfacht: ein sich mit der Drohne mitbewegender
 * Radius-Ticket (wie bereits fuer Raketen in {@link com.hbm_m.entity.missile.MissileBaseEntity}
 * etabliert), statt der Original-Liniensegment-Vorausladung. Funktional aequivalent (der Bereich um
 * die Drohne bleibt geladen), nur ohne das exakte "N Chunks voraus in Flugrichtung"-Padding.
 */
public final class DroneChunkLoader {

    private static final TicketType<UUID> CHUNK_TICKET =
            TicketType.create("hbm_m_drone", Comparator.comparing(UUID::toString));
    private static final int CHUNK_TICKET_RADIUS = 2;

    private static final WeakHashMap<EntityDeliveryDrone, ChunkPos> LOADED = new WeakHashMap<>();

    private DroneChunkLoader() {}

    public static void tick(EntityDeliveryDrone drone) {
        if (!(drone.level() instanceof ServerLevel server)) return;

        ChunkPos newPos = new ChunkPos(drone.blockPosition());
        ChunkPos oldPos = LOADED.get(drone);
        if (newPos.equals(oldPos)) return;

        if (oldPos != null) {
            server.getChunkSource().removeRegionTicket(CHUNK_TICKET, oldPos, CHUNK_TICKET_RADIUS, drone.getUUID());
        }
        server.getChunkSource().addRegionTicket(CHUNK_TICKET, newPos, CHUNK_TICKET_RADIUS, drone.getUUID());
        LOADED.put(drone, newPos);
    }

    public static void release(EntityDeliveryDrone drone) {
        ChunkPos pos = LOADED.remove(drone);
        if (pos != null && drone.level() instanceof ServerLevel server) {
            server.getChunkSource().removeRegionTicket(CHUNK_TICKET, pos, CHUNK_TICKET_RADIUS, drone.getUUID());
        }
    }
}
