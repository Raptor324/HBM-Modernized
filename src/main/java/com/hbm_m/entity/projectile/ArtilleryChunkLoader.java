package com.hbm_m.entity.projectile;

import java.util.Comparator;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

/**
 * Ersatz fuer das {@code ForgeChunkManager.Ticket} von {@code EntityArtilleryShell}/{@code EntityArtilleryRocket}
 * ({@code IChunkLoader}): {@code loadNeighboringChunks} haelt genau den Chunk des Geschosses geladen,
 * {@code clearChunkLoader} gibt ihn frei. Radius 2 = der Mittelchunk tickt Entities (wie der erzwungene Chunk).
 */
public final class ArtilleryChunkLoader {

    private static final TicketType<UUID> TICKET = TicketType.create("hbm_m_artillery", Comparator.comparing(UUID::toString));
    private static final int RADIUS = 2;
    private static final WeakHashMap<Entity, ChunkPos> LOADED = new WeakHashMap<>();

    private ArtilleryChunkLoader() {}

    /** Original {@code loadNeighboringChunks(newChunkX, newChunkZ)}. */
    public static void load(Entity entity, int chunkX, int chunkZ) {
        if (!(entity.level() instanceof ServerLevel server)) return;
        ChunkPos newPos = new ChunkPos(chunkX, chunkZ);
        ChunkPos oldPos = LOADED.get(entity);
        if (newPos.equals(oldPos)) return;
        if (oldPos != null) server.getChunkSource().removeRegionTicket(TICKET, oldPos, RADIUS, entity.getUUID());
        server.getChunkSource().addRegionTicket(TICKET, newPos, RADIUS, entity.getUUID());
        LOADED.put(entity, newPos);
    }

    /** Original {@code clearChunkLoader()}. */
    public static void release(Entity entity) {
        ChunkPos pos = LOADED.remove(entity);
        if (pos != null && entity.level() instanceof ServerLevel server) {
            server.getChunkSource().removeRegionTicket(TICKET, pos, RADIUS, entity.getUUID());
        }
    }
}
