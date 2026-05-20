package com.hbm_m.client.render;

import java.nio.Buffer;

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
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Vanilla (non-Iris) instanced batch renderer: handles
 * {@code flushBatchVanilla}, single-instance vanilla draws,
 * uniform cache management, and brightness calculations.
 * <p>
 * Extracted from {@link InstancedStaticPartRenderer} to reduce
 * class complexity. Holds a reference to the parent renderer
 * for access to shared state (instance buffer, VAO/VBO ids, etc.).
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
final class VanillaInstancedBatchRenderer {

    private final InstancedStaticPartRenderer parent;
    private static volatile boolean diagLogged;

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
    private Uniform uSampler0;
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
        this.uSampler0 = shader.getUniform("Sampler0");
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
        if (uSampler0 != null) uSampler0.set(0);
        if (uFadeAlpha != null) uFadeAlpha.set(SingleMeshVboRenderer.getFadeAlpha());
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
        this.uSampler0 = null;
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
        parent.instanceBuffer.clear();
        Matrix4f mat = poseStack.last().pose();
        mat.getTranslation(parent.posTmp);
        mat.getNormalizedRotation(parent.rotTmp);

        BlockPos blockPosForSample = (blockEntity != null) ? blockEntity.getBlockPos() : BlockPos.ZERO;
        if (LightSampleCache.BASE_POSE_SET.get()) {
            parent.tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(mat);
        } else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            parent.tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
            parent.tmpLocalPose.set(parent.tmpInvViewRot).mul(mat);
            parent.tmpLocalPose.m30(parent.tmpLocalPose.m30() - (float) (blockPosForSample.getX() - cam.x));
            parent.tmpLocalPose.m31(parent.tmpLocalPose.m31() - (float) (blockPosForSample.getY() - cam.y));
            parent.tmpLocalPose.m32(parent.tmpLocalPose.m32() - (float) (blockPosForSample.getZ() - cam.z));
        }
        long partHash = System.identityHashCode(parent);

        if (parent.useSlicedLight) {
            LightSampleCache.getOrSample16(blockEntity, partHash, parent.objBbox, blockPosForSample,
                                           parent.tmpLocalPose, packedLight, parent.tmpCornerUV);
        } else {
            LightSampleCache.getOrSample8(blockEntity, partHash, parent.objBbox, blockPosForSample,
                                          parent.tmpLocalPose, packedLight, parent.tmpCornerUV);
        }

        parent.memPutInstanceRecordAtBaseFloat(0);
        ((Buffer) parent.instanceBuffer).position(parent.instanceDataSize);
        parent.instanceBuffer.flip();
    }

    // ── Vanilla renderSingle ───────────────────────────────────────────

    void renderSingleVanilla(PoseStack poseStack, int packedLight, BlockPos blockPos,
                             @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        ShaderInstance shader = parent.useSlicedLight ? ModShaders.getBlockLitInstancedSlicedShader()
                                                     : ModShaders.getBlockLitInstancedShader();
        if (shader == null) {
            if (parent.quadsForIris != null && !parent.quadsForIris.isEmpty() && bufferSource != null) {
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                PoseStack.Pose pose = poseStack.last();
                for (BakedQuad quad : parent.quadsForIris) {
                    //? if forge {
                    consumer.putBulkData(pose, quad, 1f, 1f, 1f, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                    //?} else {
                    /*consumer.putBulkData(pose, quad, 1f, 1f, 1f, packedLight, OverlayTexture.NO_OVERLAY);
                    *///?}
                }
            }
            return;
        }

        try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
            RenderSystem.setShader(() -> shader);
            uploadSingleInstance(poseStack, packedLight, blockEntity);

            applyCommonUniforms(shader, RenderSystem.getProjectionMatrix(), new Matrix4f(RenderSystem.getModelViewMatrix()));
            SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
            shader.apply();

            float fade = parent.instanceBuffer.get(parent.instanceFadeFloatOffset);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            GL11.glDisable(GL11.GL_CULL_FACE);
            if (fade < 0.99f) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            }

            GL30.glBindVertexArray(parent.vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, parent.instanceVboId);
            parent.uploadInstanceStreamToBoundVbo();
            for (int i = InstancedStaticPartRenderer.INSTANCE_ATTRIB_FIRST; i <= parent.instanceAttribLast; i++)
                GL20.glEnableVertexAttribArray(i);

            InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, parent.indexCount, GL11.GL_UNSIGNED_INT, 0, 1);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("VanillaInstancedBatchRenderer.renderSingleVanilla failed", e);
        } finally {
            parent.instanceBuffer.clear();
        }
    }

    // ── Batch flush ────────────────────────────────────────────────────

    void flushBatchVanilla(Matrix4f projectionMatrix) {
        boolean alreadyFlipped = false;

        MdiBatchCoordinator coord = MdiBatchCoordinator.active();
        if (coord != null
                && !parent.storesPerInstancePartBone
                && !parent.useSlicedLight
                && parent.atlasVertexBytesRetained != null
                && parent.atlasIndicesRetained != null
                && parent.atlasIndexCountRetained > 0
                && !ShaderCompatibilityDetector.isExternalShaderActive()) {
            parent.instanceBuffer.flip();
            alreadyFlipped = true;
            boolean accepted = coord.submit(parent, parent.indexCount, parent.instanceCount,
                    parent.instanceDataSize, parent.instanceBuffer, parent.instanceCullIndices, parent.instanceOcclusionKeys,
                    parent.atlasVertexBytesRetained, parent.atlasIndicesRetained, parent.atlasIndexCountRetained);
            if (accepted) {
                return;
            }
        }

        ShaderInstance shader = parent.useSlicedLight ? ModShaders.getBlockLitInstancedSlicedShader()
                                                     : ModShaders.getBlockLitInstancedShader();
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

        try {
            GL30.glBindVertexArray(parent.vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, parent.instanceVboId);
            parent.uploadInstanceStreamToBoundVbo();

            for (int i = InstancedStaticPartRenderer.INSTANCE_ATTRIB_FIRST; i <= parent.instanceAttribLast; i++) {
                GL20.glEnableVertexAttribArray(i);
            }

            RenderSystem.setShader(() -> shader);
            applyCommonUniforms(shader, projectionMatrix, new Matrix4f(RenderSystem.getModelViewMatrix()));

            SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
            shader.apply();

            // Force-rebind textures via raw GL — bypass all caching.
            int blockAtlasId = net.minecraft.client.Minecraft.getInstance()
                    .getTextureManager()
                    .getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                    .getId();

            // Diagnostic: log actual GL state ONCE to find the texture bug
            if (!diagLogged) {
                diagLogged = true;
                int activeProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
                org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
                int boundTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE1);
                int boundTex1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                int sampler0Loc = GL20.glGetUniformLocation(activeProgram, "Sampler0");
                int sampler2Loc = GL20.glGetUniformLocation(activeProgram, "Sampler2");
                int[] sampler0Val = {-999};
                int[] sampler2Val = {-999};
                if (sampler0Loc >= 0) GL20.glGetUniformiv(activeProgram, sampler0Loc, sampler0Val);
                if (sampler2Loc >= 0) GL20.glGetUniformiv(activeProgram, sampler2Loc, sampler2Val);
                int lightmapId = RenderSystem.getShaderTexture(2);
                MainRegistry.LOGGER.warn(
                    "[HBM-M DIAG] flushBatchVanilla: program={} (shader.getId={}), "
                    + "blockAtlasId={}, lightmapId={}, "
                    + "GL_TEXTURE0.bound={}, GL_TEXTURE1.bound={}, "
                    + "Sampler0.loc={} val={}, Sampler2.loc={} val={}, "
                    + "instanceCount={}",
                    activeProgram, shader.getId(),
                    blockAtlasId, lightmapId,
                    boundTex0, boundTex1,
                    sampler0Loc, sampler0Val[0], sampler2Loc, sampler2Val[0],
                    parent.instanceCount);
            }

            org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, blockAtlasId);
            int sampler0Loc = GL20.glGetUniformLocation(shader.getId(), "Sampler0");
            if (sampler0Loc >= 0) {
                GL20.glUniform1i(sampler0Loc, 0);
            }

            float minFade = 1f;
            for (int i = 0; i < parent.instanceCount; i++) {
                float fa = parent.instanceBuffer.get(i * parent.instanceDataSize + parent.instanceFadeFloatOffset);
                if (fa < minFade) minFade = fa;
            }
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            if (minFade < 0.99f) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            }

            InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, parent.indexCount, GL11.GL_UNSIGNED_INT, 0, parent.instanceCount);

            if (minFade < 0.99f) {
                RenderSystem.disableBlend();
            }

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during instanced flush (vanilla)", e);
        } finally {
            GL30.glBindVertexArray(previousVao);
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
            RenderSystem.setShaderTexture(0, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        }
    }
}
