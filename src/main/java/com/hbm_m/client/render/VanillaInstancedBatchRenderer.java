package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.Buffer;
import java.nio.FloatBuffer;

import com.hbm_m.client.render.shader.ModShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Vanilla (non-Iris) instanced batch renderer: handles
 * {@code flushBatchVanilla}, single-instance vanilla draws,
 * uniform cache management, and brightness calculations.
 * <p>
 * Extracted from {@link InstancedStaticPartRenderer} to reduce
 * class complexity. Holds a reference to the parent renderer
 * for access to shared state (instance buffer, VAO/VBO ids, etc.).
 */

@OnlyIn(Dist.CLIENT)
final class VanillaInstancedBatchRenderer {

    private final InstancedStaticPartRenderer parent;

    /** Scratch MV composite for uploadSingleInstance (rendering is single-threaded). */
    private final Matrix4f singleMvScratch = new Matrix4f();

    private ShaderInstance cachedShader = null;
    private int cachedShaderProgramId = -1;
    /**
     * Pipeline generation this uniform cache was built against.
     * Program IDs alone are unsafe as a cache key because GL drivers recycle
     * deleted IDs on pipeline rebuild; pairing with the generation counter
     * guarantees we re-resolve {@link Uniform} handles whenever the underlying
     * GL program was torn down and re-linked.
     */
    private long cachedPipelineGeneration = -1L;
    private Uniform uProjMat;
    private Uniform uModelView;
    private Uniform uFogStart;
    private Uniform uFogEnd;
    private Uniform uFogColor;
    Uniform uBrightness;
    Uniform uFadeAlpha;

    VanillaInstancedBatchRenderer(InstancedStaticPartRenderer parent) {
        this.parent = parent;
    }

    // ── Uniform cache ──────────────────────────────────────────────────

    void updateUniformCache(ShaderInstance shader) {
        int programId = (shader != null) ? shader.getId() : -1;
        long currentGen = com.hbm_m.client.render.shader.IrisExtendedShaderAccess.getPipelineGeneration();
        if (this.cachedShaderProgramId == programId
                && this.cachedShader == shader
                && this.cachedPipelineGeneration == currentGen
                && this.cachedShader != null) return;

        this.cachedShader = shader;
        this.cachedShaderProgramId = programId;
        this.cachedPipelineGeneration = currentGen;
        this.uProjMat = shader.getUniform("ProjMat");
        this.uModelView = shader.getUniform("ModelViewMat");
        this.uFogStart = shader.getUniform("FogStart");
        this.uFogEnd = shader.getUniform("FogEnd");
        this.uFogColor = shader.getUniform("FogColor");
        this.uBrightness = shader.getUniform("Brightness");
        this.uFadeAlpha = shader.getUniform("FadeAlpha");
    }

    void applyCommonUniforms(ShaderInstance shader, Matrix4f projectionMatrix, Matrix4f modelView) {
        updateUniformCache(shader);

        if (uProjMat != null) uProjMat.set(projectionMatrix);
        else if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(projectionMatrix);

        if (uModelView != null) uModelView.set(modelView);
        else if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(modelView);

        if (uFogStart != null) uFogStart.set(RenderSystem.getShaderFogStart());
        if (uFogEnd != null) uFogEnd.set(RenderSystem.getShaderFogEnd());
        if (uFogColor != null) {
            float[] fogColor = RenderSystem.getShaderFogColor();
            uFogColor.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
        }
        if (uFadeAlpha != null) uFadeAlpha.set(SingleMeshVboRenderer.getFadeAlpha());
        // Sampler0/Sampler2: {@link SingleMeshVboRenderer#prepareBlockLitSamplers} + bindBlockLitSamplerTextures.
    }

    /**
     * Iris instanced path: sets the iris_ uniforms, switches the program to ours
     * (the pack shader has already bound the FB) and uploads them. Call instead of
     * apply(): ExtendedShader.apply()/clear() would bind their own (dead) FB clones.
     */
    void useIrisProgram(ShaderInstance ours, Matrix4f proj, Matrix4f modelView) {
        updateUniformCache(ours);
        if (uProjMat != null) uProjMat.set(proj);
        if (uModelView != null) uModelView.set(modelView);
        if (uFogStart != null) uFogStart.set(RenderSystem.getShaderFogStart());
        if (uFogEnd != null) uFogEnd.set(RenderSystem.getShaderFogEnd());
        if (uFogColor != null) {
            float[] c = RenderSystem.getShaderFogColor();
            uFogColor.set(c[0], c[1], c[2], c[3]);
        }
        GL20.glUseProgram(ours.getId());
        if (uProjMat != null) uProjMat.upload();
        if (uModelView != null) uModelView.upload();
        if (uFogStart != null) uFogStart.upload();
        if (uFogEnd != null) uFogEnd.upload();
        if (uFogColor != null) uFogColor.upload();
    }

    // ── 1.21.1 view-rotation stripping ────────────────────────────────

    //? if >= 1.21.1 {
    /*// On 1.21.1 Mojang moves the camera view rotation (R_cam) into the projection matrix
    // of RenderLevelStageEvent: event.getProjectionMatrix() = P*R_cam. Meanwhile the BER
    // poseStack, from which addInstance extracts InstPos/InstRot, also carries R_cam
    // (mat = R_cam * T(blockPos - cameraPos) * perBELocal - see the comment in
    // SingleMeshVboRenderer.render and fillInstanceCornerLight, where R_cam is inverted).
    // The instanced VS builds modelView = T(InstPos)*R(InstRot) == R_cam*T(d)*localRot,
    // and if ProjMat = P*R_cam the result is P*R_cam*R_cam*T(d)*localRot = P*R_cam^2*...
    // -> double rotation. Symptom: the model "flies" across the screen, correct only at
    // yaw=180/0 (where R_cam^2 ~ I). Fix: strip R_cam from the event projection before upload.
    //
    // IMPORTANT: strip ONLY the event projection (from flushBatchVanilla/flushBatchIris).
    // RenderSystem.getProjectionMatrix() on 1.21.1 does NOT contain R_cam (pure P there) -
    // it is used by renderSingleVanilla and SingleMeshVboRenderer.render (non-instanced BER),
    // where R_cam is applied once via the poseStack ModelViewMat. Stripping there breaks it.
    private final org.joml.Matrix4f strippedProjection = new org.joml.Matrix4f();
    private final org.joml.Matrix4f invViewRotTmp = new org.joml.Matrix4f();
    *///?}

    Matrix4f stripViewRotationForInstanced(Matrix4f projection) {
        // Per vanilla GameRenderer.renderLevel on 1.21.1, the event projection is P*bob
        // WITHOUT R_cam (R_cam is passed separately as the frustumMatrix argument and lives
        // in modelViewStack). Multiplying by P * R_cam^-1 corrupted the projection ->
        // instanced models flew across the screen.
        return projection;
    }

    /** Re-enable mesh + instance attribs (chunk/MDI passes may disable UV0). */
    void enableVertexAttribsForDraw() {
        for (int i = 0; i <= parent.instanceAttribLast; i++) {
            GL20.glEnableVertexAttribArray(i);
        }
    }

    /**
     * Invalidates the cached shader and Iris uniform locations.
     * Called when the Iris pipeline rebuilds.
     */
    void invalidateShaderCache() {
        this.cachedShader = null;
        this.cachedShaderProgramId = -1;
        this.cachedPipelineGeneration = -1L;
        this.uProjMat = null;
        this.uModelView = null;
        this.uFogStart = null;
        this.uFogEnd = null;
        this.uFogColor = null;
        this.uBrightness = null;
        this.uFadeAlpha = null;
    }

    Uniform getModelViewUniform() {
        return uModelView;
    }

    // ── Brightness ─────────────────────────────────────────────────────

    float brightnessFromUV(float blockU, float skyV, float cachedSkyDarken) {
        float blockLight = blockU / 16.0f;
        float skyLight   = skyV   / 16.0f;

        float skyDarken;
        if (cachedSkyDarken >= 0f && cachedSkyDarken <= 1f) {
            skyDarken = cachedSkyDarken;
        } else {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return Math.max(0.05f, Math.max(blockLight, skyLight) / 15.0f);
            }
            skyDarken = level.getSkyDarken(1.0f);
        }

        float skyBrightness = 0.05f + (skyDarken * 0.95f);
        float effectiveSkyLight = skyLight * skyBrightness;
        float maxLight = Math.max(blockLight, effectiveSkyLight);
        return 0.05f + (maxLight / 15.0f) * 0.95f;
    }

    float calculateBrightness(int packedLight) {
        return calculateBrightness(packedLight, Float.NaN);
    }

    float calculateBrightness(int packedLight, float cachedSkyDarken) {
        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);

        float skyDarken;
        if (cachedSkyDarken >= 0f && cachedSkyDarken <= 1f) {
            skyDarken = cachedSkyDarken;
        } else {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return Math.max(0.05f, Math.max(blockLight, skyLight) / 15.0f);
            }
            skyDarken = level.getSkyDarken(1.0f);
        }

        float skyBrightness = 0.05f + (skyDarken * 0.95f);
        float effectiveSkyLight = skyLight * skyBrightness;
        float maxLight = Math.max(blockLight, effectiveSkyLight);
        return 0.05f + (maxLight / 15.0f) * 0.95f;
    }

    // ── Single instance upload ─────────────────────────────────────────

    void uploadSingleInstance(PoseStack poseStack, int packedLight,
                              @Nullable BlockEntity blockEntity) {
        // renderSingle writes the record into the shared instanceBuffer - the clean-reuse
        // sync with the coordinator is lost (safety for accidental double use of a renderer).
        parent.noteMdiDispatchLost();
        parent.mdiRecordWriteHappened = true;
        parent.invalidateRoster();
        parent.instanceBuffer.clear();
        Matrix4f mat = singleMvScratch.set(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
        // Record in world coordinates (see InstancedStaticPartRenderer.convertToWorldRecord):
        // the camera is applied in the vsh via ModelViewMat = FrameViewState.viewMatrix().
        parent.convertToWorldRecord(mat);

        BlockPos blockPosForSample = (blockEntity != null) ? blockEntity.getBlockPos() : BlockPos.ZERO;
        if (LightSampleCache.BASE_POSE_SET.get()) {
            parent.tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(poseStack.last().pose());
        } else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            //? if < 1.21.1 {
            parent.tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
             //?} else {
            /*// rotation(camera.rotation()) = R_cam^-1 with no extra invert (see
            // FrameViewState.capture).
            parent.tmpInvViewRot.identity().rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            *///?}
            parent.tmpLocalPose.set(parent.tmpInvViewRot).mul(poseStack.last().pose());
            parent.tmpLocalPose.m30(parent.tmpLocalPose.m30() - (float) (blockPosForSample.getX() - cam.x));
            parent.tmpLocalPose.m31(parent.tmpLocalPose.m31() - (float) (blockPosForSample.getY() - cam.y));
            parent.tmpLocalPose.m32(parent.tmpLocalPose.m32() - (float) (blockPosForSample.getZ() - cam.z));
        }
        long partHash = System.identityHashCode(parent);

        LightSampleCache.getOrSample8(blockEntity, partHash, parent.objBbox, blockPosForSample,
                parent.tmpLocalPose, packedLight, parent.tmpCornerUV);

        parent.memPutInstanceRecordAtBaseFloat(0);
        ((Buffer) parent.instanceBuffer).position(parent.instanceDataSize);
        parent.instanceBuffer.flip();
    }

    // ── Vanilla renderSingle ───────────────────────────────────────────

    void renderSingleVanilla(PoseStack poseStack, int packedLight, BlockPos blockPos,
                             @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        ShaderInstance shader = ModShaders.getBlockLitInstancedShader();
        if (shader == null) {
            if (parent.quadsForIris != null && !parent.quadsForIris.isEmpty() && bufferSource != null) {
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                PoseStack.Pose pose = poseStack.last();
                for (BakedQuad quad : parent.quadsForIris) {
                    RenderHooks.putBulkData(consumer, pose, quad, 1f, 1f, 1f, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

        try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
            uploadSingleInstance(poseStack, packedLight, blockEntity);

            GL30.glBindVertexArray(parent.vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, parent.instanceVboId);
            parent.uploadInstanceStreamToBoundVbo();
            enableVertexAttribsForDraw();

            RenderSystem.setShader(() -> shader);
            // renderSingle is called from a BER (DoorRenderer) - the projection from
            // RenderSystem.getProjectionMatrix() does NOT contain R_cam on 1.21.1 (R_cam
            // is only in event.getProjectionMatrix()). InstPos/InstRot are world-space now,
            // the camera lives in ModelViewMat (R_cam * T(-cam)). Identity on 1.20.1.
            applyCommonUniforms(shader, RenderSystem.getProjectionMatrix(), FrameViewState.viewMatrix());
            SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
            shader.apply();
            SingleMeshVboRenderer.bindBlockLitSamplerTextures(shader);

            float fade = parent.instanceBuffer.get(parent.instanceFadeFloatOffset);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            // Managed call: a raw GL11.glDisable(GL_CULL_FACE) did not update the
            // GlStateManager cache, and RenderStateGuard.close() restored cull as a
            // no-op (the cache believed it was still enabled).
            RenderSystem.disableCull();
            if (fade < 0.99f) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                // A fading single instance (doors outside the batch) is drawn right in the
                // BER phase - before the MDI bases; without depth writing it would not
                // depth-reject them. RenderStateGuard will restore the mask.
                RenderSystem.depthMask(false);
            }

            InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, parent.indexCount, GL11.GL_UNSIGNED_INT, 0, 1);
            NucleusDebug.recordDraw(1, 1, "Instanced (single)");
        } catch (Exception e) {
            MainRegistry.LOGGER.error("VanillaInstancedBatchRenderer.renderSingleVanilla failed", e);
        } finally {
            parent.instanceBuffer.clear();
        }
    }

    // ── Batch flush ────────────────────────────────────────────────────

    // REGRESSION GUARD: draw order is VAO -> shader -> identity ModelView -> prepareSamplers -> apply -> bind -> draw.
    // Do NOT change the order; do NOT draw without bindBlockLitSamplerTextures after apply (white OBJs).
    void flushBatchVanilla(Matrix4f projectionMatrix) {
        // 1.21.1: the projection from event.getProjectionMatrix() carries R_cam; the instanced
        // VS builds modelView from InstPos/InstRot (also with R_cam) -> double rotation. Strip it.
        Matrix4f proj = stripViewRotationForInstanced(projectionMatrix);
        boolean alreadyFlipped = false;

        MdiBatchCoordinator coord = MdiBatchCoordinator.active();
        if (coord != null
                && parent.atlasVertexBytesRetained != null
                && parent.atlasIndicesRetained != null
                && parent.atlasIndexCountRetained > 0
                && !ShaderCompatibilityDetector.isExternalShaderActive()) {
            parent.instanceBuffer.flip();
            alreadyFlipped = true;
            // Clean frame: not a single write into the buffer (skip-write) and the buffer
            // is in sync with the snapshot - the coordinator reuses last frame's record as is.
            if (parent.canSubmitMdiClean()
                    && coord.submitClean(parent, parent.indexCount, parent.instanceCount)) {
                return;
            }
            boolean accepted = coord.submit(parent, parent.indexCount, parent.instanceCount,
                    parent.instanceDataSize, parent.instanceBuffer, parent.instanceCullIndices, parent.instanceOcclusionKeys,
                    parent.atlasVertexBytesRetained, parent.atlasIndicesRetained, parent.atlasIndexCountRetained);
            if (accepted) {
                parent.noteMdiDispatched();
                return;
            }
            parent.noteMdiDispatchLost();
        }

        ShaderInstance shader = ModShaders.getBlockLitInstancedShader();
        if (shader == null) {
            if (!InstancedStaticPartRenderer.warnedInstancedShaderNullFlush) {
                InstancedStaticPartRenderer.warnedInstancedShaderNullFlush = true;
                MainRegistry.LOGGER.warn(
                        "InstancedStaticPartRenderer: instanced shader is null, discarding flush of {} instances (Fabric: ClientSetup.registerFabricShaders)",
                        parent.instanceCount);
            }
            return;
        }

        if (!alreadyFlipped) {
            parent.instanceBuffer.flip();
        }

        // RenderStateGuard snapshots and symmetrically restores VAO,
        // ARRAY_BUFFER, cull, depth test/mask/func and blend+blendFunc -
        // exactly the set that used to be snapshotted manually below.
        try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
            // Variant G on the direct path (renderers not accepted by the atlas):
            // opaque instances are drawn NOW - before the MDI dispatch; fading ones
            // are copied into the snapshot and picked up by flushFadingVanilla AFTER
            // the multi-draw. Otherwise fading parts write depth before the opaque
            // base from MDI and depth-reject it.
            int stride = parent.instanceDataSize;
            int opaque = InstancedStaticPartRenderer.partitionInstancesOpaqueFirst(
                    parent.instanceBuffer, parent.instanceCount, stride, parent.instanceFadeFloatOffset,
                    parent.instanceOcclusionKeys);
            if (opaque < parent.instanceCount) {
                // The partition reordered records (fading present) - the buffer content
                // diverged from the coordinator's snapshot; the clean path is unavailable.
                parent.noteMdiDispatchLost();
            }
            if (opaque > 0) {
                drawInstanceRange(shader, proj, parent.instanceBuffer, 0, opaque);
            }
            parent.deferFading(opaque, parent.instanceCount - opaque);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during instanced flush (vanilla)", e);
        } finally {
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            RenderSystem.setShaderTexture(0, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        }
    }

    /**
     * Draws {@code count} instances starting at record {@code firstRecord} of buffer
     * {@code data}. The block_lit contract is unchanged: VAO -> shader -> ModelViewMat=V ->
     * prepareSamplers -> apply -> bind -> draw (do NOT change - white OBJs).
     * InstPos/InstRot are world-space: ModelViewMat = R_cam * T(-cam) (FrameViewState).
     * Blend is unnecessary for the opaque range: all fade ~ 1 by partition construction.
     */
    private void drawInstanceRange(ShaderInstance shader, Matrix4f proj, FloatBuffer data,
                                   int firstRecord, int count) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();

        GL30.glBindVertexArray(parent.vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, parent.instanceVboId);
        int stride = parent.instanceDataSize;
        // Span upload: only changed ranges (orphans are forbidden - skipped spans
        // must keep the old VBO content).
        parent.uploadToInstanceVboSpanned(data, firstRecord * stride, 0, count * stride);
        enableVertexAttribsForDraw();

        RenderSystem.setShader(() -> shader);
        // ModelViewMat = V: the instanced VS multiplies world-space InstPos/InstRot by it.
        applyCommonUniforms(shader, proj, FrameViewState.viewMatrix());
        SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
        shader.apply();
        SingleMeshVboRenderer.bindBlockLitSamplerTextures(shader);

        InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, parent.indexCount,
                GL11.GL_UNSIGNED_INT, 0, count);
        NucleusDebug.recordDraw(1, count, "Instanced (direct)");
    }

    /**
     * Phase 2 of the direct path: fading instances deferred by {@link #flushBatchVanilla}.
     * Called AFTER the MDI dispatch (InstancedRenderFrame.flushAllInstancedFading).
     * <p>
     * depthMask(true): depth-write is required for self-overlap within a model
     * (a spike inside an arm housing must not "poke through" while blending).
     * Mutual depth-reject of machines is excluded by sorting: instances within the
     * snapshot are back-to-front (partitionInstancesOpaqueFirst), renderer windows are
     * sorted by the farthest fading instance in {@code MachineSpec.flushFading}.
     */
    void flushFadingVanilla(Matrix4f projectionMatrix, int count) {
        ShaderInstance shader = ModShaders.getBlockLitInstancedShader();
        if (shader == null || parent.fadingSnapshot == null) {
            return;
        }
        Matrix4f proj = stripViewRotationForInstanced(projectionMatrix);
        try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            GL30.glBindVertexArray(parent.vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, parent.instanceVboId);
            // Span upload of the fading range (region [0, count) - same as in the
            // opaque phase; the span diff will roll the content over correctly).
            parent.uploadToInstanceVboSpanned(parent.fadingSnapshot, 0, 0, count * parent.instanceDataSize);
            enableVertexAttribsForDraw();

            RenderSystem.setShader(() -> shader);
            applyCommonUniforms(shader, proj, FrameViewState.viewMatrix());
            SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
            shader.apply();
            SingleMeshVboRenderer.bindBlockLitSamplerTextures(shader);

            InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, parent.indexCount,
                    GL11.GL_UNSIGNED_INT, 0, count);
            NucleusDebug.recordDraw(1, count, "Instanced (direct)");

            RenderSystem.disableBlend();
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during instanced fading flush (vanilla)", e);
        } finally {
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            RenderSystem.setShaderTexture(0, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        }
    }
}
