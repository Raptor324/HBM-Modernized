package com.hbm_m.entity.logic;

import java.util.Comparator;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Базовая сущность, которая удерживает вокруг себя чанки загруженными.
 * Используется длительными эффектами вроде ядерных взрывов и fallout-дождя.
 *
 * Тикет выставляется в {@link #onAddedToWorld()}, иначе сущность в выгруженном чанке
 * никогда не получит tick() и взрыв не затронет местность (как у ракеты через MissileBaseEntity).
 */
public abstract class EntityExplosionChunkloading extends Entity {

    private static final TicketType<UUID> CHUNK_TICKET =
            TicketType.create("hbm_m_explosion_chunkload", Comparator.comparing(UUID::toString));

    private static final int DEFAULT_CHUNK_TICKET_RADIUS = 3;

    private ChunkPos loadedChunk;
    private int activeTicketRadius = DEFAULT_CHUNK_TICKET_RADIUS;

    protected EntityExplosionChunkloading(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Радиус region ticket в чанках; переопределяется под радиус кратера / fallout. */
    protected int getChunkLoadRadius() {
        return DEFAULT_CHUNK_TICKET_RADIUS;
    }

    /**
     * Обновляет тикет при перемещении сущности в другой чанк.
     * Нужно вызывать каждый тик на стороне сервера.
     */
    protected void updateChunkTicket() {
        if (level().isClientSide || !(level() instanceof ServerLevel server)) {
            return;
        }

        ChunkPos newPos = new ChunkPos(this.blockPosition());
        int radius = getChunkLoadRadius();
        if (this.loadedChunk != null && newPos.equals(this.loadedChunk) && radius == this.activeTicketRadius) {
            return;
        }

        releaseChunkTicket(server);
        this.loadedChunk = newPos;
        this.activeTicketRadius = radius;
        server.getChunkSource().addRegionTicket(CHUNK_TICKET, this.loadedChunk, this.activeTicketRadius, this.getUUID());
    }

    /**
     * Сбрасывает текущий тикет подгрузки чанка.
     * Вызывать при завершении долгоживущих эффектов (например, в remove()).
     */
    protected void clearChunkTicket() {
        if (level() instanceof ServerLevel server) {
            releaseChunkTicket(server);
        }
    }

    private void releaseChunkTicket(ServerLevel server) {
        if (this.loadedChunk != null) {
            server.getChunkSource().removeRegionTicket(
                    CHUNK_TICKET,
                    this.loadedChunk,
                    this.activeTicketRadius,
                    this.getUUID()
            );
            this.loadedChunk = null;
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide && level() instanceof ServerLevel server && this.loadedChunk == null) {
            this.loadedChunk = new ChunkPos(this.blockPosition());
            this.activeTicketRadius = getChunkLoadRadius();
            server.getChunkSource().addRegionTicket(
                    CHUNK_TICKET,
                    this.loadedChunk,
                    this.activeTicketRadius,
                    this.getUUID()
            );
        }
    }

    @Override
    public void onRemovedFromWorld() {
        if (!level().isClientSide && level() instanceof ServerLevel server) {
            releaseChunkTicket(server);
        }
        super.onRemovedFromWorld();
    }
}
