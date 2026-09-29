package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.lang.reflect.Field;

import org.joml.Matrix4f;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractPartBasedRenderer<T extends BlockEntity, M extends BakedModel>
        implements com.hbm_m.client.render.HbmBerBounds<T> {

    /**
     * Gets the model for rendering. By default - from the blockstate.
     * Can be overridden to pick a model from BlockEntity data (e.g. doors with different skins).
     */
    protected BakedModel getModel(T blockEntity) {
        return Minecraft.getInstance().getBlockRenderer()
            .getBlockModel(blockEntity.getBlockState());
    }

    protected abstract M getModelType(BakedModel rawModel);
    protected abstract Direction getFacing(T blockEntity);
    protected abstract void renderParts(T blockEntity, M model, LegacyAnimator animator, float partialTick,
                                        int packedLight, int packedOverlay, PoseStack poseStack, MultiBufferSource bufferSource);

    /** Rotation/offset of the block in local coordinates before {@link #renderParts}. */
    protected void setupBlockTransform(LegacyAnimator animator, T blockEntity) {
        animator.setupBlockTransform(getFacing(blockEntity));
    }

    /**
     * Snapshot of the most-recent {@code poseStack.last().pose()} captured at the
     * start of {@link #render}. Reused (mutated in place) rather than reallocated
     * to keep this hot per-BE method allocation-free; downstream callers that
     * need a stable copy go through {@link #getCurrentModelViewMatrix()} which
     * does the defensive copy on demand.
     */
    protected final Matrix4f currentModelViewMatrix = new Matrix4f();
    
    /** Defensive copy - callers may not mutate the renderer's snapshot field. */
    public Matrix4f getCurrentModelViewMatrix() {
        return new Matrix4f(currentModelViewMatrix);
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }

    @Override
    public int getViewDistance() {
        return RenderDistanceHelper.getStaticViewDistanceBlocks();
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Frustum cull FIRST for the main pass. Shadow pass uses light-space
        // bounds; the main-camera frustum here would drop off-screen casters.
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            // 1.21.1 diagnostics (machines cast no shadows under Iris): counter of
            // BER calls inside the shadow pass; logged from ClientModEvents
            // (AFTER_SKY of the main pass). 0 calls = Iris never calls BER in
            // shadow (empty shadow BE list, terrain-mod interop) - then the
            // problem is not in our draw path.
            SHADOW_BER_INVOCATIONS++;
        } else if (!isInViewFrustum(blockEntity)) {
            return;
        }

        // Mutate the persistent snapshot in place - no Matrix4f allocation per
        // BE per pass. The field is private and only read by getCurrentModelViewMatrix
        // (which makes its own defensive copy), so the in-place update is safe.
        currentModelViewMatrix.set(poseStack.last().pose());

        BakedModel rawModel = getModel(blockEntity);
        // Continuity (via Connector/FFAPI) wraps all blockstate models in CtmBakedModel/
        // EmissiveBakedModel, which extend ForwardingBakedModel (Fabric FRAPI).
        // Unwrap so the instanceof check in getModelType() works correctly.
        rawModel = unwrapFabricForwardingModels(rawModel);
        M model = getModelType(rawModel);
        
        if (model == null) return;

        LegacyAnimator animator = LegacyAnimator.create(poseStack);

        com.hbm_m.client.render.LightSampleCache.BASE_POSE.get().set(poseStack.last().pose());
        com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(true);

        poseStack.pushPose();
        try {
            setupBlockTransform(animator, blockEntity);
            renderParts(blockEntity, model, animator, partialTick, packedLight, packedOverlay, poseStack, bufferSource);
        } finally {
            poseStack.popPose();
            com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(false);
        }
    }

    protected final Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    // -----------------------------------------------------------------------
    // Shared culling and fade helpers for implementations
    // -----------------------------------------------------------------------

    /** Occlusion/frustum culling over the AABB from {@link #frustumCullBounds}. */
    protected final boolean passesOcclusionCulling(T blockEntity) {
        return passesOcclusionCulling(blockEntity, frustumCullBounds(blockEntity));
    }

    /** Engine-facing access to this BE's AABB (shared light computations, etc.). */
    protected final AABB renderBounds(T blockEntity) {
        return frustumCullBounds(blockEntity);
    }

    /**
     * Visibility check via the BE's level: inside a Create contraption BE.getLevel() is
     * a VirtualRenderWorld; shouldRender() recognizes it and skips frustum/ray-march
     * culling.
     */
    protected final boolean passesOcclusionCulling(T blockEntity, AABB bounds) {
        return OcclusionCullingHelper.shouldRender(blockEntity, bounds);
    }

    /**
     * Shared culling + static fade logic: skips rendering beyond the distance and
     * sets {@link SingleMeshVboRenderer#setFadeAlpha}.
     *
     * @return fade in [0,1], or -1 if rendering should be skipped.
     */
    protected final float applyCullingAndStaticFade(T blockEntity) {
        return applyCullingAndStaticFade(blockEntity, frustumCullBounds(blockEntity));
    }

    protected final float applyCullingAndStaticFade(T blockEntity, AABB bounds) {
        return applyCullingAndStaticFade(blockEntity, bounds,
                RenderDistanceHelper.distanceSqToCamera(blockEntity.getBlockPos()));
    }

    /** Variant with a precomputed distance - no repeated camera fetch (MachineBer cache per frame). */
    protected final float applyCullingAndStaticFade(T blockEntity, AABB bounds, double distSq) {
        if (!passesOcclusionCulling(blockEntity, bounds)) {
            NucleusDebug.recordMachineCulled();
            return -1f;
        }
        float staticFade = RenderDistanceHelper.computeStaticFade(blockEntity, distSq);
        if (staticFade < 0) {
            NucleusDebug.recordMachineCulled();
            return -1f;
        }
        SingleMeshVboRenderer.setFadeAlpha(staticFade);
        NucleusDebug.recordMachineRendered();
        return staticFade;
    }

    /** Shadow pass diagnostics: BER calls since the last reset. See render(). */
    protected static int SHADOW_BER_INVOCATIONS = 0;

    /** Reads and resets the shadow-pass BER call counter (once per frame from AFTER_SKY). */
    public static int drainShadowBerInvocations() {
        int v = SHADOW_BER_INVOCATIONS;
        SHADOW_BER_INVOCATIONS = 0;
        return v;
    }

    protected boolean isInViewFrustum(T blockEntity) {
        return isInViewFrustum(blockEntity, frustumCullBounds(blockEntity));
    }

    /** Variant with a precomputed AABB - the bbox is computed once per (machine, frame). */
    protected final boolean isInViewFrustum(T blockEntity, AABB bounds) {
        // Contraption (Create train, etc.): the BE lives on a fake level and
        // getRenderBoundingBox() returns a local-space AABB, which the world-space
        // frustum rejects -> the model is invisible (the shadow still renders,
        // since the shadow pass skips this check). Skip position-based culling.
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(blockEntity)) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) {
            return true;
        }
        Frustum frustum = mc.levelRenderer.getFrustum();
        if (frustum == null) {
            return true;
        }
        return frustum.isVisible(bounds);
    }

    /**
     * AABB for the frustum test before the expensive occlusion ray-march.
     * Forge: {@link net.minecraftforge.common.extensions.IForgeBlockEntity#getRenderBoundingBox()}.
     * Fabric: only known subclasses with an explicit method (others - 1 block + margin).
     */
    private static AABB frustumCullBounds(BlockEntity blockEntity) {
        return com.hbm_m.platform.RenderHooks.getRenderBoundingBox(blockEntity);
    }

    // -----------------------------------------------------------------------
    // Fabric FRAPI compatibility (Continuity, Emissive, etc.)
    // -----------------------------------------------------------------------

    /**
     * The 'wrapped' field of ForwardingBakedModel (Fabric FRAPI).
     * Cached on the first successful lookup; null if FRAPI is unavailable.
     */
    private static Field fabricWrappedField;
    private static boolean fabricWrappedFieldChecked = false;

    /**
     * Unwraps a chain of {@code ForwardingBakedModel} wrappers (Continuity CtmBakedModel,
     * EmissiveBakedModel, etc.) down to the original model.
     *
     * <p>Continuity via Connector wraps all blockstate models in {@code CtmBakedModel}
     * (extends {@code ForwardingBakedModel}), which makes the instanceof checks in
     * {@link #getModelType} return null and the block invisible.
     */
    /**
     * Unwrap cache keyed by model identity (weak keys: wrappers are GC-collected
     * after reload). Previously the class hierarchy walk ran for every BE on every
     * pass - now a model is wrapped (or not) once and forever.
     */
    private static final java.util.Map<BakedModel, BakedModel> UNWRAP_CACHE =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static BakedModel unwrapFabricForwardingModels(BakedModel model) {
        if (model == null) return null;

        if (!fabricWrappedFieldChecked) {
            fabricWrappedFieldChecked = true;
            try {
                Class<?> cls = Class.forName("net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel");
                Field f = cls.getDeclaredField("wrapped");
                f.setAccessible(true);
                fabricWrappedField = f;
            } catch (ClassNotFoundException ignored) {
                // FRAPI not present in this environment
            } catch (Exception e) {
                MainRegistry.LOGGER.warn("[HBM] Failed to get field ForwardingBakedModel.wrapped: {}", e.toString());
            }
        }
        if (fabricWrappedField == null) return model;

        BakedModel cached = UNWRAP_CACHE.get(model);
        if (cached != null) {
            return cached;
        }
        BakedModel unwrapped = unwrapFabricForwardingModelsImpl(model);
        UNWRAP_CACHE.put(model, unwrapped);
        return unwrapped;
    }

    private static BakedModel unwrapFabricForwardingModelsImpl(BakedModel model) {
        if (model == null) return null;

        int depth = 0;
        while (depth++ < 8) {
            Class<?> cls = model.getClass();
            boolean isFrapi = false;
            while (cls != null && cls != Object.class) {
                if (cls.getName().equals("net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel")) {
                    isFrapi = true;
                    break;
                }
                cls = cls.getSuperclass();
            }
            if (!isFrapi) break;

            try {
                BakedModel inner = (BakedModel) fabricWrappedField.get(model);
                if (inner == null || inner == model) break;
                if (depth == 1) {
                    MainRegistry.LOGGER.debug("[HBM] Unwrapping {} -> {}",
                            model.getClass().getSimpleName(), inner.getClass().getSimpleName());
                }
                model = inner;
            } catch (Exception e) {
                break;
            }
        }
        return model;
    }
}
