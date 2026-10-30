
package com.hbm_m.client.render.shader;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Try-with-resources scope helper that calls
 * {@code WorldRenderingPipeline.setPhase(WorldRenderingPhase.<phase>)} on entry
 * and restores {@code WorldRenderingPhase.NONE} on close.
 */

@OnlyIn(Dist.CLIENT)
public final class IrisPhaseGuard implements AutoCloseable {

    private static final IrisPhaseGuard NOOP = new IrisPhaseGuard(false, null);

    private final boolean active;
    private final Object previousPhase;

    private IrisPhaseGuard(boolean active, Object previousPhase) {
        this.active = active;
        this.previousPhase = previousPhase;
    }

    /**
     * Pushes {@code BLOCK_ENTITIES} phase. Returns a guard whose {@link #close()} restores
     * previous phase or {@code NONE}. When Iris is missing or the reflective call fails the guard is a no-op.
     */
    public static IrisPhaseGuard pushBlockEntities() {
        if (!IrisReflectionBridge.isReflectionAvailable()) {
            return NOOP;
        }

        try {
            Object pipeline = IrisReflectionBridge.getLivePipeline();
            if (pipeline == null) return NOOP;
            Object target = IrisReflectionBridge.getPhaseBlockEntities();
            if (target == null) return NOOP;
            Object prevPhase = IrisReflectionBridge.getPhase(pipeline);
            if (prevPhase == target) {
                return NOOP;
            }

            IrisReflectionBridge.setPhase(pipeline, target);
            return new IrisPhaseGuard(true, prevPhase);
        } catch (Throwable t) {
            return NOOP;
        }
    }

    @Override
    public void close() {
        if (!active) return;
        try {
            Object pipeline = IrisReflectionBridge.getLivePipeline();
            if (pipeline == null) return;
            if (previousPhase != null) {
                IrisReflectionBridge.setPhase(pipeline, previousPhase);
            } else {
                Object none = IrisReflectionBridge.getPhaseNone();
                if (none != null) {
                    IrisReflectionBridge.setPhase(pipeline, none);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
