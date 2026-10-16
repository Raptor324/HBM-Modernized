package com.hbm_m.client.render.cache;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.MdiGeometryAtlas;
import com.hbm_m.client.render.culling.InstancedRenderFrame;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * SINGLE invalidation point for client render caches.
 * <p>
 * Previously the cleanup lists lived in three diverging places (ClientSetup disconnect,
 * ClientSetup reload listener, DeferredCacheCleanupReloadListener), each with its own
 * subset of caches, and new caches were regularly forgotten. Now any code path (reload,
 * disconnect, debugging) calls {@link #invalidateAll(Reason)}; new rendering subsystems
 * register their cleanup via {@link #register(InvalidationHook)} and are not mentioned anywhere else.
 * <p>
 * Invalidation always runs on the render thread (deferred via
 * {@link RenderSystem#recordRenderCall} if called from another thread) -
 * GL objects must not be deleted outside the GL context.
 */
@OnlyIn(Dist.CLIENT)
public final class RenderCacheManager {

    public enum Reason {
        /** F3+T / resource pack change / shader reload. */
        RESOURCE_RELOAD,
        /** Leaving a world / disconnect. */
        SESSION_END
    }

    /** Clears a single cache; invoked strictly on the render thread. */
    @FunctionalInterface
    public interface InvalidationHook {
        void invalidate(Reason reason);
    }

    private static final List<InvalidationHook> HOOKS = new CopyOnWriteArrayList<>();

    private RenderCacheManager() {}

    /**
     * Registers an extra invalidator. Call during init (not every frame). No duplicate
     * protection by class name - the call happens once from the cache owner's static init.
     */
    public static void register(InvalidationHook hook) {
        HOOKS.add(hook);
    }

    /**
     * Full invalidation of all caches. Thread-safe: always executes on the
     * render thread (either immediately or deferred).
     */
    public static void invalidateAll(Reason reason) {
        if (RenderSystem.isOnRenderThread()) {
            invalidateAllNow(reason);
        } else {
            RenderSystem.recordRenderCall(() -> invalidateAllNow(reason));
        }
    }

    /**
     * Immediate invalidation. Render thread only!
     * Fixed order: per-frame/MDI states first, then the GPU atlas,
     * then renderer caches, then shared mesh/light/occlusion caches.
     */
    public static void invalidateAllNow(Reason reason) {
        try {
            // Per-frame states (MDI frame gate, stats, deferred redraws)
            guarded("InstancedRenderFrame.clear", InstancedRenderFrame::clear);

            // MDI GPU atlas: sessions must not survive reload/disconnect
            guarded("MdiGeometryAtlas.resetForResourceLifecycle", MdiGeometryAtlas::resetForResourceLifecycle);

            // Retained instance scene graph & sparse queue
            guarded("RetainedInstanceGraph.clear",
                    () -> com.hbm_m.client.render.retained.RetainedInstanceGraph.get().clear());

            // Renderer-specific caches (instancers, DAE, door skins, etc.);
            // factory machines are cleared via MachineRenderRegistry.clearAll().
            guarded("MachineRenderRegistry.clearAll",
                    com.hbm_m.client.render.machine.MachineRenderRegistry::clearAll);
            guarded("MachineDoorRenderer.clearDaeCaches",
                    com.hbm_m.client.render.implementations.MachineDoorRenderer::clearDaeCaches);
            guarded("MachineDoorRenderer.resetUvRectAttribCheck",
                    com.hbm_m.client.render.implementations.MachineDoorRenderer::resetUvRectAttribCheck);

            // Registered invalidators (machine specs, special effects)
            for (InvalidationHook hook : HOOKS) {
                try {
                    hook.invalidate(reason);
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("RenderCacheManager: hook {} failed", hook, t);
                }
            }

            // Shared engine compilers/caches
            guarded("MeshRenderCache.clearAll", MeshRenderCache::clearAll);
            guarded("LightSampleCache.invalidateAll", LightSampleCache::invalidateAll);
            guarded("OcclusionCullingHelper.clearCache", OcclusionCullingHelper::clearCache);

            guarded("AbstractObjArmorLayer.clearAllCaches",
                    com.hbm_m.powerarmor.layer.AbstractObjArmorLayer::clearAllCaches);

            // GPU-compute subsystems + keyed caches: their GL storage (HiZ pyramid
            // texture, culler/scatter SSBOs) lives until its explicit destroy and
            // must not survive a reload/disconnect; the sampler-location cache is
            // keyed by pipeline generation and grows across Iris reloads.
            guarded("HiZDepthPyramid.destroy",
                    () -> com.hbm_m.client.render.culling.HiZDepthPyramid.get().destroy());
            guarded("NucleusGpuCuller.destroy",
                    com.hbm_m.client.render.culling.NucleusGpuCuller::destroy);
            guarded("ComputeScatterUploader.destroy",
                    com.hbm_m.client.render.ComputeScatterUploader::destroy);
            guarded("SingleMeshVboRenderer.clearSamplerLocationCache",
                    com.hbm_m.client.render.SingleMeshVboRenderer::clearSamplerLocationCache);

            MainRegistry.LOGGER.info("Render cache invalidation completed ({})", reason);
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("Error during render cache invalidation", t);
        }
    }

    /**
     * One teardown step with its own guard: a throw from one cache must not
     * cancel the frees of the others (GL storage lives until its Delete* call
     * and these steps are the only free paths - see the audit 2026-10-05 §3.11).
     */
    private static void guarded(String name, Runnable step) {
        try {
            step.run();
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("RenderCacheManager: step {} failed", name, t);
        }
    }
}
