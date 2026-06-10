package com.hbm_m.client.render.shader;

import com.hbm_m.main.MainRegistry;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * Safe wrapper around {@link ShaderInstance#apply()} for Iris
 * {@code ExtendedShader} instances whose sampler/image bindings can throw
 * {@code IllegalStateException: Tried to use a destroyed GlResource} when the
 * pipeline was rebuilt or the render phase moved on.
 */
public final class IrisShaderApply {

    private IrisShaderApply() {}

    /**
     * @return {@code true} if {@code apply()} completed without throwing.
     */
    public static boolean tryApply(ShaderInstance shader) {
        if (shader == null) return false;
        try {
            shader.apply();
            return true;
        } catch (IllegalStateException e) {
            if (isDestroyedGlResource(e)) {
                IrisExtendedShaderAccess.invalidateShaderCache();
                IrisRenderBatch.invalidateCaches();
                MainRegistry.LOGGER.debug("IrisShaderApply: apply failed (destroyed GlResource), caches invalidated");
            }
            return false;
        } catch (Throwable t) {
            MainRegistry.LOGGER.debug("IrisShaderApply: apply failed ({})", t.toString());
            return false;
        }
    }

    private static boolean isDestroyedGlResource(Throwable t) {
        String msg = t.getMessage();
        return msg != null && msg.contains("destroyed GlResource");
    }
}
