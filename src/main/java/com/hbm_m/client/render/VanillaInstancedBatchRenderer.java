package com.hbm_m.client.render;

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

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
final class VanillaInstancedBatchRenderer {

    private final InstancedStaticPartRenderer parent;

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
     * Iris-инстансный путь: сеттит iris_-юниформы, подменяет программу на нашу
     * (pack-шейдер уже забиндил FB) и аплоадит их. Вызывать вместо apply():
     * ExtendedShader.apply()/clear() биндили бы свои (мёртвые) FB-клоны.
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
    /*// На 1.21.1 Mojang переносит camera view rotation (R_cam) в projection matrix
    // RenderLevelStageEvent'а: event.getProjectionMatrix() = P*R_cam. При этом BER
    // poseStack, из которого addInstance извлекает InstPos/InstRot, тоже несёт R_cam
    // (mat = R_cam * T(blockPos - cameraPos) * perBELocal — см. комментарий в
    // SingleMeshVboRenderer.render и fillInstanceCornerLight, где R_cam invert'ится).
    // Instanced VS собирает modelView = T(InstPos)*R(InstRot) ≡ R_cam*T(d)*localRot,
    // и если ProjMat = P*R_cam, итог = P*R_cam*R_cam*T(d)*localRot = P*R_cam²*...
    // → двойная ротация. Симптом: модель "летает" по экрану, корректно только при
    // yaw=180/0 (где R_cam²≈I). Фикс: stripp'им R_cam из event projection перед upload'ом.
    //
    // ВАЖНО: stripp'им ТОЛЬКО event projection (из flushBatchVanilla/flushBatchIris).
    // RenderSystem.getProjectionMatrix() на 1.21.1 НЕ содержит R_cam (там чистая P) —
    // его использует renderSingleVanilla и SingleMeshVboRenderer.render (не-instanced BER),
    // где R_cam применяется один раз через poseStack ModelViewMat. Стриппинг там ломает.
    private final org.joml.Matrix4f strippedProjection = new org.joml.Matrix4f();
    private final org.joml.Matrix4f invViewRotTmp = new org.joml.Matrix4f();
    *///?}

    Matrix4f stripViewRotationForInstanced(Matrix4f projection) {
        // По ванильному GameRenderer.renderLevel 1.21.1 проекция события — это P*bob
        // БЕЗ R_cam (R_cam передаётся отдельным аргументом frustumMatrix и живёт в modelViewStack).
        // Умножение P * R_cam^-1 портило проекцию → instanced-модели летали по экрану.
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
        // renderSingle пишет запись в общий instanceBuffer — clean-reuse синхронизация
        // с координатором теряется (страховка на случай сдвоенного использования рендерера).
        parent.noteMdiDispatchLost();
        parent.mdiRecordWriteHappened = true;
        parent.instanceBuffer.clear();
        Matrix4f mat = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
        // Мировые координаты записи (см. InstancedStaticPartRenderer.convertToWorldRecord):
        // камера применяется в vsh через ModelViewMat = FrameViewState.viewMatrix().
        parent.convertToWorldRecord(mat);

        BlockPos blockPosForSample = (blockEntity != null) ? blockEntity.getBlockPos() : BlockPos.ZERO;
        if (LightSampleCache.BASE_POSE_SET.get()) {
            parent.tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(poseStack.last().pose());
        } else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            //? if < 1.21.1 {
            parent.tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
             //?} else {
            /*// rotation(camera.rotation()) = R_cam⁻¹ БЕЗ доп. invert (см.
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
            // renderSingle вызывается из BER (DoorRenderer) — projection из RenderSystem.getProjectionMatrix()
            // НЕ содержит R_cam на 1.21.1 (R_cam только в event.getProjectionMatrix()). InstPos/InstRot
            // теперь мировые, камера живёт в ModelViewMat (R_cam·T(-cam)). На 1.20.1 тождественно.
            applyCommonUniforms(shader, RenderSystem.getProjectionMatrix(), FrameViewState.viewMatrix());
            SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
            shader.apply();
            SingleMeshVboRenderer.bindBlockLitSamplerTextures(shader);

            float fade = parent.instanceBuffer.get(parent.instanceFadeFloatOffset);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            // Управляемый вызов: сырой GL11.glDisable(GL_CULL_FACE) не обновлял
            // кеш GlStateManager, и RenderStateGuard.close() восстанавливал
            // cull но-опом (кеш считал его всё ещё включённым).
            RenderSystem.disableCull();
            if (fade < 0.99f) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                // Затухающий single-инстанс (двери вне батча) рисуется сразу в
                // BER-фазе — раньше MDI-баз позади; без записи глубины он их
                // не depth-reject'ит. RenderStateGuard восстановит маску.
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

    // РЕГРЕССИЯ-СТОП: порядок draw — VAO → shader → identity ModelView → prepareSamplers → apply → bind → draw.
    // НЕ менять порядок; НЕ рисовать без bindBlockLitSamplerTextures после apply (белые OBJ).
    void flushBatchVanilla(Matrix4f projectionMatrix) {
        // 1.21.1: projection из event.getProjectionMatrix() несёт R_cam; instanced VS
        // собирает modelView из InstPos/InstRot (тоже с R_cam) → двойная ротация. Stripp'им.
        Matrix4f proj = stripViewRotationForInstanced(projectionMatrix);
        boolean alreadyFlipped = false;

        MdiBatchCoordinator coord = MdiBatchCoordinator.active();
        if (coord != null
                && !parent.storesPerInstancePartBone
                && parent.atlasVertexBytesRetained != null
                && parent.atlasIndicesRetained != null
                && parent.atlasIndexCountRetained > 0
                && !ShaderCompatibilityDetector.isExternalShaderActive()) {
            parent.instanceBuffer.flip();
            alreadyFlipped = true;
            // Чистый кадр: ни одной записи в буфер (skip-write) и буфер синхронизирован
            // со снапшотом — координатор переиспользует прошлокадровую запись целиком.
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

        // RenderStateGuard снимает и симметрично восстанавливает VAO,
        // ARRAY_BUFFER, cull, depth test/mask/func и blend+blendFunc —
        // ровно тот набор, который раньше снапшотился вручную ниже.
        try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
            // Вариант G на прямом пути (GPU-bones chain-части в MDI не ходят):
            // opaque-инстансы рисуются СЕЙЧАС — до MDI-диспетча, затухающие
            // копируются в снапшот; их добирает flushFadingVanilla ПОСЛЕ
            // мульти-драва. Иначе затухающая цепочка (Ring/руки сборочного)
            // пишет глубину раньше непрозрачной базы из MDI и depth-reject'ит её.
            int stride = parent.instanceDataSize;
            int opaque = InstancedStaticPartRenderer.partitionInstancesOpaqueFirst(
                    parent.instanceBuffer, parent.instanceCount, stride, parent.instanceFadeFloatOffset);
            if (opaque < parent.instanceCount) {
                // Партиция переставила записи (есть fading) — содержимое буфера
                // разошлось со снапшотом координатора, чистый путь недоступен.
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
     * Рисует {@code count} инстансов начиная с записи {@code firstRecord} буфера
     * {@code data}. Контракт block_lit неизменен: VAO → shader → ModelViewMat=V →
     * prepareSamplers → apply → bind → draw (НЕ менять — белые OBJ).
     * InstPos/InstRot мировые: ModelViewMat = R_cam·T(-cam) (FrameViewState).
     * Для opaque-диапазона blend не нужен: все fade ≈ 1 по построению партиции.
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
        // Span-аплоад: только изменившиеся диапазоны (орфан запрещён — скипнутые
        // span-ы обязаны сохранять старое содержимое VBO).
        parent.uploadToInstanceVboSpanned(data, firstRecord * stride, 0, count * stride);
        enableVertexAttribsForDraw();

        RenderSystem.setShader(() -> shader);
        // ModelViewMat = V: instanced VS домножает мировые InstPos/InstRot на него.
        applyCommonUniforms(shader, proj, FrameViewState.viewMatrix());
        SingleMeshVboRenderer.prepareBlockLitSamplers(shader);
        shader.apply();
        SingleMeshVboRenderer.bindBlockLitSamplerTextures(shader);

        InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, parent.indexCount,
                GL11.GL_UNSIGNED_INT, 0, count);
        NucleusDebug.recordDraw(1, count, "Instanced (direct)");
    }

    /**
     * Фаза 2 прямого пути: затухающие инстансы, отложенные {@link #flushBatchVanilla}.
     * Вызывается ПОСЛЕ MDI-диспетча (InstancedRenderFrame.flushAllInstancedFading).
     * <p>
     * depthMask(true): depth-write обязателен для самоперекрытия внутри модели
     * (шип внутри кожуха руки не должен «вылезать» наружу при блендинге).
     * Взаимный depth-reject машин исключён сортировкой: инстансы внутри снапшота
     * back-to-front (partitionInstancesOpaqueFirst), окна рендереров сортируются
     * по дальнему fading-инстансу в {@code MachineSpec.flushFading}.
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
            // Span-аплоад затухающего диапазона (регион [0, count) — тот же, что
            // у opaque-фазы; span-дифф корректно перевалит содержимое).
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
