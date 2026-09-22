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
    /** Scratch: composed shadow model-view для записи в глобальный shadow-батч. */
    private final Matrix4f tmpShadowMv = new Matrix4f();
    /** Scratch: мировая трансформация инстанса = inv(shadowMV) · composed. */
    private final Matrix4f tmpShadowWorld = new Matrix4f();
    private final Vector3f tmpShadowPos = new Vector3f();
    private final Quaternionf tmpShadowRot = new Quaternionf();
    /** Кэш shadow model-view + инверсии (константа в течение shadow-прохода). */
    private final Matrix4f shadowMvCache = new Matrix4f();
    private final Matrix4f shadowInvMvCache = new Matrix4f();
    private boolean shadowInvValid = false;
    /** Scratch: R_cam (view-ротация) для конверсии мировых записей → view-space. */
    private final Matrix4f irisCamRotMat = new Matrix4f();
    /** Scratch: view-space поза инстанса = R_cam · T(world-cam) · R_world. */
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
            /*// rotation(camera.rotation()) = R_cam⁻¹ БЕЗ доп. invert (см.
            // FrameViewState.capture и fillInstanceCornerLight).
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
     * Батчевый shadow-путь: вместо немедленного {@code drawCompanion} записывает
     * инстанс в глобальный shadow-батч ({@link IrisShadowBatchCollector}) —
     * флаш один на всю shadow BE-фазу из Iris-миксина, per-instance дроуки через
     * pack-программу SHADOW_* (см. flushGlobalShadowBatch).
     * <p>
     * Iris передаёт BER'у PoseStack {@code shadowModelView · T(bePos - camPos)}
     * (ShadowRenderer.renderBlockEntities переводит стек на bePos-cam поверх
     * createShadowModelView). Запись — точная декомпозиция T(pos)·R(rot) этой
     * позы: getTranslation/getNormalizedRotation от аффинной матрицы с
     * ортонормированным 3x3 обратимы без потерь, поэтому флаш
     * recomposition'ом восстанавливает исходную позу бит-в-бит (до float-шума).
     */
    boolean tryRecordShadowInstance(PoseStack poseStack, int packedLight,
                                    BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        if (!IrisShadowBatchCollector.isBatchingEnabled()) {
            return false;
        }
        Matrix4f currentMv = RenderSystem.getModelViewMatrix();
        // Stash — КАЖДЫЙ кадр (флаш сбрасывает stashValid; условный вызов убивал
        // батчинг со второго кадра — «no shadow matrices stashed», debug.log 0913).
        IrisShadowBatchCollector.stashShadowMatrices(RenderSystem.getProjectionMatrix());
        if (!shadowInvValid || !shadowMvCache.equals(currentMv)) {
            shadowMvCache.set(currentMv);
            shadowInvMvCache.set(shadowMvCache).invertAffine();
            shadowInvValid = true;
        }
        tmpShadowMv.set(currentMv).mul(poseStack.last().pose());
        tmpShadowWorld.set(shadowInvMvCache).mul(tmpShadowMv);
        tmpShadowWorld.getTranslation(tmpShadowPos);
        tmpShadowWorld.getNormalizedRotation(tmpShadowRot);
        // Свет в shadow не нужен (глубина/shadowcolor) — вместо дорогого
        // 8-corner сэмпла пишем нули (sampleCornersForSingleDraw здесь был
        // ~2% кадра на ферме).
        java.util.Arrays.fill(parent.tmpCornerUV, 0.0f);
        IrisShadowBatchCollector.record(parent, tmpShadowPos, tmpShadowRot, parent.objBbox, parent.tmpCornerUV);
        return true;
    }

    boolean drawSingleWithIrisExtended(PoseStack poseStack, int packedLight,
                                       BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        // Shadow pass: только через АКТИВНЫЙ per-BE батч (см. SingleMeshVboRenderer
        // .renderWithIrisExtended и IrisRenderBatch.begin). Standalone-путь в
        // shadow запрещён. Без батча — false, вызывающий addInstance/renderSingle
        // уйдёт в putBulkData через bufferSource (SHADOW_BLOCK на endBatch).
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
            // R_cam живёт в RenderSystem.getModelViewMatrix() на ОБЕИХ версиях (см. фикс в
            // InstancedStaticPartRenderer.addInstance) — композит обязателен, иначе модели летают.
            Matrix4f fullModelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
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

            Matrix4f fullModelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
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
     * Истинный инстансный флаш под Iris: наш {@code ExtendedShader}
     * (hbm_m:iris/block_lit_instanced_iris — наш GLSL, юниформы iris_*) +
     * parent VAO (instance-атрибуты 4..11, divisors уже в VAO-state) +
     * ОДИН {@code glDrawElementsInstanced} на part-renderer.
     * <p>
     * Порядок обязателен: pack-шейдер применяется ПЕРВЫМ (его apply() биндит
     * правильный gbuffer-FB) → {@link IrisInstancedShaders#prepare(boolean)}
     * снапшотит аттачменты живого FBO → наш шейдер рисует в те же текстуры.
     * При недоступности нашего шейдера — прежний companion per-instance путь.
     */
    void flushBatchIris(Matrix4f projectionMatrix) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return;
        }
        if (parent.instanceCount == 0 || parent.instanceBuffer == null) {
            return;
        }

        // Pack-шейдер биндит актуальный gbuffer-FB; программу подменяем на нашу.
        ShaderInstance packShader = IrisExtendedShaderAccess.getBlockShader(false);
        if (packShader == null) {
            return;
        }

        // ── Tier 1: GPU Compute Bake (main) ─────────────────────────────
        // ОДИН glDrawElements на part-renderer РОДНОЙ gbuffers-программой пака —
        // пак сам кодирует свой gbuffer. Главное следствие: нераспознанные схемы
        // (BSL: per-instance companion = 1618 дкоуков) получают те же ~27 дкоуков,
        // что и распознанные (Photon). Записи мировые (FrameViewState), свет —
        // трилинейный из 8-corner полей записи. Провал → прежние пути ниже.
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
                                viewRot, FrameViewState.camX(), FrameViewState.camY(),
                                FrameViewState.camZ());
                    }
                }
                if (baked) {
                    return;
                }
            }
        }

        // РЕГРЕССИЯ-СТОП (чёрная база под паками): deferred-паки пишут gbuffer
        // СВОИМИ программами в собственном формате — например, схема "packed"
        // пакует albedo+flat normal+light levels через pack_unorm_2x8 в один
        // таргет (RENDERTARGETS: 1). Однотаргетный FSH с распакованным
        // albedo*lightmap композит декодирует в мусор: геометрия верна, цвет
        // чёрный. Поэтому без распознанного энкодера (IrisInstancedEncoders)
        // инстансный ExtendedShader не используется: рисуем через
        // pack-программу BLOCK_ENTITY (companion per-instance — тот же путь,
        // что у анимированных частей, корректен под любым паком).
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
            // Наш инстансный путь недоступен (рефлексия/версия Iris) — прежний путь.
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

        // РЕГРЕССИЯ-ФИКС: closePersistentIfActive перед флашем биндит main-RT
        // (ExtendedShader.clear -> mainRenderTarget.bindWrite) — без повторного
        // pack-apply машины писались в main-RT и стирались финальным проходом
        // («чанки внутри машин», полупрозрачность). tryApply возвращает
        // актуальный gbuffer-FB; IrisPhaseGuard — фазу BLOCK_ENTITIES.
        try (IrisPhaseGuard phaseGuard = IrisPhaseGuard.pushBlockEntities()) {
            if (!IrisShaderApply.tryApply(packShader)) {
                return;
            }
            GL30.glBindVertexArray(parent.vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, parent.instanceVboId);
            // Span-дифф: только изменившиеся окна записей (мировые координаты —
            // статичная сцена даёт ноль аплоада).
            parent.uploadInstanceStreamToBoundVbo();
            parent.vanillaHelper.enableVertexAttribsForDraw();

            var mc = Minecraft.getInstance();
            if (mc.gameRenderer != null) {
                //? if < 1.21.1 {
                mc.gameRenderer.lightTexture().updateLightTexture(mc.getFrameTime());
                //?} else {
                /*mc.gameRenderer.lightTexture().updateLightTexture(mc.getTimer().getGameTimeDeltaPartialTick(true));
                *///?}
            }

            // iris_-юниформы + подмена программы (useIrisProgram); FB пака остаётся.
            parent.vanillaHelper.useIrisProgram(
                    ours, parent.vanillaHelper.stripViewRotationForInstanced(projectionMatrix),
                    FrameViewState.viewMatrix());
            SingleMeshVboRenderer.primeIrisInstancedSamplerMap(ours, Minecraft.getInstance());

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            // Divisors 4..11 = 1 уже в VAO-state (заданы при создании) — per-draw
            // не трогаются, ванильным VAO утечка не грозит.
            InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, parent.indexCount,
                    GL11.GL_UNSIGNED_INT, 0, parent.instanceCount);
            NucleusDebug.recordDraw(1, parent.instanceCount, "Iris instanced");

            // НЕ ours.clear() (ребиндит main-RT) и НЕ glUseProgram(0):
            // ExtendedShader.lastApplied продолжает считать pack-программу активной
            // и скипает glUseProgram при следующем tryApply — юниформы пака летели
            // бы в программу 0 (GL_INVALID_OPERATION spam, чёрная база). Возвращаем
            // pack-программу явно — трекинг Iris остаётся консистентным.
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
     * Прежний companion per-instance путь — fallback, когда наш ExtendedShader
     * недоступен (рефлексия не сошлась / старая версия Iris/Oculus).
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

            // Записи мировые (FrameViewState): Iris-шейдер ждёт view-space позу
            // инстанса → конверсия R_cam·T(world-cam)·R_world на каждый инстанс.
            irisCamRotMat.set(FrameViewState.inverseViewRotation()).transpose();
            final float camX = FrameViewState.camX();
            final float camY = FrameViewState.camY();
            final float camZ = FrameViewState.camZ();

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
