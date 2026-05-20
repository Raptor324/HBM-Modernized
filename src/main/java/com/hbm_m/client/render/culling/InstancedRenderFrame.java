package com.hbm_m.client.render.culling;

import org.joml.Matrix4f;

import com.hbm_m.client.render.MdiBatchCoordinator;
import com.hbm_m.client.render.implementations.DoorRenderer;
import com.hbm_m.client.render.implementations.MachineAdvancedAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineChemicalPlantRenderer;
import com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer;
import com.hbm_m.client.render.implementations.MachinePressRenderer;
import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Instanced / MDI: accumulate instances while block entities render (possibly many
 * chunk slices per visual frame with ModernFix / similar), then flush once per
 * {@linkplain net.minecraftforge.event.TickEvent.RenderTickEvent render tick}.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class InstancedRenderFrame {

    private static final Matrix4f DEFERRED_PROJECTION = new Matrix4f();
    private static Vec3 deferredCamera = Vec3.ZERO;
    private static boolean deferredPresent;
    private static int chunkSliceCount;

    private InstancedRenderFrame() {}

    /**
     * Called from each {@code AFTER_BLOCK_ENTITIES} (per chunk slice). Only stores
     * projection / camera; instances stay in per-renderer buffers until flush.
     */
    public static void deferPresent(Matrix4f projection, Vec3 cameraPos) {
        if (!ModClothConfig.useInstancedBatching()) {
            return;
        }
        if (!RenderSystem.isOnRenderThread() || projection == null) {
            return;
        }
        chunkSliceCount++;
        DEFERRED_PROJECTION.set(projection);
        deferredCamera = cameraPos;
        deferredPresent = true;
    }

    /**
     * Once per frame, from {@code AFTER_LEVEL} (Forge) or {@code WorldRenderEvents.END}
     * (Fabric). Must fire while the world framebuffer and depth buffer are still active;
     * firing later (e.g. {@code RenderTickEvent.END}) draws after blit/swap and produces
     * broken depth + missing textures.
     */
    public static void flushDeferredPresent() {
        if (!deferredPresent) {
            IrisRenderBatch.closePersistentIfActive();
            return;
        }
        if (!RenderSystem.isOnRenderThread()) {
            return;
        }

        int slices = chunkSliceCount;
        chunkSliceCount = 0;
        deferredPresent = false;

        InstancedRenderStats.beginPresent(slices);
        presentNow(DEFERRED_PROJECTION, deferredCamera);
        InstancedRenderStats.endPresent();
    }

    private static void presentNow(Matrix4f projection, Vec3 cameraPos) {
        IrisRenderBatch.closePersistentIfActive();

        ModClothConfig cfg = ModClothConfig.get();
        boolean useMdi = cfg.useMultiDrawIndirect && MdiBatchCoordinator.isMdiAvailable();

        try {
            if (useMdi) {
                OcclusionCullingHelper.runGpuCullDispatchBeforeMdi(projection, cameraPos);
                MdiBatchCoordinator session = MdiBatchCoordinator.beginFrame(projection);
                flushAllInstanced(projection);
                if (session != null) {
                    session.endFrame(false);
                }
                OcclusionCullingHelper.runGpuCullingAfterBlockEntities(projection, cameraPos);
            } else {
                flushAllInstanced(projection);
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M] instanced present failed", t);
            MdiBatchCoordinator.discardActiveSessionNoDispatch();
        }

        IrisExtendedShaderAccess.tickPass();
        LightSampleCache.onFrameStart();
        OcclusionCullingHelper.onFrameStart();
        MdiRenderFrameGate.advanceAfterPresent();
    }

    private static void flushAllInstanced(Matrix4f projection) {
        MachineAdvancedAssemblerRenderer.flushInstancedBatches(projection);
        MachineHydraulicFrackiningTowerRenderer.flushInstancedBatches(projection);
        MachineAssemblerRenderer.flushInstancedBatches(projection);
        DoorRenderer.flushInstancedBatches(projection);
        MachinePressRenderer.flushInstancedBatches(projection);
        MachineChemicalPlantRenderer.flushInstancedBatches(projection);
    }

    public static void clear() {
        deferredPresent = false;
        chunkSliceCount = 0;
        MdiRenderFrameGate.reset();
        InstancedRenderStats.clear();
        MdiBatchCoordinator.cancelScheduledDraw();
        MdiBatchCoordinator.clearCachedRedraw();
        MdiBatchCoordinator.discardActiveSessionNoDispatch();
    }
}
