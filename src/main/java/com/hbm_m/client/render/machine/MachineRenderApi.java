package com.hbm_m.client.render.machine;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Context available to {@link MachineRenderHook}s while rendering a machine frame.
 */
public interface MachineRenderApi {

    /** Final fade of this BE in this frame (min(static, animated)). */
    float fadeAlpha();

    /**
     * Transform of the part RELATIVE TO THE BLOCK-TRANSFORM BASIS in this frame,
     * or {@code null} if the part was not rendered (or was culled/frozen this
     * frame without a saved pose).
     * <p>
     * Contract: apply with {@code poseStack.mulPose(matrix)} immediately after
     * your own {@code pushPose()} - the hook receives the stack already carrying
     * the block transform, and the returned delta is exactly what is missing
     * from it. Static parts resolve to identity, animator-driven parts to the
     * animator's delta, parametric parts to recordStatics * jointDelta.
     * <p>
     * The returned matrix is a LIVE reference into this frame's transform slot,
     * NOT a copy: mutating it corrupts the value seen by every hook that runs
     * later in the same frame, and it is overwritten at the next machine frame.
     * Copy it ({@code new Matrix4f(m)}) if you need to change or hold it.
     */
    @Nullable Matrix4f partTransform(String partName);

    /** BlockEntity position. */
    net.minecraft.core.BlockPos blockPos();
}
