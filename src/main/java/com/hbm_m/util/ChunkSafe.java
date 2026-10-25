package com.hbm_m.util;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Weltzugriffe, die nie einen Chunk laden.
 * <p>
 * Fuer setRemoved/onChunkUnloaded und alle Netz-Abmeldepfade: {@code level.getBlockState}/
 * {@code getBlockEntity} auf einen nicht mehr geladenen Nachbarn laedt den Chunk synchron nach -
 * beim Stoppen des Servers bzw. Verlassen der Welt haengt der Prozess dann. {@code getChunkNow}
 * liefert nur bereits fertig geladene Chunks (abseits des Server-Hauptthreads immer null).
 */
public final class ChunkSafe {

    private ChunkSafe() {}

    /** true nur, wenn der Chunk von {@code pos} vollstaendig geladen ist - laedt nie nach. */
    public static boolean isLoaded(@Nullable Level level, BlockPos pos) {
        return level != null && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    /** Blockzustand oder null, wenn der Chunk nicht geladen ist. */
    @Nullable
    public static BlockState getBlockState(@Nullable Level level, BlockPos pos) {
        return isLoaded(level, pos) ? level.getBlockState(pos) : null;
    }

    /** BlockEntity oder null, wenn der Chunk nicht geladen ist. */
    @Nullable
    public static BlockEntity getBlockEntity(@Nullable Level level, BlockPos pos) {
        return isLoaded(level, pos) ? level.getBlockEntity(pos) : null;
    }
}
