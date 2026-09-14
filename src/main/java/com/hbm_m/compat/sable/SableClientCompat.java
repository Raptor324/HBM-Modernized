package com.hbm_m.compat.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Matrix4f;

/**
 * Client half of {@link SableCompat}: the render transform of the ship whose plot chunk holds
 * {@code pos}, i.e. plot-grid coordinates -> world coordinates for this frame. Overlays drawn
 * from absolute block positions (placement frames, obstruction boxes) are invisible on a ship
 * without it - the box lands millions of blocks away.
 */
public final class SableClientCompat {

    private SableClientCompat() {}

    /**
     * Matrix taking coordinates relative to {@code origin} (a plot-grid block on a ship) to
     * camera-relative world space; null when {@code origin} is not on a ship. Everything is composed
     * in double: plot coordinates (~2e7) lose whole blocks once cast to float, so callers draw their
     * geometry relative to {@code origin} and only the small final translation reaches the GPU.
     */
    @Nullable
    public static Matrix4f localToView(BlockPos origin, Vec3 cameraPos) {
        if (!SableCompat.isLoaded()) return null;
        Matrix4d shipToWorld = Impl.matrix(origin);
        if (shipToWorld == null) return null;
        Matrix4d m = new Matrix4d()
                .translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)
                .mul(shipToWorld)
                .translate(origin.getX(), origin.getY(), origin.getZ());
        return new Matrix4f(m);
    }

    // The companion jar is only on the 1.21.1 compile classpath (Sable has no 1.20.1 build).
    private static final class Impl {
        @Nullable
        static Matrix4d matrix(BlockPos pos) {
            //? if >= 1.21.1 {
            try {
                var sub = dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContainingClient(pos);
                if (sub == null) return null;
                return sub.renderPose().bakeIntoMatrix(new Matrix4d());
            } catch (Throwable t) {
                return null;
            }
            //?} else {
            /*return null;
            *///?}
        }
    }
}
