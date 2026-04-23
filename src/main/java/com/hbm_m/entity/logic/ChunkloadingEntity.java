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
 */
public abstract class ChunkloadingEntity extends Entity {

    private static final TicketType<UUID> CHUNK_TICKET =
            TicketType.create("chunkloading_entity", Comparator.comparing(UUID::toString), 0);

    private ChunkPos loadedChunk;

    protected ChunkloadingEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * Обновляет тикет при перемещении сущности в другой чанк.
     * Нужно вызывать каждый тик на стороне сервера.
     */
    protected void updateChunkTicket() {
        if (!level().isClientSide && level() instanceof ServerLevel server) {
            ChunkPos newPos = new ChunkPos(this.blockPosition());
            if (this.loadedChunk == null || !newPos.equals(this.loadedChunk)) {
                if (this.loadedChunk != null) {
                    server.getChunkSource().removeRegionTicket(
                            CHUNK_TICKET,
                            this.loadedChunk,
                            3,
                            this.getUUID()
                    );
                }

                this.loadedChunk = newPos;
                server.getChunkSource().addRegionTicket(
                        CHUNK_TICKET,
                        this.loadedChunk,
                        3,
                        this.getUUID()
                );
            }
        }
    }

    /**
     * Сбрасывает текущий тикет подгрузки чанка.
     * Вызывать при завершении долгоживущих эффектов (например, в remove()).
     */
    protected void clearChunkTicket() {
        if (!level().isClientSide && loadedChunk != null && level() instanceof ServerLevel server) {
            server.getChunkSource().removeRegionTicket(
                    CHUNK_TICKET,
                    this.loadedChunk,
                    3,
                    this.getUUID()
            );
            this.loadedChunk = null;
        }
    }
}

