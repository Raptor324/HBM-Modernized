package com.hbm_m.client.render.shader;


import com.hbm_m.main.MainRegistry;


import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;


/**
 * Resource reload listener for detecting shader changes.
 * Fires on F3+T, shader pack swap, or world load.
 */
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
@OnlyIn(Dist.CLIENT)
//?}
public class ShaderReloadListener extends SimplePreparableReloadListener<Void> {
    
    /**
     * Prepare phase (async).
     */
    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.push("hbm_shader_detection");
        MainRegistry.LOGGER.debug("ShaderReloadListener: prepare phase");
        profiler.pop();
        return null;
    }
    
    /**
     * Apply phase (sync, main thread).
     * Safe to touch game objects here.
     */
    @Override
    protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.push("hbm_shader_detection_apply");
        
        try {
            // Drop the per-pass shader cache and the IrisRenderBatch's cached
            // uniform handles - F3+T rebuilds the Iris pipeline and gives every
            // ExtendedShader a fresh program ID, so the cached references
            // would be pointing at GL programs that are about to be deleted.
            IrisExtendedShaderAccess.invalidateShaderCache();
            IrisRenderBatch.invalidateCaches();
            MainRegistry.LOGGER.debug("ShaderReloadListener: Resource reload complete, Iris caches invalidated");
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during shader reload", e);
        } finally {
            profiler.pop();
        }
    }
}
