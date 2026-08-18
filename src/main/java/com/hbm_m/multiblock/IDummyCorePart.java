package com.hbm_m.multiblock;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Contract for "dummy core" multiblocks: a single designated block (the core) holds all real
 * logic/state, while every other placed block of the structure just remembers the core's
 * position and defers to it. Mirrors the original 1.7.10 {@code BlockDummyable} pattern.
 *
 * <p>An interface (not a base class) so it can be mixed into any existing block entity hierarchy
 * (e.g. a tank multiblock that must also extend the fluid-tank block entity chain) instead of
 * forcing single inheritance from a dedicated dummy-core base.
 *
 * <p>Implementers just need a nullable {@code BlockPos} field for the raw core pointer, exposed
 * via {@link #getRawCorePos()}/{@link #setRawCorePos(BlockPos)}, and must persist it themselves
 * (e.g. in {@code saveAdditional}/{@code load}) — everything else is a default method.
 */
public interface IDummyCorePart {

    BlockPos getBlockPos();

    @Nullable
    Level getLevel();

    /** Raw stored pointer to the core; {@code null} means "this block IS the core". */
    @Nullable
    BlockPos getRawCorePos();

    /** Stores the raw core pointer as-is (pass {@code null} to mark this block as core). */
    void setRawCorePos(@Nullable BlockPos pos);

    /** True if this block IS the core (either explicitly marked, or never assigned a different core). */
    default boolean isCore() {
        BlockPos raw = getRawCorePos();
        return raw == null || raw.equals(getBlockPos());
    }

    /** Absolute position of the core of this structure (returns this block's own position if it is the core). */
    default BlockPos getCorePos() {
        return isCore() ? getBlockPos() : getRawCorePos();
    }

    /** Marks this block as a non-core part pointing at {@code pos} (pass this block's own position to mark it as core). */
    default void setCorePos(BlockPos pos) {
        setRawCorePos(pos.equals(getBlockPos()) ? null : pos.immutable());
    }

    /**
     * Resolves the core block entity: returns {@code this} if already core, otherwise looks up the
     * core position in the level. Returns {@code null} if the core is missing/unloaded or of the
     * wrong type.
     */
    @Nullable
    default <T extends BlockEntity> T resolveCore(Class<T> type) {
        if (isCore()) {
            return type.isInstance(this) ? type.cast(this) : null;
        }
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(getCorePos());
        return type.isInstance(be) ? type.cast(be) : null;
    }
}
