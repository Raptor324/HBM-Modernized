package com.hbm_m.client.render.culling;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.MdiBatchCoordinator;
import com.hbm_m.client.render.RenderFrameLight;
import com.hbm_m.client.render.implementations.DoorRenderer;
import com.hbm_m.client.render.implementations.MachineAdvancedAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineChemicalPlantRenderer;
import com.hbm_m.client.render.implementations.MachineCrystallizerRenderer;
import com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer;
import com.hbm_m.client.render.implementations.MachinePressRenderer;
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
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Instanced / MDI batching for OBJ machine parts on vanilla Forge world render stages.
 *
 * <p>One client <em>render frame</em> = one {@code AFTER_BLOCK_ENTITIES} flush + present.
 * Do not defer draw to {@code Level#getGameTime()} — several render frames share one tick,
 * buffers would fill to {@link com.hbm_m.client.render.InstancedStaticPartRenderer#MAX_INSTANCES}
 * and machines would strobe (overflow warnings in log).
 *
 * <p>РЕГРЕССИЯ-СТОП: present here, not {@code AFTER_LEVEL} / {@code RenderTickEvent.END}
 * (dirty GL texture units → white lightmap).
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class InstancedRenderFrame {

    private InstancedRenderFrame() {}

    /** {@code AFTER_ENTITIES}: frustum + CPU occlusion cache before BER. */
    public static void onBeforeBlockEntities(Matrix4f projection, Vec3 cameraPos,
                                            @Nullable Frustum blockEntityFrustum) {
        if (projection == null) {
            return;
        }
        RenderFrameLight.onFrameStart();
        ClientRenderFlags.onFrameStart();
        // Detect Iris pipeline rebuilds before BER so cached ExtendedShader /
        // sampler bindings are not reused with destroyed GlResources.
        IrisExtendedShaderAccess.tickPass();
        MachineChemicalPlantRenderer.clearDeferredFluids();
        MachineCrystallizerRenderer.clearDeferredFluids();
        if (ClientRenderFlags.enableOcclusionCulling()) {
            OcclusionCullingHelper.onFrameStart();
            OcclusionCullingHelper.captureBlockEntityPassFrustum(blockEntityFrustum);
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

        try {
            if (ClientRenderFlags.useInstancedBatching()) {
                ModClothConfig cfg = ModClothConfig.get();
                boolean useMdi = cfg.useMultiDrawIndirect && MdiBatchCoordinator.isMdiAvailable();

                IrisRenderBatch.closePersistentIfActive();
                RenderFrameLight.ensureLightTextureUpdated();

                if (useMdi) {
                    MdiBatchCoordinator coord = MdiBatchCoordinator.beginFrame(projection);
                    flushAllInstanced(projection);
                    if (coord != null) {
                        coord.endFrame(false);
                    }
                } else {
                    flushAllInstanced(projection);
                }

                LightSampleCache.onFrameStart();
                MdiRenderFrameGate.advanceAfterPresent();
            }

            // После flush instanced (или при выключенном batching): depth содержит все части BER.
            MachineChemicalPlantRenderer.presentDeferredFluids();
            MachineCrystallizerRenderer.presentDeferredFluids();
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M] instanced present failed", t);
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
        MachineAdvancedAssemblerRenderer.flushInstancedBatches(projection);
        MachineHydraulicFrackiningTowerRenderer.flushInstancedBatches(projection);
        MachineAssemblerRenderer.flushInstancedBatches(projection);
        DoorRenderer.flushInstancedBatches(projection);
        MachinePressRenderer.flushInstancedBatches(projection);
        MachineChemicalPlantRenderer.flushInstancedBatches(projection);
        MachineCrystallizerRenderer.flushInstancedBatches(projection);
    }

    public static void clear() {
        MdiRenderFrameGate.reset();
        InstancedRenderStats.clear();
        MdiBatchCoordinator.cancelScheduledDraw();
        MdiBatchCoordinator.discardActiveSessionNoDispatch();
        MdiBatchCoordinator.clearCachedRedraw();
    }
}
