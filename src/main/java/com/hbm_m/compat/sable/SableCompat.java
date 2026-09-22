package com.hbm_m.compat.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sable (Create Aeronautics) sub-levels are not separate levels: a ship's blocks live in the same
 * {@link Level} inside far-away "plot" chunks, so a block entity on a ship reports a position
 * millions of blocks from the players. Anything that leaves the block grid - entities, particle
 * packets, distance checks - has to be projected into world space first.
 *
 * <p>The companion API ships inside Sable's jar and is only on our compile classpath, so every
 * call is routed through {@link Impl}, which is not loaded unless Sable is present.
 */
public final class SableCompat {

    private static final boolean LOADED = dev.architectury.platform.Platform.isModLoaded("sable");

    private SableCompat() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    /** Sub-level (plot grid) position -> world position; identity outside sub-levels or without Sable. */
    public static Vec3 toWorld(Level level, Vec3 pos) {
        return LOADED ? Impl.toWorld(level, pos) : pos;
    }

    public static Vec3 toWorld(Level level, double x, double y, double z) {
        return toWorld(level, new Vec3(x, y, z));
    }

    /** Block centre of {@code pos} in world space. */
    public static Vec3 blockCenterInWorld(Level level, BlockPos pos) {
        return toWorld(level, new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
    }

    // The companion jar is only on the 1.21.1 compile classpath (Sable has no 1.20.1 build).
    private static final class Impl {
        static Vec3 toWorld(Level level, Vec3 pos) {
            //? if >= 1.21.1 {
            /*try {
                return dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.projectOutOfSubLevel(level, pos);
            } catch (Throwable t) {
                return pos;
            }
            *///?} else {
            return pos;
            //?}
        }
    }
}
