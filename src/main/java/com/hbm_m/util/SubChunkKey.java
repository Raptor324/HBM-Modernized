package com.hbm_m.util;

import net.minecraft.world.level.ChunkPos;

/**
 * Порт {@code com.hbm.util.SubChunkKey} из 1.7.10 (автор mlbv).
 * Идентификатор суб-чанка 16×16×16: чанк + абсолютный индекс секции по Y.
 */
public final class SubChunkKey {

    private final int chunkX;
    private final int chunkZ;
    /** Абсолютный индекс секции (worldY >> 4), НЕ индекс в массиве секций чанка. */
    private final int subY;
    private final int hash;

    public SubChunkKey(int cx, int cz, int sy) {
        this.chunkX = cx;
        this.chunkZ = cz;
        this.subY = sy;
        int result = subY;
        result = 31 * result + cx;
        result = 31 * result + cz;
        this.hash = result;
    }

    public SubChunkKey(ChunkPos pos, int sy) {
        this(pos.x, pos.z, sy);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubChunkKey)) return false;
        SubChunkKey k = (SubChunkKey) o;
        return this.subY == k.subY && this.chunkX == k.chunkX && this.chunkZ == k.chunkZ;
    }

    public int getSubY() {
        return subY;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public ChunkPos getPos() {
        return new ChunkPos(this.chunkX, this.chunkZ);
    }
}
