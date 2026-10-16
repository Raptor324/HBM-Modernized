package com.hbm_m.client.render.culling;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.FrameViewState;
import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.MdiBatchCoordinator;
import com.hbm_m.client.render.NucleusDebug;
import com.hbm_m.client.render.PersistentUploadStaging;
import com.hbm_m.client.render.RenderFrameLight;
import com.hbm_m.client.render.implementations.MachineAdvancedAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineChemicalPlantRenderer;
import com.hbm_m.client.render.implementations.MachineCrystallizerRenderer;
import com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer;
import com.hbm_m.client.render.implementations.MachinePressRenderer;
import com.hbm_m.client.render.implementations.MachineRadarRenderer;
import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/**
 * Instanced / MDI batching for OBJ machine parts on vanilla Forge world render stages.
 *
 * <p>One client <em>render frame</em> = one {@code AFTER_BLOCK_ENTITIES} flush + present.
 * Do not defer draw to {@code Level#getGameTime()} - several render frames share one tick,
 * buffers would fill to the per-part instance cap ({@link ClientRenderFlags#maxInstances()})
 * and machines would strobe (overflow warnings in log).
 *
 * <p>REGRESSION STOP: present here, not {@code AFTER_LEVEL} / {@code RenderTickEvent.END}
 * (dirty GL texture units lead to a white lightmap).
 */

@OnlyIn(Dist.CLIENT)
public final class InstancedRenderFrame {

    private InstancedRenderFrame() {}

    /** {@code AFTER_ENTITIES}: frustum + CPU occlusion cache before BER. */
    public static void onBeforeBlockEntities(Matrix4f projection, Vec3 cameraPos,
                                            @Nullable Frustum blockEntityFrustum) {
        if (projection == null) {
            return;
        }
        if (cameraPos != null) {
            FrameViewState.checkAnchorDrift(cameraPos);
        }
        RenderFrameLight.onFrameStart();
        ClientRenderFlags.onFrameStart();
        // Shadow phase has already run: an unflushed global shadow batch means the
        // mixin did not fire -> automatic fallback to the immediate shadow path.
        com.hbm_m.client.render.IrisShadowBatchCollector.onMainPassFrameStart();
        com.hbm_m.client.render.shader.IrisInstancedShaders.onFrameStart();
        // Per-frame counters for the F3 ([Nucleus]) section; the overlay draws later in this frame.
        NucleusDebug.onFrameStart();
        // Detect Iris pipeline rebuilds before BER so cached ExtendedShader /
        // sampler bindings are not reused with destroyed GlResources.
        IrisExtendedShaderAccess.tickPass();
        MachineChemicalPlantRenderer.clearDeferredFluids();
        MachineCrystallizerRenderer.clearDeferredFluids();
        // Single source of truth: the effective occlusion mode. The legacy
        // enableOcclusionCulling boolean is only a config-load mirror - gating the
        // CPU helper on it left ray-marching enabled (mode=CPU) without frustum
        // capture / cache pruning when the mirror drifted.
        if (ModClothConfig.get().getEffectiveOcclusionCullingMode() == ModClothConfig.OcclusionCullingMode.CPU) {
            OcclusionCullingHelper.onFrameStart();
            OcclusionCullingHelper.captureBlockEntityPassFrustum(blockEntityFrustum);
            OcclusionCullingHelper.captureCpuFrustumFallback(projection, cameraPos);
        } else {
            OcclusionCullingHelper.captureBlockEntityPassFrustum(null);
        }
    }

    /**
     * {@code AFTER_BLOCK_ENTITIES}: flush all instanced batches for this render frame.
     */
    public static void presentAfterBlockEntities(Matrix4f projection, Vec3 cameraPos) {
        if (!RenderSystem.isOnRenderThread() || projection == null) {
            MachineChemicalPlantRenderer.clearDeferredFluids();
            MachineCrystallizerRenderer.clearDeferredFluids();
            return;
        }

        // Per-part VBO BERs open a persistent Iris batch even when instanced batching is off.
        // Must close before outline / hand - not only inside the instanced-flush branch below.
        IrisRenderBatch.closePersistentIfActive();

        try {
            // Instancing is always enabled; forceVanillaImmediatePath is checked inside
            // ClientRenderFlags.useInstancedBatching() by the BERs themselves. MDI is enabled
            // automatically (caps + no shader pack) - see MdiBatchCoordinator.beginFrame.
            RenderFrameLight.ensureLightTextureUpdated();

            MdiBatchCoordinator coord = MdiBatchCoordinator.beginFrame(projection);
            flushAllInstanced(projection);
            if (coord != null) {
                coord.endFrame();
            }

            // Phase 2: fading instances of direct (non-MDI) paths (GPU-bones chain parts
            // of assembler machines, DoorRenderer, MDI fallback) - strictly AFTER the
            // MDI multi-draw, so translucent geometry does not write depth before
            // opaque machines (depth-reject "hole in chunk" artifact).
            flushAllInstancedFading(projection);

            // All span uploads of this frame went to the staging ring - place a fence
            // guarding reuse of the range by the next frame.
            PersistentUploadStaging staging = PersistentUploadStaging.getOrCreate();
            if (staging != null) {
                staging.endFrame();
            }

            MdiRenderFrameGate.advanceAfterPresent();

            // NO useInstancedBatching guard here: even with instancing disabled the
            // non-instanced path (SingleMeshVboRenderer.render / renderSingle) still goes
            // through LightSampleCache. Without the currentFrame increment, the
            // lastFrame == currentFrame condition stays true forever - machine light
            // freezes at the first sample, and the fast-path lastQueriedBE slot keeps a
            // strong reference to the last BlockEntity (pinning the whole Level after
            // leaving a world).
            LightSampleCache.onFrameStart();

            // After the instanced flush (or with batching disabled): depth contains all BER parts.
            // Chemplant/Crystallizer are deferred: drawn here, in AFTER_BLOCK_ENTITIES,
            // after closePersistentIfActive and the instanced flush, inside the IrisPhaseGuard
            // BLOCK_ENTITIES phase (see MachineChemicalPlantRenderer.presentDeferredFluids).
            MachineChemicalPlantRenderer.presentDeferredFluids();
            MachineCrystallizerRenderer.presentDeferredFluids();
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M] instanced present failed", t);
            // Safety after the exception: fence the staging copies already submitted
            // (otherwise the next frame may overwrite an unfinished range) and
            // invalidate the camera cache (serial did not grow - without the reset the
            // next frame would reuse a stale view matrix).
            PersistentUploadStaging staging = PersistentUploadStaging.getOrCreate();
            if (staging != null) {
                staging.endFrame();
            }
            FrameViewState.invalidate();
            MdiBatchCoordinator.discardActiveSessionNoDispatch();
            MachineChemicalPlantRenderer.clearDeferredFluids();
            MachineCrystallizerRenderer.clearDeferredFluids();
        }
    }

    /** {@code AFTER_LEVEL}: Iris persistent batch cleanup only. */
    public static void onRenderSliceEnd() {
        IrisRenderBatch.closePersistentIfActive();
    }

    /** @deprecated use {@link #onBeforeBlockEntities} */
    @Deprecated
    public static void onBeforeBlockEntitySlices(Matrix4f projection, Vec3 cameraPos,
                                                 @Nullable Frustum blockEntityFrustum) {
        onBeforeBlockEntities(projection, cameraPos, blockEntityFrustum);
    }

    /** @deprecated use {@link #presentAfterBlockEntities} */
    @Deprecated
    public static void accumulateSliceInstances(Matrix4f projection, Vec3 cameraPos) {
        presentAfterBlockEntities(projection, cameraPos);
    }

    /** @deprecated use {@link #onBeforeBlockEntities} */
    @Deprecated
    public static void onRenderFrameBegin(float partialTick, Matrix4f projection, Vec3 cameraPos) {
        onBeforeBlockEntities(projection, cameraPos, null);
    }

    /** @deprecated use {@link #onRenderSliceEnd} */
    @Deprecated
    public static void flushDeferredPresent(Matrix4f projection, Vec3 cameraPos) {
        onRenderSliceEnd();
    }

    public static void flushPendingNow(Matrix4f projection, Vec3 cameraPos) {
        presentAfterBlockEntities(projection, cameraPos);
    }

    private static void flushAllInstanced(Matrix4f projection) {
        // Factory machines (machine/) - single registry instead of N hardcoded flushInstancedBatches.
        com.hbm_m.client.render.machine.MachineRenderRegistry.flushAll(projection);
    }

    /**
     * Phase 2 of the flush: fading instances of direct (non-MDI) paths. Frame invariant:
     * "opaque of all paths -> fading MDI (inside the multi-draw) -> fading of direct paths".
     */
    private static void flushAllInstancedFading(Matrix4f projection) {
        com.hbm_m.client.render.machine.MachineRenderRegistry.flushAllFading(projection);
    }

    public static void clear() {
        MdiRenderFrameGate.reset();
        InstancedRenderStats.clear();
        NucleusDebug.onFrameStart();
        MdiBatchCoordinator.discardActiveSessionNoDispatch();
        MdiBatchCoordinator.clearCachedRedraw();
    }
}
