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
     * Final matrix of the part in this frame (block-relative, as on the stack after
     * the animator), or {@code null} if the part was not rendered. A copy - safe to
     * mutate. Lets hooks (stamp items, etc.) position themselves relative to the
     * animated part.
     */
    @Nullable Matrix4f partTransform(String partName);

    /** BlockEntity position. */
    net.minecraft.core.BlockPos blockPos();
}
