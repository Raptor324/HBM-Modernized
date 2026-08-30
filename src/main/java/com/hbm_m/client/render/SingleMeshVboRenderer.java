package com.hbm_m.client.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.client.render.shader.ModShaders;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.platform.GlStateManager;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public abstract class SingleMeshVboRenderer extends AbstractGpuMesh {

    /**
     * Вершина instanced-мешей: pos(12) + normal(12) + uv(8) + int {@code bone_id} (4) = 36 байт.
     * См. {@code block_lit.vsh} (USE_VERTEX_BONE_ID) и UBO костей в {@link InstancedStaticPartRenderer}.
     */
    public static final int MACHINE_PART_VERTEX_STRIDE_BYTES = 36;

    /** Optional companion mesh in Iris-extended {@code NEW_ENTITY} format, lazy-built. */
    @Nullable
    private IrisCompanionMesh irisCompanion;
    private boolean irisCompanionAttempted;

    /**
     * Thread-local fade alpha for distance-based dissolve. Set by BER callers
     * via {@link #setFadeAlpha(float)} before invoking {@link #render}; the
     * value is uploaded to the {@code FadeAlpha} shader uniform and also applied
     * to the Iris putBulkData fallback path via alpha modulation.
     * Defaults to 1.0 (fully opaque). NOTE: callers are responsible for
     * restoring the previous value after their last {@link #render} call —
     * BERs typically render multiple parts under the same fade, so the
     * renderer itself does NOT auto-reset between part renders.
     * See {@code MachinePressRenderer} for the save/restore pattern.
     */
    private static final ThreadLocal<Float> currentFadeAlpha = ThreadLocal.withInitial(() -> 1.0f);
    /**
     * Network-tracked ballistic missiles: draw without depth (terrain occludes at horizon)
     * and with shader fog pushed to effectively disabled.
     */
    private static final ThreadLocal<Boolean> worldMissileOverlayDraw = ThreadLocal.withInitial(() -> false);
    /** Entity-pass missile mesh: bias depth vs terrain written in the solid pass (horizon z-fighting). */
    private static final ThreadLocal<Boolean> entityMissileDepthBias = ThreadLocal.withInitial(() -> false);
    /**
     * Какой веткой последний раз рисовался track-меш (только под флагом
     * entityMissileDepthBias): "vbo", "quads" (фолбэк putBulkData) или null
     * (ничего не рисовалось). Читается EngineHandler'ом для диагностики
     * «чёрного экрана»: расхождение между ожидаемым VBO-путём и фактическим
     * фолбэком сразу объясняет пропажу блит-биндов из кадра.
     */
    public static final ThreadLocal<String> lastTrackMeshBranch = new ThreadLocal<>();
    private static final float ENTITY_MISSILE_DEPTH_FACTOR = -4.0F;
    private static final float ENTITY_MISSILE_DEPTH_UNITS = -4.0F;


    /**
     * Диагностика «меш прилипает к экрану»: раз в секунду логируем итоговую
     * ModelView и её компоненты. Ожидание: RenderSystem MV = поворот камеры
     * (верхний левый 3x3 ортонормирован, столбец трансляции ≈ 0), полная
     * матрица несёт смещение меша относительно камеры.
     */
    private static long lastMvmLogMs = 0;
    private static void logMissileMatrices(Matrix4f fullModelView, String mvSource) {
        long now = System.currentTimeMillis();
        if (now - lastMvmLogMs < 1000) return;
        lastMvmLogMs = now;
        try {
            org.joml.Matrix4f rsMv = new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix());
            MainRegistry.LOGGER.info(
                    "HBM vbo.mvm [iris={} src={}]: rsMV[col0=({},{},{}) col3=({},{},{})] full[m30={} m31={} m32={}]",
                    com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive(),
                    mvSource,
                    String.format("%.4f", rsMv.m00()), String.format("%.4f", rsMv.m10()), String.format("%.4f", rsMv.m20()),
                    String.format("%.4f", rsMv.m03()), String.format("%.4f", rsMv.m13()), String.format("%.4f", rsMv.m23()),
                    String.format("%.2f", fullModelView.m30()), String.format("%.2f", fullModelView.m31()),
                    String.format("%.2f", fullModelView.m32()));
        } catch (Throwable ignored) {
        }
    }

    public static void setFadeAlpha(float alpha) {
        currentFadeAlpha.set(alpha);
    }

    public static float getFadeAlpha() {
        return currentFadeAlpha.get();
    }

    public static void setWorldMissileOverlayDraw(boolean enabled) {
        worldMissileOverlayDraw.set(enabled);
    }

    public static boolean isWorldMissileOverlayDraw() {
        return worldMissileOverlayDraw.get();
    }

    public static void setEntityMissileDepthBias(boolean enabled) {
        entityMissileDepthBias.set(enabled);
    }

    private static void beginEntityMissileDepthBias() {
        if (entityMissileDepthBias.get()) {
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(ENTITY_MISSILE_DEPTH_FACTOR, ENTITY_MISSILE_DEPTH_UNITS);
        }
    }

    private static void endEntityMissileDepthBias() {
        if (entityMissileDepthBias.get()) {
            RenderSystem.disablePolygonOffset();
        }
    }

    // Scratch for 8-corner trilinear uniform upload in the non-instanced path.
    // tmpLocalPose holds the per-BE transform stripped of both the camera view
    // rotation (baked in by GameRenderer.renderLevel) and the
    // (blockPos - cameraPos) offset (applied by LevelRenderer). See the long
    // comment in {@link InstancedStaticPartRenderer#addInstance} for the
    // derivation and the precision argument.
    private final Matrix4f tmpLocalPose = new Matrix4f();
    private final Matrix4f tmpInvViewRot = new Matrix4f();
    private final float[] tmpCornerUV = new float[16];

    // Cached block_lit uniform handles (per renderer instance, invalidated on shader relink).
    private ShaderInstance cachedBlockLitShader;
    private int cachedBlockLitProgramId = -1;
    private long cachedBlockLitPipelineGen = -1L;
    private Uniform cachedBboxMinU;
    private Uniform cachedBboxSizeU;
    private Uniform cachedLightC01;
    private Uniform cachedLightC23;
    private Uniform cachedLightC45;
    private Uniform cachedLightC67;
    private Uniform cachedFogStartU;
    private Uniform cachedFogEndU;
    private Uniform cachedFogColorU;
    private Uniform cachedFadeAlphaU;

    private void updateBlockLitUniformCache(ShaderInstance shader) {
        int programId = (shader != null) ? shader.getId() : -1;
        long pipelineGen = IrisExtendedShaderAccess.getPipelineGeneration();
        if (cachedBlockLitShader == shader
                && cachedBlockLitProgramId == programId
                && cachedBlockLitPipelineGen == pipelineGen
                && cachedBlockLitShader != null) {
            return;
        }
        cachedBlockLitShader = shader;
        cachedBlockLitProgramId = programId;
        cachedBlockLitPipelineGen = pipelineGen;
        if (shader == null) {
            cachedBboxMinU = null;
            cachedBboxSizeU = null;
            cachedLightC01 = null;
            cachedLightC23 = null;
            cachedLightC45 = null;
            cachedLightC67 = null;
            cachedFogStartU = null;
            cachedFogEndU = null;
            cachedFogColorU = null;
            cachedFadeAlphaU = null;
            return;
        }
        cachedBboxMinU = shader.getUniform("BboxMin");
        cachedBboxSizeU = shader.getUniform("BboxSize");
        cachedLightC01 = shader.getUniform("LightC01");
        cachedLightC23 = shader.getUniform("LightC23");
        cachedLightC45 = shader.getUniform("LightC45");
        cachedLightC67 = shader.getUniform("LightC67");
        cachedFogStartU = shader.getUniform("FogStart");
        cachedFogEndU = shader.getUniform("FogEnd");
        cachedFogColorU = shader.getUniform("FogColor");
        cachedFadeAlphaU = shader.getUniform("FadeAlpha");
    }

    protected abstract VboData buildVboData();

    /**
     * Квады для Iris-совместимого пути (BufferBuilder + GameRenderer shader).
     * Переопределяется в рендерерах, созданных через MeshRenderCache.
     */
    protected List<BakedQuad> getQuadsForIrisPath() {
        return null;
    }

    static final class TextureBinder {
        static void bindForModelIfNeeded(ShaderInstance shader) {
            var minecraft = Minecraft.getInstance();
            var textureManager = minecraft.getTextureManager();

            // Только управляемые бинды: сырые glActiveTexture/glBindTexture
            // рассинхронизируют кеш GlStateManager — последующие управляемые
            // бинды ванили но-опятся (чёрные квадраты солнца/луны, битая рука).
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            var blockAtlas = textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS);
            GlStateManager._bindTexture(blockAtlas.getId());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // РЕГРЕССИЯ-СТОП: instanced block_lit — белые модели без текстур атласа
    // ═══════════════════════════════════════════════════════════════════════════
    // Симптом: инстансинг включён → все OBJ белые; выключен → нормально.
    // Причина A: Sampler2 uniform=0 → FS читает atlas вместо lightmap (угол ~белый).
    // Причина B: turnOnLightLayer() при active TEXTURE0 → atlas на unit 0 затирается.
    // Причина C: flush в AFTER_LEVEL, а не в AFTER_BLOCK_ENTITIES → слоты GL грязные.
    // Контракт: prepareBlockLitSamplers → apply → bindBlockLitSamplerTextures → draw.
    // НЕЛЬЗЯ: только apply(); только setSampler без GL bind; flush в конце уровня.
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Primes the shader's sampler map so that {@link ShaderInstance#apply()} binds the
     * block atlas to {@code Sampler0} and the dynamic lightmap to {@code Sampler2} for
     * the non-Iris {@code block_lit} pipeline. MUST be called <b>before</b>
     * {@code shader.apply()}.
     * <p>
     * <b>Why.</b> {@code ShaderInstance.apply()} iterates samplers by JSON array index
     * {@code j}, uploads {@code Sampler"j"} uniform = {@code j}, activates
     * {@code GL_TEXTURE0+j}, and binds the texture from {@code samplerMap}. If
     * {@code samplerMap.get(name)} is {@code null}, the entry is skipped — uniform
     * stays at its link-time default of 0, so every sampler reads from
     * {@code GL_TEXTURE0} (the block atlas). That's the root cause of the
     * "everything is solid white" regression: {@code texture(Sampler2, lightmapUV)}
     * was reading the atlas at {@code uv ≈ (0.97, 0.97)} — a near-white atlas
     * corner — instead of the lightmap.
     * <p>
     * Vanilla's {@code VertexBuffer._drawWithShader} solves this by calling
     * {@code shader.setSampler("Sampler" + i, RenderSystem.getShaderTexture(i))} for
     * {@code i = 0..11} before {@code apply()}. We replicate that here because our
     * render path goes straight through {@code glDrawElements} and bypasses
     * {@code VertexBuffer}.
     * <p>
     * For JSON samplers {@code ["Sampler0", "Sampler2"]}:
     * <ul>
     *   <li>{@code j=0}: {@code Sampler0} uniform = 0, atlas bound to {@code GL_TEXTURE0}</li>
     *   <li>{@code j=1}: {@code Sampler2} uniform = 1, lightmap bound to {@code GL_TEXTURE1}</li>
     * </ul>
     * The shader's {@code texture(Sampler2, lightmapUV)} then correctly reads
     * {@code GL_TEXTURE1} (the lightmap) via the {@code Sampler2} uniform value of 1.
     * The name "Sampler2" is purely cosmetic — vanilla uses the same convention for
     * {@code rendertype_solid}.
     */
    public static void prepareBlockLitSamplers(ShaderInstance shader) {
        if (shader == null) {
            return;
        }
        RenderFrameLight.ensureLightTextureUpdated();
        primeBlockLitSamplerMap(shader, Minecraft.getInstance());
    }

    /**
     * Fills {@code samplerMap} so {@link ShaderInstance#apply()} binds atlas → unit 0 and
     * lightmap → unit 1. Mirrors {@code LevelRenderer} / {@code VertexBuffer._drawWithShader}.
     */
    private static void primeBlockLitSamplerMap(ShaderInstance shader, Minecraft mc) {
        var textureManager = mc.getTextureManager();

        // НЕЛЬЗЯ вызывать turnOnLightLayer() пока active TEXTURE0 — перезапишет atlas (белые модели).
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        textureManager.bindForSetup(TextureAtlas.LOCATION_BLOCKS);

        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        mc.gameRenderer.lightTexture().turnOnLightLayer();

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        textureManager.bindForSetup(TextureAtlas.LOCATION_BLOCKS);

        AbstractTexture atlasTex = textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS);
        int lightmapGlId = resolveLightmapGlId(mc);
        if (atlasTex != null) {
            shader.setSampler("Sampler0", atlasTex);
        } else {
            int atlasGlId = resolveBlockAtlasGlId(mc);
            if (atlasGlId > 0) {
                shader.setSampler("Sampler0", atlasGlId);
            }
        }
        // Имя "Sampler2" в JSON ≠ GL_TEXTURE2: apply() кладёт его на unit 1 (индекс j в массиве).
        if (lightmapGlId > 0) {
            shader.setSampler("Sampler2", lightmapGlId);
        }
    }

    /** GL texture id for the block atlas — never trust slot 0 alone after chunk/MDI draws. */
    private static int resolveBlockAtlasGlId(Minecraft mc) {
        AbstractTexture atlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        if (atlas != null) {
            return atlas.getId();
        }
        int fromSlot = RenderSystem.getShaderTexture(0);
        return fromSlot > 0 ? fromSlot : -1;
    }

    /**
     * GL texture id for the dynamic lightmap. {@link LightTexture#turnOnLightLayer()} must run
     * while {@code GL_TEXTURE1} is active (see {@link #prepareBlockLitSamplers}).
     */
    private static int resolveLightmapGlId(Minecraft mc) {
        int fromSlot = RenderSystem.getShaderTexture(2);
        if (fromSlot > 0) {
            return fromSlot;
        }
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        return bound > 0 ? bound : -1;
    }

    /**
     * Re-binds block atlas ({@code Sampler0} → {@code GL_TEXTURE0}) and dynamic lightmap
     * ({@code Sampler2} → {@code GL_TEXTURE1}) immediately before instanced draws.
     * <p>
     * {@link ShaderInstance#apply()} maps JSON sampler index {@code j} to {@code GL_TEXTURE0 + j}
     * (so {@code Sampler2} uses unit 1, not 2). If {@code Sampler2} stays at 0, the fragment
     * shader samples the atlas near {@code (0.97, 0.97)} and machines look solid white.
     * VAO / instance-buffer work between {@code apply()} and the draw must not rely on
     * {@code apply()} alone — force GL binds here.
     */
    /**
     * Вызывать <b>после</b> {@link ShaderInstance#apply()} и <b>перед</b> любым instanced glDraw*.
     * Пропуск = белые модели (см. блок «РЕГРЕССИЯ-СТОП» над {@link #prepareBlockLitSamplers}).
     */
    public static void bindBlockLitSamplerTextures(ShaderInstance shader) {
        if (shader == null) return;
        RenderSystem.assertOnRenderThread();
        Minecraft mc = Minecraft.getInstance();

        int atlasGlId = resolveBlockAtlasGlId(mc);
        int lightmapGlId = resolveLightmapGlId(mc);
        if (atlasGlId <= 0 || lightmapGlId <= 0) return;

        // TU0: Атлас (управляемо: кеш GlStateManager остаётся честным)
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(atlasGlId);

        // TU1: Оверлей
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE1);
        mc.gameRenderer.overlayTexture().setupOverlayColor(); // Внутри уже использует GlStateManager

        // TU2: Лайтмап
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE2);
        GlStateManager._bindTexture(lightmapGlId);

        // ОБЯЗАТЕЛЬНО возвращаем кеш и физику на TU0 для всего остального рендера игры!
        GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);

        shader.setSampler("Sampler0", atlasGlId);
        shader.setSampler("Sampler2", lightmapGlId);

        var uSampler0 = shader.getUniform("Sampler0");
        if (uSampler0 != null) uSampler0.set(0);
        var uSampler1 = shader.getUniform("Sampler1");
        if (uSampler1 != null) uSampler1.set(1);
        var uSampler2 = shader.getUniform("Sampler2");
        if (uSampler2 != null) uSampler2.set(2);
    }

    private boolean shouldRenderWithCulling(BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return true;
        }

        AABB renderBounds = worldBoundsFromMesh(blockEntity);
        return OcclusionCullingHelper.shouldRender(blockPos, blockEntity.getLevel(), renderBounds);
    }

    /** Без Forge {@code getRenderBoundingBox}: мирный AABB из позиции BE и object-space {@link #objBbox}. */
    private AABB worldBoundsFromMesh(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() + objBbox[0], pos.getY() + objBbox[1], pos.getZ() + objBbox[2],
                pos.getX() + objBbox[3], pos.getY() + objBbox[4], pos.getZ() + objBbox[5]
        );
    }

    @Nullable
    private IrisCompanionMesh getOrBuildIrisCompanion() {
        if (irisCompanion != null && irisCompanion.isBuilt()) return irisCompanion;
        if (irisCompanion != null && irisCompanion.isFailed()) return null;
        if (irisCompanionAttempted && irisCompanion == null) return null;

        List<BakedQuad> quads = getQuadsForIrisPath();
        if (quads == null || quads.isEmpty()) {
            irisCompanionAttempted = true;
            return null;
        }
        if (irisCompanion == null) {
            irisCompanion = new IrisCompanionMesh(quads);
            irisCompanionAttempted = true;
        }
        return irisCompanion.ensureBuilt() ? irisCompanion : null;
    }

    protected void initVbo() {
        if (initialized) return;

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousElementArrayBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        VboData data = null;

        try {
            vaoId = GL30.glGenVertexArrays();
            vboId = GL15.glGenBuffers();

            data = buildVboData();
            if (data == null) {
                MainRegistry.LOGGER.warn("VboData is null, cannot initialize VBO");
                throw new IllegalStateException("VboData is null");
            }
            indexCount = data.indices != null ? data.indices.remaining() : 0;
            setObjBboxFrom(data);

            int vs = data.bytesPerVertex;

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, vs, 0);

            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, vs, 12);

            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, vs, 24);

            if (data.indices != null && data.indices.remaining() > 0) {
                eboId = GL15.glGenBuffers();
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
            }

            GL30.glBindVertexArray(0);

            data.close();

            initialized = true;

        } catch (Exception e) {
            if (data != null) {
                data.close();
            }

            if (vaoId != -1) {
                GL30.glDeleteVertexArrays(vaoId);
                vaoId = -1;
            }
            if (vboId != -1) {
                GL15.glDeleteBuffers(vboId);
                vboId = -1;
            }
            if (eboId != -1) {
                GL15.glDeleteBuffers(eboId);
                eboId = -1;
            }

            throw e;

        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);

            if (previousVao == 0) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, previousElementArrayBuffer);
            }
        }
    }

    private void renderToBufferSource(PoseStack poseStack, int packedLight, List<BakedQuad> quads, MultiBufferSource bufferSource) {
        if (quads == null || quads.isEmpty() || bufferSource == null) return;
        float fade = currentFadeAlpha.get();
        var consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.cutout());
        var pose = poseStack.last();
        for (BakedQuad quad : quads) {
            RenderHooks.putBulkData(consumer, pose, quad, 1f, 1f, 1f, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
        }
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos) {
        render(poseStack, packedLight, blockPos, null, null);
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos,
                       @Nullable BlockEntity blockEntity) {
        render(poseStack, packedLight, blockPos, blockEntity, null);
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos,
                       @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        // Per-part culling removed: all current call sites (multiblock BERs)
        // perform a single per-BlockEntity OcclusionCullingHelper.shouldRender()
        // check with the full multiblock AABB BEFORE invoking render() on each
        // part. The per-mesh AABB computed by worldBoundsFromMesh() is much
        // smaller and does not cover the structure's dummy blocks, so the
        // structure's own solid blocks register as occluders and cause the
        // model to flicker when the camera moves. This matches the same fix
        // already applied in InstancedStaticPartRenderer.addInstance().

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // 1) Iris ExtendedShader path through our companion mesh.
            if (renderWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            // 2) Fallback: classic putBulkData delegation lets Iris's pipeline render us as
            //    plain terrain quads. Used when companion mesh build failed or Iris reflection
            //    is unavailable.
            List<BakedQuad> irisQuads = getQuadsForIrisPath();
            if (irisQuads != null && bufferSource != null) {
                renderToBufferSource(poseStack, packedLight, irisQuads, bufferSource);
            }
            return;
        }

        if (!initialized && !initFailed) {
            try {
                initVbo();
            } catch (Exception e) {
                initFailed = true;
                MainRegistry.LOGGER.debug("VBO init failed (part has no geometry or other error), skipping: {}", e.getMessage());
                vaoId = -1;
                vboId = -1;
                eboId = -1;
                return;
            }
        }
        if (entityMissileDepthBias.get()) {
            lastTrackMeshBranch.set("none");
        }
        if (initFailed) return;
        if (!initialized || vaoId <= 0 || vboId <= 0) {
            return;
        }

        if (eboId <= 0 || indexCount <= 0) {
            return;
        }

        ShaderInstance shader = ModShaders.getBlockLitSimpleShader();
        if (shader == null) {
            // Shader not loaded yet (resource reload race) - fall back to putBulkData.
            if (entityMissileDepthBias.get()) lastTrackMeshBranch.set("quads:shader-null");
            List<BakedQuad> fallbackQuads = getQuadsForIrisPath();
            if (fallbackQuads != null && bufferSource != null) {
                renderToBufferSource(poseStack, packedLight, fallbackQuads, bufferSource);
            }
            return;
        }
        if (entityMissileDepthBias.get()) lastTrackMeshBranch.set("vbo");

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean previousCullFaceEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        // Snapshot depth state: the try body force-sets depthFunc/depthMask/depthTest
        // for both the overlay and the normal draw path. Previously only the overlay
        // path restored depth in finally, so the normal path left the caller's
        // depthFunc (GL_LEQUAL) and depthMask(true) permanently clobbered.
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean previousDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        ShaderInstance previousShader = RenderSystem.getShader();
        int previousTexture0 = RenderSystem.getShaderTexture(0);

        try {
            RenderSystem.setShader(() -> shader);
            // Источник поворота камеры для меша ракет: ambient RenderSystem
            // ModelViewMat под Oculus даже с выключенным паком бывает перезаписан
            // в identity ЧУЖИМ bookkeeping'ом уже ВНУТРИ нашего окна пуша
            // (диагностика «vbo.mvm»: rsMV=identity в кадрах трека). Поэтому в
            // контексте трека берём захваченную копию из RenderHooks; там, где
            // ambient корректен, результат идентичен (пуш кладёт ту же матрицу).
            // Станки/двери (флаг false) продолжают читать ambient = R_cam фазы BE.
            org.joml.Matrix4f fullModelView;
            String mvSource;
            if (entityMissileDepthBias.get()) {
                org.joml.Matrix4f levelRot =
                        com.hbm_m.platform.RenderHooks.currentLevelRotation();
                if (levelRot != null) {
                    // Track-путь: поворот камеры УЖЕ запечён в poseStack
                    // (MissileTrackWorldRender домножает R_cam при построении
                    // стопки — в чистой ваниле ambient MV на AFTER_WEATHER
                    // identity, умножение дало бы двойной поворот/мусор).
                    fullModelView = new Matrix4f(poseStack.last().pose());
                    mvSource = "baked";
                } else {
                    fullModelView = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix())
                            .mul(poseStack.last().pose());
                    mvSource = "ambient";
                }
            } else {
                fullModelView = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix())
                        .mul(poseStack.last().pose());
                mvSource = "ambient";
            }
            logMissileMatrices(fullModelView, mvSource);
            if (shader.MODEL_VIEW_MATRIX != null)
                shader.MODEL_VIEW_MATRIX.set(fullModelView);
            if (shader.PROJECTION_MATRIX != null)
                shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());

            // Lighting uniforms: either 8-corner trilinear or 2x4x2 sliced probes.
            //
            // The BER poseStack carries BOTH the camera view rotation (baked
            // in by GameRenderer.renderLevel before calling LevelRenderer) AND
            // the per-dispatch translate(blockPos - cameraPos) Mojang applies
            // in LevelRenderer's block-entities loop:
            //
            //   mat = viewRot * T( (float)(blockPos - cameraPos) ) * perBELocal
            //
            // Naively composing T(cameraPos) * mat leaves an extra viewRot
            // between cameraPos and the offset: the 8 sampled corners rotate
            // with the camera and drift into opaque blocks / underground /
            // sky (symptom: "models darken from the bottom up when looking
            // up"). Even after stripping viewRot, building a full absolute
            // world pose inside a Matrix4f loses float32 precision at large
            // camera offsets - (float)cameraPos + (float)(blockPos - cameraPos)
            // doesn't exactly equal (float)blockPos at cameraPos > 10^4, and
            // Mth.floor on the composed translation jitters between adjacent
            // blocks as the player moves sub-block distances (symptom:
            // "model shimmers between light and dark near a torch").
            //
            // Fix: keep the math block-relative. Strip viewRot using
            // RenderSystem.getInverseViewRotationMatrix() (Mojang stamps this
            // right before dispatching the level), then subtract the
            // (blockPos - cameraPos) translation column using the EXACT same
            // float cast LevelRenderer used - rounding errors cancel
            // bit-for-bit and we end up with a clean perBELocal matrix.
            // LightSampleCache then derives world sample positions as
            // blockPos.getX() + floor(perBELocal * corner.x), with no
            // absolute-world float arithmetic in the flooring step.
            //
            // See the matching (more detailed) comment in
            // InstancedStaticPartRenderer.addInstance.
            long partHash = System.identityHashCode(this);
            if (LightSampleCache.BASE_POSE_SET.get()) {
                tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(poseStack.last().pose());
            } else {
                var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                //? if < 1.21.1 {
                tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
                //?} else {
                /*tmpInvViewRot.identity().rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation()).invert();
                *///?}
                tmpLocalPose.set(tmpInvViewRot).mul(poseStack.last().pose());
                tmpLocalPose.m30(tmpLocalPose.m30() - (float) (blockPos.getX() - cam.x));
                tmpLocalPose.m31(tmpLocalPose.m31() - (float) (blockPos.getY() - cam.y));
                tmpLocalPose.m32(tmpLocalPose.m32() - (float) (blockPos.getZ() - cam.z));
            }
            LightSampleCache.getOrSample8(blockEntity, partHash, objBbox, blockPos,
                                          tmpLocalPose, packedLight, tmpCornerUV);

            updateBlockLitUniformCache(shader);
            if (cachedBboxMinU != null) cachedBboxMinU.set(objBbox[0], objBbox[1], objBbox[2]);
            if (cachedBboxSizeU != null) {
                cachedBboxSizeU.set(
                    Math.max(1e-4f, objBbox[3] - objBbox[0]),
                    Math.max(1e-4f, objBbox[4] - objBbox[1]),
                    Math.max(1e-4f, objBbox[5] - objBbox[2])
                );
            }
            if (cachedLightC01 != null) cachedLightC01.set(tmpCornerUV[0], tmpCornerUV[1], tmpCornerUV[2], tmpCornerUV[3]);
            if (cachedLightC23 != null) cachedLightC23.set(tmpCornerUV[4], tmpCornerUV[5], tmpCornerUV[6], tmpCornerUV[7]);
            if (cachedLightC45 != null) cachedLightC45.set(tmpCornerUV[8], tmpCornerUV[9], tmpCornerUV[10], tmpCornerUV[11]);
            if (cachedLightC67 != null) cachedLightC67.set(tmpCornerUV[12], tmpCornerUV[13], tmpCornerUV[14], tmpCornerUV[15]);

            if (worldMissileOverlayDraw.get() || entityMissileDepthBias.get()) {
                if (cachedFogStartU != null) cachedFogStartU.set(1.0E8F);
                if (cachedFogEndU != null) cachedFogEndU.set(1.0E8F);
            } else {
                if (cachedFogStartU != null) cachedFogStartU.set(RenderSystem.getShaderFogStart());
                if (cachedFogEndU != null) cachedFogEndU.set(RenderSystem.getShaderFogEnd());
            }
            if (cachedFogColorU != null) {
                float[] fogColor = RenderSystem.getShaderFogColor();
                cachedFogColorU.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
            }

            if (cachedFadeAlphaU != null) cachedFadeAlphaU.set(currentFadeAlpha.get());

            // Must come BEFORE apply() - apply() reads samplerMap populated here and
            // does glUseProgram + glUniform1i + glBindTexture in one shot.
            prepareBlockLitSamplers(shader);
            if (entityMissileDepthBias.get()) {
                com.hbm_m.client.render.FrameStateProbe.snap("s1.prep");
            }
            // ЗАЩИТА ОТ DESYNC КЕША ПРОГРАММЫ (Oculus без шейдерпака):
            // VanillaRenderingPipeline.beginLevelRendering() один раз за кадр
            // делает сырой GlStateManager._glUseProgram(0), не сбрасывая статический
            // ShaderInstance.lastProgramId. Если прошлый кадр закончил our block_lit,
            // следующий shader.apply() ПРОПУСТИТ реальный glUseProgram (кеш совпадает),
            // и все glUniform/glDrawElements уйдут в программу 0 — «No active program»,
            // чёрный меш с мусорной матрицей. Явная проверка GL_CURRENT_PROGRAM:
            // при расхождении clear() обнуляет lastProgramId (-1) и форсирует честный
            // бинд в apply(). В норме — один glGetInteger, дешевле apply().
            com.hbm_m.client.render.shader.ShaderBindResync.ensureFreshBind(shader);
            shader.apply();
            if (entityMissileDepthBias.get()) {
                com.hbm_m.client.render.FrameStateProbe.snap("s1.apply");
            }
            bindBlockLitSamplerTextures(shader);

            float fade = currentFadeAlpha.get();
            boolean overlay = worldMissileOverlayDraw.get();
            if (overlay) {
                RenderSystem.disableDepthTest();
                // Управляемый вызов: сырой GL11.glDepthMask обходил кеш
                // GlStateManager и рассинхронизировал depthMask для всех
                // последующих прозрачных дро (частицы писали глубину).
                RenderSystem.depthMask(false);
            } else {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
            }
            RenderSystem.disableCull();
            if (fade < 0.99f) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            }

            GlVaoSafety.bindVertexArray(vaoId);
            beginEntityMissileDepthBias();
            GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
            endEntityMissileDepthBias();
            if (entityMissileDepthBias.get()) {
                com.hbm_m.client.render.FrameStateProbe.snap("s1.drawn");
                int glErr = GL11.glGetError();
                while (glErr != GL11.GL_NO_ERROR) {
                    MainRegistry.LOGGER.error("HBM VBO draw GL error after track mesh: {}", glErr);
                    glErr = GL11.glGetError();
                }
            }

            if (fade < 0.99f) {
                RenderSystem.disableBlend();
            }

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during VBO render", e);
        } finally {
            GlVaoSafety.bindVertexArray(0);
            if (previousShader != null) {
                RenderSystem.setShader(() -> previousShader);
            }
            RenderSystem.setShaderTexture(0, previousTexture0);

            // ВАЖНО: НЕ анбиндим TU1/TU2. Сырой glBindTexture(0) оставлял кеш
            // GlStateManager с «живыми» оверлеем/лайтмапой — все последующие
            // управляемые бинды но-опились, и рендер после нашего меша (рука,
            // GUI на Fast, небо следующего кадра) сэмплировал пустоту.
            // Байнды bindBlockLitSamplerTextures управляемые и указывают на те
            // же текстуры, что ваниль держит в этих юнитах — «утечка» не вредит.

            GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);

            if (previousCullFaceEnabled) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
            RenderSystem.depthFunc(previousDepthFunc);
            // Восстановление через управляемый API — симметрично установке выше.
            RenderSystem.depthMask(previousDepthMask);
            if (previousDepthTestEnabled) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
        }
    }

    /**
     * Render through the Iris {@code ExtendedShader} with the lazy companion mesh.
     * Returns {@code true} if rendering happened; the caller should fall through to a
     * different path on {@code false}.
     * <p>
     * <b>Fast path:</b> when an {@link IrisRenderBatch} session is currently open
     * (typically opened by the BlockEntityRenderer that wraps multiple part draws
     * for one machine), we skip the entire shader setup and only emit the per-part
     * VAO bind, ModelViewMat upload and {@code glDrawElements}. The session pays
     * the heavy {@code apply}/{@code clear} cost once for all parts in the batch
     * - see {@link IrisRenderBatch} for the full rationale.
     */
    private boolean renderWithIrisExtended(PoseStack poseStack, int packedLight,
                                           BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        // Shadow pass: только через АКТИВНЫЙ per-BE батч (см. IrisRenderBatch
        // .begin — неперсистентный, закрывается до возврата из BER). Standalone
        // путь (apply на каждую часть) в shadow запрещён: teardown ExtendedShader
        // .clear() ребиндит MAIN FBO, а его случайные срабатывания по ходу
        // основного прохода задваивали растительность на 1.20.1. Без батча —
        // возвращаем false, вызывающий {@link #render} уйдёт в putBulkData
        // через bufferSource (Iris нарисует SHADOW_BLOCK-программой на endBatch).
        if (ShaderCompatibilityDetector.isRenderingShadowPass() && IrisRenderBatch.active() == null) {
            return false;
        }

        IrisCompanionMesh companion = getOrBuildIrisCompanion();
        if (companion == null) {
            return false;
        }

        // Skip 8-corner sampling entirely during Iris's shadow pass. Shadow
        // maps are depth-only and pack shadow programs ignore vaUV2; the
        // sampling also populates LightSampleCache under the shadow camera's
        // RenderSystem state, which the main pass then re-uses from the same
        // frame and renders incorrect block-light gradients with (symptom:
        // "the bright stripe runs sideways across a row of machines when I
        // pitch the camera up/down"). See IrisRenderBatch.drawCompanionWith-
        // PerVertexLight for the matching short-circuit on the draw side.
        boolean shadowPassEarly = ShaderCompatibilityDetector.isRenderingShadowPass();
        IrisRenderBatch batchEarly = IrisRenderBatch.active();
        if (batchEarly != null) shadowPassEarly = batchEarly.isShadowPass();

        // Sample world-space light probes for this draw: 2×2×2 corners (16 floats).
        // See {@link #render} for the same localPose reconstruction as the
        // vanilla / instanced path.
        boolean haveCorners = false;
        if (!shadowPassEarly && companion.supportsPerVertexLightmap()) {
            BlockPos anchor = (blockEntity != null) ? blockEntity.getBlockPos() : blockPos;
            if (anchor == null) anchor = BlockPos.ZERO;
            if (LightSampleCache.BASE_POSE_SET.get()) {
                tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(poseStack.last().pose());
            } else {
                var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                //? if < 1.21.1 {
                tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
                //?} else {
                /*tmpInvViewRot.identity().rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation()).invert();
                *///?}
                tmpLocalPose.set(tmpInvViewRot).mul(poseStack.last().pose());
                tmpLocalPose.m30(tmpLocalPose.m30() - (float) (anchor.getX() - cam.x));
                tmpLocalPose.m31(tmpLocalPose.m31() - (float) (anchor.getY() - cam.y));
                tmpLocalPose.m32(tmpLocalPose.m32() - (float) (anchor.getZ() - cam.z));
            }

            long partHash = System.identityHashCode(this);
            LightSampleCache.getOrSample8(blockEntity, partHash, objBbox, anchor,
                                          tmpLocalPose, packedLight, tmpCornerUV);
            haveCorners = true;
        }

        // Fast path: a batch session is open - every other part of the same
        // BlockEntity is draining apply()/clear() through it as well, so we
        // just submit our draw and exit. The session takes care of state
        // restoration on its own close(). Use the per-vertex variant when we
        // successfully gathered the 8 corner samples, else fall back to the
        // legacy constant-UV2 path.
        IrisRenderBatch batch = IrisRenderBatch.active();
        if (batch != null) {
            if (haveCorners) {
                batch.drawCompanionWithPerVertexLight(companion, poseStack.last().pose(),
                                                      tmpCornerUV, packedLight);
            } else {
                batch.drawCompanion(companion, poseStack.last().pose(), packedLight);
            }
            return true;
        }

        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        ShaderInstance shader = IrisExtendedShaderAccess.getBlockShader(shadowPass);
        if (shader == null) {
            return false;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean previousCullFaceEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        // Тело try ставит depthFunc/depthMask/depthTest для обеих веток (overlay и
        // обычной); раньше finally восстанавливал их только для overlay-пути.
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean previousDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        // Neutral blockEntityId so BSL & co. don't take EMISSIVE_RECOLOR /
        // DrawEndPortal branches based on whatever BE Iris rendered last.
        int previousBlockEntityId = IrisExtendedShaderAccess.setCurrentRenderedBlockEntity(0);

        try (IrisPhaseGuard ignored = IrisPhaseGuard.pushBlockEntities()) {
            RenderSystem.setShader(() -> shader);

            if (shader.MODEL_VIEW_MATRIX != null) {
                // Track-путь (AFTER_WEATHER, под Iris/Oculus): R_cam уже запечён
                // в poseStack (MissileTrackWorldRender), а амбиентная
                // RenderSystem.getModelViewMatrix() здесь НЕ равна R_cam — чужой
                // bookkeeping сбрасывает её в identity/мусор → двойной поворот
                // или «улетающий» меш. Берём pose целиком — как в ванильном
                // VBO-пути выше (ветка "baked"). Для BER/станков (вне track-
                // контекста) композит ambient × pose остаётся корректным: там
                // ambient = R_cam фазы BE.
                if (entityMissileDepthBias.get()
                        && com.hbm_m.platform.RenderHooks.currentLevelRotation() != null) {
                    shader.MODEL_VIEW_MATRIX.set(new Matrix4f(poseStack.last().pose()));
                } else {
                    shader.MODEL_VIEW_MATRIX.set(new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose()));
                }
            }
            if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());

            var brightnessUniform = shader.getUniform("Brightness");
            if (brightnessUniform != null) brightnessUniform.set(calculateBrightness(packedLight));

            if (worldMissileOverlayDraw.get()) {
                var fogStart = shader.getUniform("FogStart");
                if (fogStart != null) fogStart.set(1.0E8F);
                var fogEnd = shader.getUniform("FogEnd");
                if (fogEnd != null) fogEnd.set(1.0E8F);
            }

            var sampler0 = shader.getUniform("Sampler0");
            if (sampler0 != null) sampler0.set(0);

            // ExtendedShader.apply() reads RenderSystem.getShaderTexture(0..2)
            // and binds those IDs to the IrisSamplers ALBEDO/OVERLAY/LIGHTMAP
            // units. Other rendering paths (Embeddium chunk uploads, particle
            // batches) can leave wrong IDs in those slots, which would cause
            // the pack shader to sample the lightmap as the albedo and render
            // the model as a solid orange. Explicitly re-point the slots to
            // the correct atlas/overlay/lightmap textures before apply().
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            TextureBinder.bindForModelIfNeeded(shader);

            com.mojang.blaze3d.platform.GlStateManager._glBindVertexArray(companion.getVaoId());
            
            if (!com.hbm_m.client.render.shader.IrisShaderApply.tryApply(shader)) {
                return false;
            }

            boolean overlay = worldMissileOverlayDraw.get();
            if (overlay) {
                RenderSystem.disableDepthTest();
                // Управляемый вызов вместо сырого GL11.glDepthMask (см. vanilla-путь).
                RenderSystem.depthMask(false);
            } else {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
            }
            RenderSystem.disableCull();

            

            // Bind the Iris-extended attributes (iris_Entity, mc_midTexCoord,
            // at_tangent) to their linker-resolved locations on this VAO with
            // pointers into our VBO at the correct byte offsets. Iris's
            // MixinBufferBuilder.iris$beforeNext already populated the VBO with
            // valid per-vertex data for these attributes, so once bound at the
            // location the GLSL linker actually picked, the shader reads stable
            // real data and is no longer susceptible to "current value bank"
            // pollution from Embeddium chunk uploads, redstone particle batches
            // or any other immediate-mode draw - the root cause of the
            // intermittent broken-geometry symptom near torches and powered
            // redstone components. Cached per program ID; F3+T re-link
            // automatically invalidates by minting a new ID.
            companion.prepareForShader(shader.getId());

            // Per-draw lightmap. Prefer the per-vertex trilinear path so the
            // pack shader gets a smooth gradient across the mesh (a torch on
            // one side of the part actually brightens just that side).
            // Falls back to the legacy constant-UV2 path when per-vertex
            // isn't available or we didn't sample the 8 corners above
            // (degenerate mesh, Iris pre-flush race).
            int uv2Loc = companion.getUv2Location();
            if (haveCorners && companion.supportsPerVertexLightmap()) {
                companion.ensureLightmapCapacity(1);
                companion.writeInstanceLightmap(0, tmpCornerUV);
                companion.finishLightmapWrites();
                companion.activatePerVertexLightmap();
                companion.bindLightmapForInstance(0);
            } else if (uv2Loc != -1) {
                companion.restoreConstantLightmap();
                int blockU = Math.max(0, Math.min(240, packedLight & 0xFFFF));
                int skyV   = Math.max(0, Math.min(240, (packedLight >>> 16) & 0xFFFF));
                companion.bindVaoIfNeeded();
                GL30.glVertexAttribI2i(uv2Loc, blockU, skyV);
            }

            beginEntityMissileDepthBias();
            companion.bindVaoIfNeeded();
            GL11.glDrawElements(GL11.GL_TRIANGLES, companion.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
            endEntityMissileDepthBias();
            shader.clear();
            return true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("SingleMeshVboRenderer.renderWithIrisExtended failed", e);
            return false;
        } finally {
            if (companion != null) {
                companion.restoreConstantLightmap();
            }
            GlVaoSafety.bindVertexArray(0);
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            RenderSystem.depthFunc(previousDepthFunc);
            // Управляемое восстановление — симметрично установке выше.
            RenderSystem.depthMask(previousDepthMask);
            if (previousDepthTestEnabled) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            if (previousCullFaceEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            IrisExtendedShaderAccess.restoreCurrentRenderedBlockEntity(previousBlockEntityId);
        }
    }

    private float calculateBrightness(int packedLight) {
        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);

        var level = Minecraft.getInstance().level;
        if (level == null) {
            return Math.max(0.05f, Math.max(blockLight, skyLight) / 15.0f);
        }

        float skyDarken = level.getSkyDarken(1.0f);
        float skyBrightness = 0.05f + (skyDarken * 0.95f);

        float effectiveSkyLight = skyLight * skyBrightness;
        float maxLight = Math.max(blockLight, effectiveSkyLight);

        return 0.05f + (maxLight / 15.0f) * 0.95f;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        IrisCompanionMesh toDestroy = this.irisCompanion;
        this.irisCompanion = null;
        if (toDestroy != null) {
            toDestroy.destroy();
        }
    }

    public static class VboData implements AutoCloseable {
        public final ByteBuffer byteBuffer;
        public final IntBuffer indices;
        /** Object-space AABB of the mesh, computed once while packing vertices. */
        public final float minX, minY, minZ, maxX, maxY, maxZ;
        /** Stride одной вершины в byteBuffer: pos(12) + normal(12) + uv(8) + boneId(int32) = 36. */
        public final int bytesPerVertex;
        
        private final java.util.concurrent.atomic.AtomicBoolean consumed = new java.util.concurrent.atomic.AtomicBoolean(false);
        private final java.lang.ref.Cleaner.Cleanable cleanable;

        private static final java.lang.ref.Cleaner CLEANER = java.lang.ref.Cleaner.create();

        private static record NativeResourceReleaser(long bbAddress, long ibAddress) implements Runnable {
            @Override
            public void run() {
                if (bbAddress != 0L) {
                    MemoryUtil.nmemFree(bbAddress);
                }
                if (ibAddress != 0L) {
                    MemoryUtil.nmemFree(ibAddress);
                }
            }
        }

        public VboData(ByteBuffer byteBuffer, IntBuffer indices) {
            this(byteBuffer, indices, 0f, 0f, 0f, 0f, 0f, 0f, MACHINE_PART_VERTEX_STRIDE_BYTES);
        }

        public VboData(ByteBuffer byteBuffer, IntBuffer indices,
                       float minX, float minY, float minZ,
                       float maxX, float maxY, float maxZ) {
            this(byteBuffer, indices, minX, minY, minZ, maxX, maxY, maxZ, MACHINE_PART_VERTEX_STRIDE_BYTES);
        }

        public VboData(ByteBuffer byteBuffer, IntBuffer indices,
                       float minX, float minY, float minZ,
                       float maxX, float maxY, float maxZ,
                       int bytesPerVertex) {
            this.byteBuffer = byteBuffer;
            this.indices = indices;
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
            this.bytesPerVertex = bytesPerVertex;

            long bbAddr = byteBuffer != null ? MemoryUtil.memAddress(byteBuffer) : 0L;
            long ibAddr = indices != null ? MemoryUtil.memAddress(indices) : 0L;
            this.cleanable = (bbAddr != 0L || ibAddr != 0L) 
                    ? CLEANER.register(this, new NativeResourceReleaser(bbAddr, ibAddr)) 
                    : null;
        }

        public boolean isConsumed() {
            return consumed.get();
        }

        @Override
        public void close() {
            if (consumed.compareAndSet(false, true)) {
                if (cleanable != null) {
                    cleanable.clean();
                }
            }
        }
    }
}
