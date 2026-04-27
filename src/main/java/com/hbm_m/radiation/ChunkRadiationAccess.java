package com.hbm_m.radiation;

import java.util.Optional;

import com.hbm_m.interfaces.IChunkRadiation;

import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkRadiationAccess {
    private ChunkRadiationAccess() {}

    public static Optional<IChunkRadiation> get(LevelChunk chunk) {
        //? if forge {
        return chunk.getCapability(com.hbm_m.capability.ChunkRadiationProvider.CHUNK_RADIATION_CAPABILITY).resolve();
        //?} else if fabric {
        /*com.hbm_m.capability.FabricChunkComponents.IChunkRadiationComponent comp =
                com.hbm_m.capability.FabricChunkComponents.CHUNK_RADIATION.get(chunk);
        return Optional.ofNullable(comp);
        *///?} else {
        /*return Optional.empty();
        *///?}
    }
}

