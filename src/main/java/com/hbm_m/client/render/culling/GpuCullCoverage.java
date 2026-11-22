package com.hbm_m.client.render.culling;

import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;

import net.minecraft.client.Minecraft;

/**
 * Collect-time mirror of the GPU-cull enablement the MDI flush computes
 * ({@code MdiBatchCoordinator} dispatch: mode==GPU, capability, non-shadow
 * pass, depth texture present). When {@link #isActive()} is true, every
 * collected machine lands in the MDI batch and the GPU frustum/Hi-Z compaction
 * culls it - the CPU-side frustum pre-gates in {@code MachineBer} (assert path
 * and full path) are then redundant re-tests of the same memoized AABB and are
 * skipped so retained instances stay in the batch for the GPU culler.
 * <p>
 * Fail-open parity: if the GPU-cull dispatch fails at flush, the fallback
 * restores instances unculled for that frame (existing
 * {@code MdiBatchCoordinator} fallback behavior) - overdraw for one frame
 * until the next collect, never corruption.
 * <p>
 * Pure MC/GL + config reads, no loader difference. Not valid in the shadow
 * pass (returns false there by construction).
 */
public final class GpuCullCoverage {

    private GpuCullCoverage() {}

    /**
     * True when the GPU culler owns visibility for the current main-pass frame:
     * CPU-side frustum testing of collected machines may be skipped.
     */
    public static boolean isActive() {
        if (ModClothConfig.get().getEffectiveOcclusionCullingMode() != ModClothConfig.OcclusionCullingMode.GPU) {
            return false;
        }
        if (!GpuCullingCapability.isSupported()
                || ClientRenderFlags.forceVanillaImmediate()
                || ShaderCompatibilityDetector.isExternalShaderActive()
                || ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return false;
        }
        var target = Minecraft.getInstance().getMainRenderTarget();
        return target != null && target.getDepthTextureId() > 0;
    }
}
