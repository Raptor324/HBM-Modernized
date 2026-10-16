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
            InstancedRenderFrame.clear();

            // MDI GPU atlas: sessions must not survive reload/disconnect
            MdiGeometryAtlas.resetForResourceLifecycle();

            // Renderer-specific caches (instancers, DAE, door skins, etc.);
            // factory machines are cleared via MachineRenderRegistry.clearAll().
            com.hbm_m.client.render.machine.MachineRenderRegistry.clearAll();
            com.hbm_m.client.render.implementations.MachineDoorRenderer.clearDaeCaches();

            // Registered invalidators (machine specs, special effects)
            for (InvalidationHook hook : HOOKS) {
                try {
                    hook.invalidate(reason);
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("RenderCacheManager: hook {} failed", hook, t);
                }
            }

            // Shared engine compilers/caches
            MeshRenderCache.clearAll();
            LightSampleCache.invalidateAll();
            OcclusionCullingHelper.clearCache();

            com.hbm_m.powerarmor.layer.AbstractObjArmorLayer.clearAllCaches();

            MainRegistry.LOGGER.info("Render cache invalidation completed ({})", reason);
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("Error during render cache invalidation", t);
        }
    }
}
