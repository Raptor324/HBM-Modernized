package com.hbm_m.world.gen.nbt;

import net.minecraft.world.level.LevelAccessor;

/**
 * 1:1 {@code com.hbm.world.gen.nbt.INBTTileEntityTransformable}: Blockentities, die sich beim Aufbau aus einer
 * .nbt-Struktur selbst anpassen (z.B. Logikbloecke, die sich scharf schalten). Port: auch waehrend der
 * Weltgenerierung (WorldGenLevel), daher {@link LevelAccessor}.
 */
public interface INBTTileEntityTransformable {

    /** Wird aufgerufen, sobald das Blockentity in einer NBT-Struktur gesetzt wurde. */
    void transformTE(LevelAccessor world, int coordBaseMode);
}
