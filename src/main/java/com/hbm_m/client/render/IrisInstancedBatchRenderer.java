package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms;
import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.IrisShaderApply;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Iris/Oculus companion-mesh renderer: handles {@code flushBatchIris},
 * single-instance Iris draws through {@code ExtendedShader}, and
 * companion mesh lifecycle.
 * <p>
 * Extracted from {@link InstancedStaticPartRenderer} to reduce
 * class complexity.
 *
 * @credit Iris / coderbot / IMS
 * @credit Flywheel / janh
 */

@OnlyIn(Dist.CLIENT)
final class IrisInstancedBatchRenderer {

    private final InstancedStaticPartRenderer parent;

    @Nullable
    private IrisCompanionMesh irisCompanion;
    private boolean irisCompanionAttempted;

    private IrisDerivedMatrixUniforms.Locations cachedMatrixLocs = IrisDerivedMatrixUniforms.Locations.NONE;
    private long cachedMatrixPipelineGeneration = -1L;
    private int cachedMatrixProgramId = -1;
    private ShaderInstance cachedMatrixShader;

    private final Matrix4f tmpInstanceMat = new Matrix4f();

    /** Scratch MV composite for a single Iris draw (rendering is single-threaded). */
    private final Matrix4f tmpSingleMv = new Matrix4f();
    /** Scratch: composed shadow model-view matrix for recording into the global shadow batch. */
    private final Matrix4f tmpShadowMv = new Matrix4f();
    /** Scratch: instance world transformation = inv(shadowMV) * composed. */
    private final Matrix4f tmpShadowWorld = new Matrix4f();
    private final Vector3f tmpShadowPos = new Vector3f();
    private final Quaternionf tmpShadowRot = new Quaternionf();
    /** Cached shadow model-view and inverse matrices (constant throughout the shadow pass). */
    private final Matrix4f shadowMvCache = new Matrix4f();
    private final Matrix4f shadowInvMvCache = new Matrix4f();
    private boolean shadowInvValid = false;
    /** Scratch: R_cam (view rotation) for converting world-space records to view space. */
    private final Matrix4f irisCamRotMat = new Matrix4f();
    /** Scratch: instance view-space pose = R_cam * T(world - cam) * R_world. */
    private final Matrix4f irisViewPoseMat = new Matrix4f();
    private final Quaternionf irisQuatTmp = new Quaternionf();
    private static final Matrix4f IDENTITY = new Matrix4f();

    private final float[] irisMvFloats = new float[16];
    private final float[] irisMvInverseFloats = new float[16];
    private final float[] irisNormalMatFloats = new float[9];
    private final Matrix4f irisMvInverseTmp = new Matrix4f();
    private final org.joml.Matrix3f irisNormalTmp = new org.joml.Matrix3f();
    final float[] irisSingleUV = new float[2];

    /** Per-instance slot index into {@link IrisCompanionMesh}'s per-vertex lightmap VBO. */
    private final int[] instanceLightmapSlot;
    private final short[] tmpCornerShort;

    IrisInstancedBatchRenderer(InstancedStaticPartRenderer parent) {
        this.parent = parent;
        this.instanceLightmapSlot = new int[parent.maxInstances];
        this.tmpCornerShort = new short[parent.lightFloatCount];
    }

    // ── Iris companion mesh ────────────────────────────────────────────

    @Nullable
    IrisCompanionMesh getOrBuildIrisCompanion() {
        if (irisCompanion != null && irisCompanion.isBuilt()) return irisCompanion;
        if (irisCompanion != null && irisCompanion.isFailed()) return null;
        if (irisCompanionAttempted && irisCompanion == null) return null;

        if (parent.quadsForIris == null || parent.quadsForIris.isEmpty()) {
            irisCompanionAttempted = true;
            return null;
        }
        if (irisCompanion == null) {
            irisCompanion = new IrisCompanionMesh(parent.quadsForIris);
            irisCompanionAttempted = true;
        }
        return irisCompanion.ensureBuilt() ? irisCompanion : null;
    }

    @Nullable
    IrisCompanionMesh getCompanion() {
        return irisCompanion;
    }

    void invalidateIrisLocations() {
        this.cachedMatrixLocs = IrisDerivedMatrixUniforms.Locations.NONE;
        this.cachedMatrixPipelineGeneration = -1L;
        this.cachedMatrixProgramId = -1;
        this.cachedMatrixShader = null;
    }

    private IrisDerivedMatrixUniforms.Locations resolveMatrixLocs(ShaderInstance shader) {
        int programId = shader.getId();
        long gen = IrisExtendedShaderAccess.getPipelineGeneration();
        if (cachedMatrixProgramId == programId
                && cachedMatrixShader == shader
                && cachedMatrixPipelineGeneration == gen) {
            return cachedMatrixLocs;
        }
        cachedMatrixProgramId = programId;
        cachedMatrixShader = shader;
        cachedMatrixPipelineGeneration = gen;
        cachedMatrixLocs = IrisDerivedMatrixUniforms.resolve(shader);
        return cachedMatrixLocs;
    }

    // ── Corner sampling ────────────────────────────────────────────────

    void sampleCornersForSingleDraw(PoseStack poseStack, BlockPos blockPos,
                                    @Nullable BlockEntity blockEntity, int packedLight) {
        BlockPos anchor = (blockEntity != null) ? blockEntity.getBlockPos() : blockPos;
        if (anchor == null) anchor = BlockPos.ZERO;
        if (LightSampleCache.BASE_POSE_SET.get()) {
                parent.tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(poseStack.last().pose());
        } else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            //? if < 1.21.1 {
            parent.tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
            //?} else {
            /*// rotation(camera.rotation()) = R_cam⁻¹ without extra invert (see
            // FrameViewState.capture and fillInstanceCornerLight).
            parent.tmpInvViewRot.identity().rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            *///?}
            parent.tmpLocalPose.set(parent.tmpInvViewRot).mul(poseStack.last().pose());
            parent.tmpLocalPose.m30(parent.tmpLocalPose.m30() - (float) (anchor.getX() - cam.x));
            parent.tmpLocalPose.m31(parent.tmpLocalPose.m31() - (float) (anchor.getY() - cam.y));
            parent.tmpLocalPose.m32(parent.tmpLocalPose.m32() - (float) (anchor.getZ() - cam.z));
        }
        long partHash = System.identityHashCode(parent);
        LightSampleCache.getOrSample8(blockEntity, partHash, parent.objBbox, anchor,
                                      parent.tmpLocalPose, packedLight, parent.tmpCornerUV);
    }

    // ── Single draw with Iris ExtendedShader ───────────────────────────

    /**
     * Batched shadow path: instead of an immediate {@code drawCompanion}, records the
     * instance into the global shadow batch ({@link IrisShadowBatchCollector}) —
     * flushed once per entire shadow BlockEntity phase via Iris mixin, submitting
     * per-instance draws through the shaderpack's SHADOW_* program (see flushGlobalShadowBatch).
     * <p>
     * Iris passes the BER a PoseStack of {@code shadowModelView * T(bePos - camPos)}
     * (ShadowRenderer.renderBlockEntities transforms the stack to bePos - cam on top of
     * createShadowModelView). Recording is an exact decomposition T(pos) * R(rot) of this
     * pose: getTranslation/getNormalizedRotation from an affine matrix with an orthonormal
     * 3x3 are losslessly invertible, restoring the original pose bit-for-bit upon flush.
     */
    boolean tryRecordShadowInstance(PoseStack poseStack, int packedLight,
                                    BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        if (!IrisShadowBatchCollector.isBatchingEnabled()) {
            return false;
        }
        Matrix4f currentMv = RenderSystem.getModelViewMatrix();
        // Stash every frame (flush resets stashValid; conditional calls broke batching from the second frame).
        IrisShadowBatchCollector.stashShadowMatrices(RenderSystem.getProjectionMatrix());
        if (!shadowInvValid || !shadowMvCache.equals(currentMv)) {
            shadowMvCache.set(currentMv);
            shadowInvMvCache.set(shadowMvCache).invertAffine();
            shadowInvValid = true;
        }
        tmpShadowMv.set(currentMv).mul(poseStack.last().pose());
        tmpShadowWorld.set(shadowInvMvCache).mul(tmpShadowMv);
        // Light is unneeded in shadow pass (depth/shadowcolor only) — write zeros instead of expensive 8-corner sampling.
        java.util.Arrays.fill(parent.tmpCornerUV, 0.0f);
        IrisShadowBatchCollector.record(parent, tmpShadowWorld, parent.objBbox, parent.tmpCornerUV);
        return true;
    }

    boolean drawSingleWithIrisExtended(PoseStack poseStack, int packedLight,
                                       BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        // Shadow pass: only through ACTIVE per-BE batch (see SingleMeshVboRenderer.renderWithIrisExtended
        // and IrisRenderBatch.begin). Standalone path in shadow pass is prohibited. Without batching,
        // returns false so caller falls through to bufferSource.putBulkData (SHADOW_BLOCK on endBatch).
        if (ShaderCompatibilityDetector.isRenderingShadowPass() && IrisRenderBatch.active() == null) {
            return false;
        }

        IrisCompanionMesh companion = getOrBuildIrisCompanion();
        if (companion == null) return false;

        IrisRenderBatch activeBatch = IrisRenderBatch.active();
        boolean shadowPassEarly = (activeBatch != null)
                ? activeBatch.isShadowPass()
                : ShaderCompatibilityDetector.isRenderingShadowPass();

        boolean haveCorners = false;
        if (!shadowPassEarly) {
            sampleCornersForSingleDraw(poseStack, blockPos, blockEntity, packedLight);
            haveCorners = true;
        }

        IrisRenderBatch batch = IrisRenderBatch.active();
        if (batch != null) {
            // R_cam resides in RenderSystem.getModelViewMatrix() on BOTH versions (see fix in
            // InstancedStaticPartRenderer.addInstance) — composite multiplication is required.
            Matrix4f fullModelView = tmpSingleMv.set(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
            LightSampleCache.getOrSample(blockEntity, packedLight, irisSingleUV, 0);
            int blockUInt = Math.max(0, Math.min(240, Math.round(irisSingleUV[0])));
            int skyVInt   = Math.max(0, Math.min(240, Math.round(irisSingleUV[1])));
            int packedSmoothLight = (skyVInt << 16) | blockUInt;
            if (haveCorners) {
                batch.drawCompanionWithPerVertexLight(companion, fullModelView,
                        parent.tmpCornerUV, packedSmoothLight);
            } else {
                batch.drawCompanion(companion, fullModelView, packedSmoothLight);
            }
            return true;
        }

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        ShaderInstance shader = IrisExtendedShaderAccess.getBlockShader(shadowPass);
        if (shader == null) return false;

        int previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        try (RenderStateGuard stateGuard = RenderStateGuard.snapshot();
             IrisPhaseGuard ignored = IrisPhaseGuard.pushBlockEntities()) {
            RenderSystem.setShader(() -> shader);
            parent.vanillaHelper.updateUniformCache(shader);

            LightSampleCache.getOrSample(blockEntity, packedLight, irisSingleUV, 0);

            Matrix4f fullModelView = tmpSingleMv.set(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
            parent.vanillaHelper.applyCommonUniforms(shader, RenderSystem.getProjectionMatrix(), fullModelView);
            if (parent.vanillaHelper.uBrightness != null) parent.vanillaHelper.uBrightness.set(
                    parent.vanillaHelper.brightnessFromUV(irisSingleUV[0], irisSingleUV[1], Float.NaN));

            SingleMeshVboRenderer.TextureBinder.bindForModelIfNeeded(shader);
            
            companion.bindVaoIfNeeded();
            
            if (!IrisShaderApply.tryApply(shader)) {
                return false;
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            
            companion.prepareForShader(shader.getId());

            int uv2Loc = companion.getUv2Location();
            if (haveCorners && companion.supportsPerVertexLightmap()) {
                companion.ensureLightmapCapacity(1);
                companion.writeInstanceLightmap(0, parent.tmpCornerUV);
                companion.finishLightmapWrites();
                companion.activatePerVertexLightmap();
                companion.bindLightmapForInstance(0);
            } else if (uv2Loc != -1) {
                companion.restoreConstantLightmap();
                int blockUInt = Math.max(0, Math.min(240, Math.round(irisSingleUV[0])));
                int skyVInt   = Math.max(0, Math.min(240, Math.round(irisSingleUV[1])));
                companion.bindVaoIfNeeded();
                GL30.glVertexAttribI2i(uv2Loc, blockUInt, skyVInt);
            }

            companion.bindVaoIfNeeded();
            GL11.glDrawElements(GL11.GL_TRIANGLES, companion.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
            shader.clear();
            NucleusDebug.recordDraw(1, 1, "Iris single");
            return true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("IrisInstancedBatchRenderer.drawSingleWithIrisExtended failed", e);
            return false;
        } finally {
            IrisExtendedShaderAccess.restoreCurrentRenderedBlockEntity(previousBlockEntityId);
        }
    }

    // ── Batch flush (Iris) ─────────────────────────────────────────────

    /**
     * True instanced flush under Iris: our {@code ExtendedShader}
     * (hbm_m:iris/block_lit_instanced_iris — custom GLSL with iris_* uniforms) +
     * parent VAO (instance attributes 4..11, divisors configured in VAO state) +
     * ONE {@code glDrawElementsInstanced} call per part renderer.
     * <p>
     * Order is mandatory: pack shader is applied FIRST (its apply() binds
     * the active gbuffer FBO) -> {@link IrisInstancedShaders#prepare(boolean)}
     * snapshots attachments of the live FBO -> our shader draws into the same render targets.
     * If our shader is unavailable, falls back to the legacy companion per-instance path.
     */
    void flushBatchIris(Matrix4f projectionMatrix) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return;
        }
        if (parent.instanceCount == 0 || parent.instanceBuffer == null) {
            return;
        }

        // Pack shader binds active gbuffer FBO; replace program with ours.
        ShaderInstance packShader = IrisExtendedShaderAccess.getBlockShader(false);
        if (packShader == null) {
            return;
        }

        // ── Tier 1: GPU Compute Bake (main) ─────────────────────────────
        // ONE glDrawElements per part renderer using the pack's NATIVE gbuffers program —
        // the shaderpack natively encodes its own gbuffer formats. Unrecognized schemas
        // (BSL: per-instance companion = 1618 draw calls) collapse to ~27 draw calls,
        // identical to recognized ones (Photon). Instance records are anchor-relative (FrameViewState),
        // with trilinear lighting evaluated from the 8-corner bounding records.
        if (NucleusGpuBaker.isEnabled()) {
            IrisCompanionMesh bakeMesh = getOrBuildIrisCompanion();
            if (bakeMesh != null && parent.instanceCount > 0 && parent.instanceBuffer != null) {
                java.nio.FloatBuffer bakeRecords = parent.instanceBuffer.duplicate();
                bakeRecords.flip();
                Matrix4f viewRot = new Matrix4f(FrameViewState.inverseViewRotation()).transpose();
                boolean baked = false;
                try (IrisPhaseGuard guard = IrisPhaseGuard.pushBlockEntities()) {
                    if (IrisShaderApply.tryApply(packShader)) {
                        baked = NucleusGpuBaker.bakeAndDrawMain(bakeRecords, parent.instanceCount,
                                bakeMesh, packShader,
                                parent.vanillaHelper.stripViewRotationForInstanced(projectionMatrix),
                                viewRot, FrameViewState.relCamX(), FrameViewState.relCamY(),
                                FrameViewState.relCamZ());
                    }
                }
                if (baked) {
                    return;
                }
            }
        }

        // REGRESSION-STOP (black base under shaderpacks): deferred packs write gbuffers
        // using their OWN programs in custom formats (e.g. "packed" schema packs
        // albedo + flat normal + light levels via pack_unorm_2x8 into a single target
        // with RENDERTARGETS: 1). Single-target FSH with unpacked albedo*lightmap composite
        // decodes into garbage (correct geometry, black color). Therefore, without a
        // recognized encoder (IrisInstancedEncoders), instanced ExtendedShader is bypassed:
        // render through the pack's BLOCK_ENTITY program (companion per-instance — same path
        // used by animated parts, universally correct under all packs).
        if (!ClientRenderFlags.irisTrueInstancing()) {
            flushBatchIrisCompanionLegacy(projectionMatrix);
            return;
        }
        ShaderInstance ours = com.hbm_m.client.render.shader.IrisInstancedShaders.getOrCreate(false);
        if (ours == null) {
            if (legacyOnce) {
                legacyOnce = false;
                MainRegistry.LOGGER.info("[HBM-M] flushBatchIris: instanced unavailable -> legacy companion path");
            }
            // Custom instanced path unavailable (reflection/version mismatch) — fallback to legacy path.
            flushBatchIrisCompanionLegacy(projectionMatrix);
            return;
        }
        if (legacyOnce) {
            legacyOnce = false;
            MainRegistry.LOGGER.info("[HBM-M] flushBatchIris: instanced path ACTIVE (programId={})", ours.getId());
        }

        parent.instanceBuffer.flip();
        int floats = parent.instanceCount * parent.instanceDataSize;
        if (floats > parent.instanceBuffer.remaining()) {
            return;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMaskWasEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        int prevBlendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int prevBlendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int prevBlendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int prevBlendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        // REGRESSION-FIX: closePersistentIfActive prior to flush binds main-RT
        // (ExtendedShader.clear -> mainRenderTarget.bindWrite) — without re-applying
        // the pack shader, machines were drawn into main-RT and cleared by subsequent passes.
        // tryApply binds the actual gbuffer FBO; IrisPhaseGuard maintains BLOCK_ENTITIES phase.
        try (IrisPhaseGuard phaseGuard = IrisPhaseGuard.pushBlockEntities()) {
            if (!IrisShaderApply.tryApply(packShader)) {
                return;
            }
            GL30.glBindVertexArray(parent.vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, parent.instanceVboId);
            // Span diffing: only uploads modified record spans (anchor-relative coordinates
            // incur zero GPU upload traffic on static scenes).
            parent.uploadInstanceStreamToBoundVbo();
            parent.vanillaHelper.enableVertexAttribsForDraw();

            var mc = Minecraft.getInstance();
            if (mc.gameRenderer != null) {
                // At-most-once-per-frame guard: vanilla already rebuilt the lightmap
                // at the start of GameRenderer.renderLevel, and RenderFrameLight ran
                // at present start — a direct updateLightTexture here was a duplicate
                // 256×256 CPU rebuild per frame.
                RenderFrameLight.ensureLightTextureUpdated();
            }

            // Bind iris_* uniforms and replace program (useIrisProgram); preserves active pack FBO.
            parent.vanillaHelper.useIrisProgram(
                    ours, parent.vanillaHelper.stripViewRotationForInstanced(projectionMatrix),
                    FrameViewState.viewMatrix());
            SingleMeshVboRenderer.primeIrisInstancedSamplerMap(ours, Minecraft.getInstance());

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            // Divisors for attributes 4..11 are already set to 1 in VAO state during creation;
            // per-draw state is untouched, preventing state leaks to vanilla VAOs.
            InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, parent.indexCount,
                    GL11.GL_UNSIGNED_INT, 0, parent.instanceCount);
            NucleusDebug.recordDraw(1, parent.instanceCount, "Iris instanced");

            // Do NOT call ours.clear() (re-binds main-RT) or glUseProgram(0):
            // ExtendedShader.lastApplied continues tracking the pack program as active
            // and skips glUseProgram on subsequent tryApply calls, causing uniforms to target program 0.
            // Explicitly restore the pack program to keep Iris state tracking consistent.
            GL20.glUseProgram(packShader.getId());
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during instanced flush (Iris instanced)", e);
        } finally {
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            RenderSystem.depthMask(depthMaskWasEnabled);
            RenderSystem.depthFunc(previousDepthFunc);
            if (depthTestWasEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cullWasEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha);
            if (blendWasEnabled) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,
                net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            IrisExtendedShaderAccess.restoreCurrentRenderedBlockEntity(previousBlockEntityId);
        }
    }

    /**
     * Legacy companion per-instance path — fallback when ExtendedShader
     * is unavailable (reflection mismatch or older Iris/Oculus build).
     */
    private static boolean legacyOnce = true;

    private void flushBatchIrisCompanionLegacy(Matrix4f projectionMatrix) {
        IrisCompanionMesh companion = getOrBuildIrisCompanion();

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        ShaderInstance shader = IrisExtendedShaderAccess.getBlockShader(shadowPass);
        if (shader == null) {
            return;
        }

        parent.instanceBuffer.flip();
        int floats = parent.instanceCount * parent.instanceDataSize;
        if (floats > parent.instanceBuffer.remaining()) {
            return;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMaskWasEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        int prevBlendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int prevBlendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int prevBlendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int prevBlendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

        int previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        try (IrisPhaseGuard ignored = IrisPhaseGuard.pushBlockEntities()) {
            RenderSystem.setShader(() -> shader);


            parent.vanillaHelper.applyCommonUniforms(shader,
                    parent.vanillaHelper.stripViewRotationForInstanced(projectionMatrix), IDENTITY);

            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,
                net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            net.minecraft.client.Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
            net.minecraft.client.Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            SingleMeshVboRenderer.TextureBinder.bindForModelIfNeeded(shader);

            int targetVao = (companion != null) ? companion.getVaoId() : parent.vaoId;
            int targetIndexCount = (companion != null) ? companion.getIndexCount() : parent.indexCount;

            if (companion != null) {
                companion.bindVaoIfNeeded();
            } else {
                com.mojang.blaze3d.platform.GlStateManager._glBindVertexArray(targetVao);
            }
            
            if (!IrisShaderApply.tryApply(shader)) {
                return;
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            if (companion != null) {
                companion.prepareForShader(shader.getId());
            }

            final int uv2Loc = (companion != null) ? companion.getUv2Location() : -1;
            final boolean perVertexLight = companion != null
                    && companion.supportsPerVertexLightmap();

            if (perVertexLight) {
                companion.ensureLightmapCapacity(Math.max(8, parent.instanceCount));
                for (int i = 0; i < parent.instanceCount; i++) {
                    int cornerBase = i * parent.instanceDataSize + InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET;
                    long key = 1469598103934665603L;
                    for (int k = 0; k < parent.lightFloatCount; k++) {
                        float f = parent.instanceBuffer.get(cornerBase + k);
                        int q = Math.round(f);
                        if (q < 0) q = 0; else if (q > 240) q = 240;
                        tmpCornerShort[k] = (short) q;
                        key ^= (q & 0xFFFF);
                        key *= 1099511628211L;
                    }
                    long alloc = companion.allocLightmapSlot(key);
                    int slot = (int) (alloc & 0xFFFF_FFFFL);
                    boolean reused = (alloc >>> 32) != 0L;
                    instanceLightmapSlot[i] = slot;
                    if (!reused) {
                        for (int k = 0; k < parent.lightFloatCount; k++)
                            parent.tmpCornerUV[k] = (float) (tmpCornerShort[k] & 0xFFFF);
                        companion.writeInstanceLightmap(slot, parent.tmpCornerUV);
                    }
                }
                companion.finishLightmapWrites();
                companion.activatePerVertexLightmap();
            }

            IrisDerivedMatrixUniforms.Locations matrixLocs = resolveMatrixLocs(shader);
            int locModelView = matrixLocs.modelView();
            int locModelViewInverse = matrixLocs.modelViewInverse();
            int locNormalMat = matrixLocs.normalMat();

            final float[] mvFloats = irisMvFloats;
            final float[] mvInverseFloats = irisMvInverseFloats;
            final float[] normalMatFloats = irisNormalMatFloats;
            final Matrix4f mvInverseTmp = irisMvInverseTmp;
            final org.joml.Matrix3f normalTmp = irisNormalTmp;

            float lastQx = Float.NaN, lastQy = Float.NaN, lastQz = Float.NaN, lastQw = Float.NaN;
            float lastPx = Float.NaN, lastPy = Float.NaN, lastPz = Float.NaN;
            int lastBlockU = Integer.MIN_VALUE;
            int lastSkyV = Integer.MIN_VALUE;

            // Anchor-relative coordinates (FrameViewState): Iris shader expects view-space instance pose
            // -> compute R_cam * T(world - cam) * R_world for each instance.
            irisCamRotMat.set(FrameViewState.inverseViewRotation()).transpose();
            final float camX = FrameViewState.relCamX();
            final float camY = FrameViewState.relCamY();
            final float camZ = FrameViewState.relCamZ();

            for (int i = 0; i < parent.instanceCount; i++) {
                int base = i * parent.instanceDataSize;
                float px = parent.instanceBuffer.get(base);
                float py = parent.instanceBuffer.get(base + 1);
                float pz = parent.instanceBuffer.get(base + 2);
                float qx = parent.instanceBuffer.get(base + 3);
                float qy = parent.instanceBuffer.get(base + 4);
                float qz = parent.instanceBuffer.get(base + 5);
                float qw = parent.instanceBuffer.get(base + 6);

                boolean rotChanged = qx != lastQx || qy != lastQy || qz != lastQz || qw != lastQw;
                boolean posChanged = px != lastPx || py != lastPy || pz != lastPz;

                tmpInstanceMat.translationRotate(px - camX, py - camY, pz - camZ, irisQuatTmp.set(qx, qy, qz, qw));
                irisViewPoseMat.set(irisCamRotMat).mul(tmpInstanceMat);

                if (locModelView >= 0) {
                    irisViewPoseMat.get(mvFloats);
                    GL20.glUniformMatrix4fv(locModelView, false, mvFloats);
                }

                boolean haveInverseFresh = false;
                if (locModelViewInverse >= 0 && (rotChanged || posChanged)) {
                    mvInverseTmp.set(irisViewPoseMat).invertAffine();
                    mvInverseTmp.get(mvInverseFloats);
                    GL20.glUniformMatrix4fv(locModelViewInverse, false, mvInverseFloats);
                    haveInverseFresh = true;
                }
                if (locNormalMat >= 0 && rotChanged) {
                    normalTmp.set(irisViewPoseMat);
                    normalTmp.get(normalMatFloats);
                    GL20.glUniformMatrix3fv(locNormalMat, false, normalMatFloats);
                }

                if (perVertexLight) {
                    companion.bindLightmapForInstance(instanceLightmapSlot[i]);
                } else if (uv2Loc != -1) {
                    int uvBase = i * 2;
                    int blockUInt = Math.max(0, Math.min(240, Math.round(parent.instanceLightUV[uvBase])));
                    int skyVInt   = Math.max(0, Math.min(240, Math.round(parent.instanceLightUV[uvBase + 1])));
                    if (blockUInt != lastBlockU || skyVInt != lastSkyV) {
                        companion.bindVaoIfNeeded();
                        GL30.glVertexAttribI2i(uv2Loc, blockUInt, skyVInt);
                        lastBlockU = blockUInt;
                        lastSkyV = skyVInt;
                    }
                }

                if (parent.vanillaHelper.uFadeAlpha != null) {
                    parent.vanillaHelper.uFadeAlpha.set(parent.instanceBuffer.get(base + parent.instanceFadeFloatOffset));
                }

                if (companion != null) {
                    companion.bindVaoIfNeeded();
                }
                GL11.glDrawElements(GL11.GL_TRIANGLES, targetIndexCount, GL11.GL_UNSIGNED_INT, 0);

                lastQx = qx; lastQy = qy; lastQz = qz; lastQw = qw;
                lastPx = px; lastPy = py; lastPz = pz;
            }

            if (companion != null && perVertexLight) {
                companion.restoreConstantLightmap();
            }
            shader.clear();
            NucleusDebug.recordDraw(parent.instanceCount, parent.instanceCount, "Iris batch");
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during instanced flush (Iris)", e);
        } finally {
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            RenderSystem.depthMask(depthMaskWasEnabled);
            RenderSystem.depthFunc(previousDepthFunc);
            if (depthTestWasEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cullWasEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha);
            if (blendWasEnabled) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,
                net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            IrisExtendedShaderAccess.restoreCurrentRenderedBlockEntity(previousBlockEntityId);
        }
    }

    // ── Cleanup ────────────────────────────────────────────────────────

    void cleanup() {
        IrisCompanionMesh companionToDestroy = this.irisCompanion;
        this.irisCompanion = null;
        this.irisCompanionAttempted = false;
        if (companionToDestroy != null) {
            RenderSystem.recordRenderCall(companionToDestroy::destroy);
        }
    }
}
