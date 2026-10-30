package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms;
import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderBindResync;
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
    /** Scratch: the BER pose handed to the shadow collector (a light-space shadow
     * poseStack decomposition, not a world-space transform; copied from
     * {@code poseStack.last().pose()} so the collector never sees a pooled PoseStack entry). */
    private final Matrix4f tmpShadowWorld = new Matrix4f();
    private final Vector3f tmpShadowPos = new Vector3f();
    private final Quaternionf tmpShadowRot = new Quaternionf();
    /** Scratch: R_cam (view rotation) for converting anchor-relative (main-pass) records to view space. */
    private final Matrix4f irisCamRotMat = new Matrix4f();
    /** Scratch: instance view-space pose = R_cam * T(record - cam) * R (main pass). */
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
            String tag = parent.getMdiTraceTag();
            if (tag == null || tag.isEmpty()) {
                tag = parent.getClass().getSimpleName();
            }
            irisCompanion.setDebugName(tag);
            irisCompanionAttempted = true;
        }
        return irisCompanion.ensureBuilt() ? irisCompanion : null;
    }

    void onTraceTagUpdated(String tag) {
        if (irisCompanion != null && tag != null && !tag.isEmpty()) {
            irisCompanion.setDebugName(tag);
        }
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
        LightSampleCache.computeLocalPose(poseStack.last().pose(), anchor, parent.tmpLocalPose);
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
        // Invisible part (pending fade mode < 0 or a fully faded global alpha):
        // the shadow record path forces fade 1.0, so the part would cast an
        // opaque shadow. Consume the draw (return true - the fallback BER must
        // not draw it either) but write no record.
        if (parent.tmpFadeMode < 0.0f || SingleMeshVboRenderer.getFadeAlpha() <= 0.0f) {
            return true;
        }
        // Stash every frame (flush resets stashValid; conditional calls broke batching from the second frame).
        IrisShadowBatchCollector.stashShadowMatrices(RenderSystem.getProjectionMatrix());
        // The record handed to the collector is an exact pos/quat decomposition of
        // the Iris shadow poseStack MV_shadow * T(bePos - camPos) * part, i.e.
        // LIGHT-space (the light rotation rides inside the record), NOT a
        // camera-converted world-space transform. The BER pose IS the stack's top
        // entry - inv(currentMv) * currentMv cancels algebraically (the former
        // inverse-multiply roundtrip was a per-part 4x4 no-op).
        tmpShadowWorld.set(poseStack.last().pose());
        // Light is unneeded in shadow pass (depth/shadowcolor only) — write zeros instead of expensive 8-corner sampling.
        java.util.Arrays.fill(parent.tmpCornerUV, 0.0f);
        IrisShadowBatchCollector.record(parent, tmpShadowWorld, parent.objBbox, parent.tmpCornerUV);
        // SHADOW-EPOCH: feed the prebake calibration. While uncalibrated NO machine is
        // skipped, so these fallback records always flow for at least the first
        // dispatched machines; a posQuat-stable slot solves the shadow basis, a second
        // independent solve validates it (see IrisShadowBatchCollector).
        IrisShadowBatchCollector.notePrebakeCalibration(parent, blockPos, tmpShadowWorld);
        return true;
    }

    boolean drawSingleWithIrisExtended(PoseStack poseStack, int packedLight,
                                       BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        // Fully faded part (global alpha <= 0): consumed - draw nothing. The
        // vanilla MDI path discards instFade <= 0 in the vsh; this single-draw
        // path has no such discard, so the early-out is its mirror.
        if (SingleMeshVboRenderer.getFadeAlpha() <= 0.0f) {
            return true;
        }
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
            
            if (!ShaderBindResync.tryApply(shader)) {
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
     * True instanced flush under Iris: our {@code ExtendedShader} selected per
     * pack layout by IrisInstancedEncoders.shaderName — base single-target
     * (shadow) or block_lit_instanced_packed (byte-paired packing scheme, the
     * only main-pass layout with a verified encoder) — custom GLSL with
     * iris_* uniforms + parent VAO (instance attributes 4..11, divisors
     * configured in VAO state) + ONE {@code glDrawElementsInstanced} call per
     * part renderer.
     * <p>
     * Order is mandatory: pack shader is applied FIRST (its apply() binds
     * the active gbuffer FBO with the pack's own draw-buffers directive) ->
     * our program is swapped in and its outputs land in the pack's render
     * targets. If our shader is unavailable, falls back to the legacy
     * companion per-instance path.
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
        // BAKE-COLLECT on the global-batch path: the bake writes into this mesh's
        // range of the per-pass pool and the draw is DEFERRED to the single
        // end-of-phase glMultiDrawElementsIndirect (NucleusGpuBaker.presentMainBatch,
        // called from InstancedRenderFrame.presentAfterBlockEntities right after this
        // walk). A refused bake (cache-budget admission, upload or expansion
        // failure) returns false here and this mesh falls back to the Tier-2/3
        // instanced path below. The pooled flush still uses the pack's NATIVE
        // gbuffers program - the shaderpack natively encodes its own gbuffer
        // formats.
        // Instance records are anchor-relative (FrameViewState), with trilinear
        // lighting evaluated from the 8-corner bounding records.
        if (NucleusGpuBaker.isEnabled()) {
            IrisCompanionMesh bakeMesh = getOrBuildIrisCompanion();
            if (bakeMesh != null && parent.instanceCount > 0 && parent.instanceBuffer != null) {
                java.nio.FloatBuffer bakeRecords = parent.instanceBuffer.duplicate();
                bakeRecords.flip();
                // modelView * L (level basis incl. view bob): the baker multiplies
                // T(-relCam) on top, reproducing FrameViewState.viewMatrix().
                Matrix4f viewRot = new Matrix4f(FrameViewState.viewBasisRotation());
                boolean baked = false;
                try (IrisPhaseGuard guard = IrisPhaseGuard.pushBlockEntities()) {
                    // No ShaderBindResync.tryApply here: bakeMain only runs compute
                    // shaders and queues PENDING_MAIN - the pack shader is applied
                    // later in NucleusGpuBaker.presentMainBatch. An unbalanced apply
                    // here polluted ExtendedShader.lastApplied and broke the later
                    // matrix restore (first-person hand/weapon vanish).
                    baked = NucleusGpuBaker.bakeMain(bakeRecords, parent.instanceCount,
                            bakeMesh, packShader,
                            parent.vanillaHelper.stripViewRotationForInstanced(projectionMatrix),
                            viewRot, FrameViewState.relCamX(), FrameViewState.relCamY(),
                            FrameViewState.relCamZ(), parent.getRecordMutationEpoch());
                }
                if (baked) {
                    return;
                }
            }
        }

        // REGRESSION-STOP (black base under shaderpacks): deferred packs write gbuffers
        // using their OWN programs in custom formats routed through the pack's
        // RENDERTARGETS/DRAWBUFFERS directive (byte-packed pairs, octahedral normals in
        // high attachments, ...). IrisInstancedEncoders classifies the pack's layout:
        // only PACKED_SCHEME (pack_unorm_2x8 + encode_unit_vector signature) enables
        // our instanced encoder (block_lit_instanced_packed, byte-paired scheme); every
        // other layout (EXOTIC - including identity-directive packs whose content
        // encoding we cannot verify, e.g. the BSL family) keeps the companion path
        // through the pack's BLOCK_ENTITY program - writing our vanilla-parity data
        // through any other layout composites as black (correct per-instance draws,
        // universally valid under all packs).
        flushBatchIrisCompanionLegacy(projectionMatrix);
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

        // Companion build failed: the fallback draw below binds the parent VAO -
        // build it on first need (lazy own-GL).
        if (companion == null && !parent.ensureOwnGlObjects()) {
            return;
        }

        parent.instanceBuffer.flip();
        int floats = parent.instanceCount * parent.instanceDataSize;
        if (floats > parent.instanceBuffer.remaining()) {
            return;
        }

        int previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        try (RenderStateGuard stateGuard = RenderStateGuard.snapshot();
             IrisPhaseGuard ignored = IrisPhaseGuard.pushBlockEntities()) {
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
            
            if (!ShaderBindResync.tryApply(shader)) {
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
                    // FADE_MODE_INVISIBLE slot: freed/dead records draw with a
                    // degenerate (0,0,0,0) quaternion at anchorOrigin - skip them.
                    if (parent.instanceBuffer.get(i * parent.instanceDataSize + parent.instanceFadeFloatOffset) < 0.0f) {
                        continue;
                    }
                    int cornerBase = i * parent.instanceDataSize + InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET;
                    long key = IrisCompanionMesh.LIGHT_KEY_FNV_OFFSET;
                    // The record's corner light is 4 packed u8 words (2 corners per
                    // word, low byte first): word j = c2j.u, c2j.v, c2j+1.u, c2j+1.v.
                    // Float-staging index k (u at even k, v at odd k) -> word k>>2,
                    // u/v byte shift (k & 2) * 8. Decode to the 0..240 texel grid.
                    for (int k = 0; k < parent.lightFloatCount; k += 2) {
                        int word = Float.floatToRawIntBits(parent.instanceBuffer.get(cornerBase + (k >> 2)));
                        int sh = (k & 2) * 8;
                        int qu = Math.round(InstancedStaticPartRenderer.decodeLightU8(word >>> sh));
                        int qv = Math.round(InstancedStaticPartRenderer.decodeLightU8(word >>> (sh + 8)));
                        tmpCornerShort[k] = (short) qu;
                        key = IrisCompanionMesh.lightKeyStep(key, qu);
                        tmpCornerShort[k + 1] = (short) qv;
                        key = IrisCompanionMesh.lightKeyStep(key, qv);
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
            // -> compute (modelView * L) * T(world - cam) * R_world per instance
            // (L carries the view bob, re-applied here for the pack's view space).
            irisCamRotMat.set(FrameViewState.viewBasisRotation());
            final float camX = FrameViewState.relCamX();
            final float camY = FrameViewState.relCamY();
            final float camZ = FrameViewState.relCamZ();

            for (int i = 0; i < parent.instanceCount; i++) {
                int base = i * parent.instanceDataSize;
                // FADE_MODE_INVISIBLE slot: freed/dead records carry a degenerate
                // (0,0,0,0) quaternion at anchorOrigin - skip them (the vanilla
                // MDI path discards instFade <= 0 in the vsh; this legacy path
                // has no such discard).
                if (parent.instanceBuffer.get(base + parent.instanceFadeFloatOffset) < 0.0f) {
                    continue;
                }
                float px = parent.instanceBuffer.get(base);
                float py = parent.instanceBuffer.get(base + 1);
                float pz = parent.instanceBuffer.get(base + 2);
                // Quat: 4 snorm16 halves packed in words 3-4 (decode mirrors the
                // VAO's GL_SHORT normalized fetch).
                int qxy = Float.floatToRawIntBits(parent.instanceBuffer.get(base + InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET));
                int qzw = Float.floatToRawIntBits(parent.instanceBuffer.get(base + InstancedStaticPartRenderer.QUAT_FLOAT_OFFSET + 1));
                float qx = InstancedStaticPartRenderer.decodeSnorm16(qxy);
                float qy = InstancedStaticPartRenderer.decodeSnorm16(qxy >>> 16);
                float qz = InstancedStaticPartRenderer.decodeSnorm16(qzw);
                float qw = InstancedStaticPartRenderer.decodeSnorm16(qzw >>> 16);

                boolean rotChanged = qx != lastQx || qy != lastQy || qz != lastQz || qw != lastQw;
                boolean posChanged = px != lastPx || py != lastPy || pz != lastPz;

                float dx = px - camX;
                float dy = py - camY;
                float dz = pz - camZ;

                if (rotChanged) {
                    tmpInstanceMat.translationRotate(dx, dy, dz, irisQuatTmp.set(qx, qy, qz, qw));
                    irisViewPoseMat.set(irisCamRotMat).mul(tmpInstanceMat);
                    irisViewPoseMat.get(mvFloats);
                    if (locModelViewInverse >= 0) {
                        mvInverseTmp.set(irisViewPoseMat).invertAffine();
                        mvInverseTmp.get(mvInverseFloats);
                    }
                    if (locNormalMat >= 0) {
                        normalTmp.set(irisViewPoseMat);
                        normalTmp.get(normalMatFloats);
                        GL20.glUniformMatrix3fv(locNormalMat, false, normalMatFloats);
                    }
                } else if (posChanged) {
                    float tx = irisCamRotMat.m00() * dx + irisCamRotMat.m10() * dy + irisCamRotMat.m20() * dz;
                    float ty = irisCamRotMat.m01() * dx + irisCamRotMat.m11() * dy + irisCamRotMat.m21() * dz;
                    float tz = irisCamRotMat.m02() * dx + irisCamRotMat.m12() * dy + irisCamRotMat.m22() * dz;
                    mvFloats[12] = tx;
                    mvFloats[13] = ty;
                    mvFloats[14] = tz;
                    if (locModelViewInverse >= 0) {
                        mvInverseFloats[12] = -(mvInverseFloats[0] * tx + mvInverseFloats[4] * ty + mvInverseFloats[8] * tz);
                        mvInverseFloats[13] = -(mvInverseFloats[1] * tx + mvInverseFloats[5] * ty + mvInverseFloats[9] * tz);
                        mvInverseFloats[14] = -(mvInverseFloats[2] * tx + mvInverseFloats[6] * ty + mvInverseFloats[10] * tz);
                    }
                }

                if (locModelView >= 0 && (rotChanged || posChanged)) {
                    GL20.glUniformMatrix4fv(locModelView, false, mvFloats);
                }

                if (locModelViewInverse >= 0 && (rotChanged || posChanged)) {
                    GL20.glUniformMatrix4fv(locModelViewInverse, false, mvInverseFloats);
                }

                if (perVertexLight) {
                    companion.bindLightmapForInstance(instanceLightmapSlot[i]);
                } else if (uv2Loc != -1) {
                    int uvBase = i * 2;
                    int blockUInt = Math.max(0, Math.min(240, Math.round(parent.instanceLightUV[uvBase])));
                    int skyVInt   = Math.max(0, Math.min(240, Math.round(parent.instanceLightUV[uvBase + 1])));
                    if (blockUInt != lastBlockU || skyVInt != lastSkyV) {
                        GL30.glVertexAttribI2i(uv2Loc, blockUInt, skyVInt);
                        lastBlockU = blockUInt;
                        lastSkyV = skyVInt;
                    }
                }

                if (parent.vanillaHelper.uFadeAlpha != null) {
                    parent.vanillaHelper.uFadeAlpha.set(parent.instanceBuffer.get(base + parent.instanceFadeFloatOffset));
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
