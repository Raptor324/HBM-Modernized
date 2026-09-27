package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}


import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Instanced Renderer для статических частей (Base/Frame).
 * Без шейдеров рендерит все машины одного типа одним {@code glDrawElementsInstanced}.
 * Под Iris/Oculus переключается на per-machine draw через {@code ExtendedShader}
 * + companion VBO с {@code IrisVertexFormats.ENTITY} layout, что даёт корректный
 * G-buffer / shadow pass / pack uniforms.
 * <p>
 * Flush logic is delegated to {@link VanillaInstancedBatchRenderer} (vanilla path)
 * and {@link IrisInstancedBatchRenderer} (Iris/Oculus path).
 * GL compatibility helpers live in {@link InstancedGlCompat}.
 */
@OnlyIn(Dist.CLIENT)
public class InstancedStaticPartRenderer extends AbstractGpuMesh
        implements VanillaInstancedMeshRenderer, IrisCompanionMeshRenderer {

    /** Per-part instance cap (one renderer = one mesh part, e.g. ChemPlant/Base). */
    final int maxInstances = ClientRenderFlags.maxInstances();
    private static final java.util.concurrent.atomic.AtomicInteger OVERFLOW_ADD_COUNT =
            new java.util.concurrent.atomic.AtomicInteger();

    /** Diagnostics: consumed by {@link com.hbm_m.client.render.culling.InstancedRenderStats}. */
    public static int drainOverflowAddCount() {
        return OVERFLOW_ADD_COUNT.getAndSet(0);
    }

    // Per-instance layout (floats):
    //   InstPos       vec3 (loc 4) @ 0
    //   InstRot       vec4 (loc 5) @ 3
    //   InstBboxMin   vec3 (loc 6) @ 7
    //   InstBboxSize  vec4 (loc 7) @ 10 — xyz extent, w = fade
    //   InstLightC01  vec4 (loc 8) @ 14   -- c0.uv, c1.uv
    //   InstLightC23  vec4 (loc 9) @ 18   -- c2.uv, c3.uv
    //   InstLightC45  vec4 (loc 10) @ 22   -- c4.uv, c5.uv
    //   InstLightC67  vec4 (loc 11) @ 26  -- c6.uv, c7.uv
    //   InstUvRect    vec4 (loc 12) @ 30  -- uv0.x, uv0.y, du, dv (sprite-local VBO → atlas;
    //                                     identity 0,0,1,1 для обычных машин, см. UVRECT_FLOAT_OFFSET)
    //   InstColor     vec4 (loc 13) @ 34  -- per-instance RGBA-тинт (накал/свечение частей; RGB
    //                                     может быть > 1 — overbright), white = passthrough
    //   GradParams    vec4 (loc 14) @ 38  -- пространственный фоллофф тинта в МОДЕЛЬНЫХ координатах:
    //                                     x = ось (0/1/2, <0 = выключен), y = координата полного
    //                                     тинта (источник), z = координата нуля; плавный
    //                                     smoothstep между ними (block_lit_instanced.vsh)
    static final int INSTANCE_ATTRIB_FIRST = 4;
    static final int LIGHT_FLOAT_OFFSET = 14;
    static final int UVRECT_FLOAT_OFFSET = 30;
    static final int TINT_FLOAT_OFFSET = 34;
    static final int GRAD_FLOAT_OFFSET = 38;
    /** public: читается culling-пакетом (NucleusGpuCuller) и моддев-артефактами. */
    public static final int INSTANCE_DATA_SIZE = 42;

    final int instanceDataSize;
    final int instanceAttribLast;
    final int instanceFadeFloatOffset;
    final int lightFloatCount;

    int instanceCount = 0;
    final int[] instanceCullIndices = new int[maxInstances];
    final long[] instanceOcclusionKeys = new long[maxInstances];
    float batchSkyDarken = -1f;
    private boolean overflowLogged = false;
    /** В этой фазе записи была фактическая запись в instanceBuffer (skip-write не сработал хотя бы раз). */
    boolean mdiRecordWriteHappened = false;
    /**
     * Содержимое instanceBuffer синхронизировано со снапшотом MDI-координатора:
     * последний флаш ушёл accepted-сабмитом и с тех пор записей в буфер не было.
     * Сбрасывается direct-путём (партиция с перестановкой), renderSingle и неуспешными
     * сабмитами; истинность позволяет {@code submitClean} переиспользовать снапшот.
     */
    boolean mdiBufferSynced = false;
    static volatile boolean warnedInstancedShaderNullFlush;

    /**
     * Фаза-2 отложенное затухание прямого пути (MDI-fallback): {@code flush()} на
     * прямом пути рисует только opaque-инстансы,
     * а затухающие копирует в {@link #fadingSnapshot}; их добирает
     * {@link #flushFading(Matrix4f)} ПОСЛЕ MDI-мульти-драва (см.
     * InstancedRenderFrame). Иначе полупрозрачная геометрия, нарисованная раньше
     * непрозрачной MDI-базы (она уходит в координатор и рисуется в конце флаша),
     * пишет глубину и depth-reject'ит базу — вместо неё просвечивает чанк.
     */
    int deferredFadingCount = 0;
    FloatBuffer fadingSnapshot;

    final float[] instanceLightUV = new float[maxInstances * 2];

    /**
     * Текущий uvRect записи ({u0, v0, du, dv}); identity {0,0,1,1} для обычных машин.
     * Заполняется из {@link #addInstance} (новый 7-arg-оверрайд с uvRect) перед записью
     * рекорда и чтением {@code recordMatchesBuffer}; теневой батч пишет его же в рекорд.
     */
    final float[] tmpUvRect = {0f, 0f, 1f, 1f};
    private static final float[] IDENTITY_UV_RECT = {0f, 0f, 1f, 1f};

    /** Активный uvRect (для рекордов теневого батча); всегда length 4. */
    float[] getActiveUvRect() {
        return tmpUvRect;
    }

    /**
     * Текущий RGBA-тинт записи; RGB white = passthrough, A = сила эмиссии (heat 0..1) —
     * шейдеры доворачивают lightmap к fullbright на tint*gradient (glow следует тинту).
     * Заполняется из {@link #addInstance} перед записью рекорда; теневой батч пишет его же
     * в рекорд (теневой FB цвет не использует — важно только выравнивание стрида).
     */
    final float[] tmpTint = {1f, 1f, 1f, 0f};
    private static final float[] WHITE_TINT = {1f, 1f, 1f, 0f};

    /**
     * Текущий пространственный фоллофф тинта {axis, fullCoord, zeroCoord, pad};
     * axis &lt; 0 = выключен (множитель 1). Конфиг статический на часть
     * ({@code MachineSpecBuilder.tintFalloff}), но пишется в рекорд инстанса —
     * шейдер считает градиент по модельным координатам вершины.
     */
    final float[] tmpGrad = {-1f, 0f, 0f, 0f};

    /** Активный фоллофф (для рекордов теневого батча); всегда length 4. */
    float[] getActiveGrad() {
        return tmpGrad;
    }

    /** Активный тинт (для рекордов теневого батча); всегда length 4. */
    float[] getActiveTint() {
        return tmpTint;
    }

    final Vector3f posTmp = new Vector3f();
    final Quaternionf rotTmp = new Quaternionf();
    final Matrix4f tmpLocalPose = new Matrix4f();
    final Matrix4f tmpInvViewRot = new Matrix4f();
    /** Scratch для конверсии composed(view) → мировой трансформ (см. {@link #convertToWorldRecord}). */
    private final Matrix4f tmpWorldMat = new Matrix4f();
    final float[] tmpCornerUV;

    int instanceVboId = -1;
    FloatBuffer instanceBuffer;
    private long instanceBufferAddress;

    // ── Direct-path VBO shadow (span-diff аплоады) ─────────────────────
    // CPU-копия содержимого instanceVboId: drawInstanceRange/flushFadingVanilla/
    // renderSingleVanilla аплоадят только изменившиеся span-ы (см. GpuSpanUploader).
    // Ленивый alloc: только рендереры, реально ходящие прямым путём (не MDI).
    private FloatBuffer vboShadow;
    private int vboShadowFloats = 0;
    private boolean vboShadowValid = false;
    private static long activeAnchorGen = -1L;
    private long lastAnchorGen = -1L;

    public static void onRenderOriginChanged() {
        activeAnchorGen = FrameViewState.anchorGeneration();
        // Записи якорно-относительны: сдвиг якоря протухает их все — fast-path
        // roster-assert обязан выдать worldGen-мисс и уйти в полный пересбор.
        NucleusRenderVersion.bump();
    }
    /** Оценка VRAM этого рендерера (вершины+индексы+instance VBO); 0 = не учтён. */
    private long vramBytes = 0;

    java.nio.ByteBuffer atlasVertexBytesRetained;
    java.nio.IntBuffer atlasIndicesRetained;
    int atlasIndexCountRetained;
    @Nullable
    private String mdiTraceTag;

    private static final Cleaner CLEANER = Cleaner.create();
    private Cleaner.Cleanable instanceBufferCleanable;

    final List<BakedQuad> quadsForIris;

    // ── Delegate helpers ───────────────────────────────────────────────
    final VanillaInstancedBatchRenderer vanillaHelper;
    private final IrisInstancedBatchRenderer irisHelper;

    // ── Scratch ─────────────────────────────────────────────────────────
    private final Matrix4f tmpCompositeMat = new Matrix4f();

    // ── Constructors ───────────────────────────────────────────────────

    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data) {
        this(data, null);
    }

    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data, List<BakedQuad> quadsForIris) {
        this.quadsForIris = quadsForIris;
        this.instanceDataSize = INSTANCE_DATA_SIZE;
        this.instanceAttribLast = 14;
        this.instanceFadeFloatOffset = 13; // InstBboxSize.w
        this.lightFloatCount = 16;
        this.tmpCornerUV = new float[lightFloatCount];

        this.vanillaHelper = new VanillaInstancedBatchRenderer(this);
        this.irisHelper = new IrisInstancedBatchRenderer(this);

        if (data == null) {
            MainRegistry.LOGGER.error("InstancedStaticPartRenderer: Received NULL VboData! Cannot create renderer.");
            initialized = false;
            return;
        }
        if (!RenderSystem.isOnRenderThread()) {
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: Skipping initialization because this is not render thread.");
            data.close();
            initialized = false;
            return;
        }
        if (GLFW.glfwGetCurrentContext() == 0L) {
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: No current GLFW OpenGL context; falling back to non-instanced render path.");
            data.close();
            initialized = false;
            return;
        }
        if (!InstancedGlCompat.supportsInstancedAttributeDivisor()) {
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: Instancing entrypoints unavailable. Falling back to non-instanced render path.");
            data.close();
            initialized = false;
            return;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        try {
            vaoId = GL30.glGenVertexArrays();
            vboId = GL15.glGenBuffers();

            if (vaoId == 0 || vboId == 0) {
                throw new IllegalStateException("Failed to generate VAO/VBO!");
            }

            indexCount = data.indices != null ? data.indices.remaining() : 0;
            setObjBboxFrom(data);

            if (data.bytesPerVertex != SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES) {
                throw new IllegalStateException("InstancedStaticPartRenderer expects VboData.bytesPerVertex="
                        + SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES + " got " + data.bytesPerVertex);
            }
            int meshStride = data.bytesPerVertex;

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, meshStride, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, meshStride, 12);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, meshStride, 24);
            GL30.glEnableVertexAttribArray(3);
            GL30.glVertexAttribIPointer(3, 1, GL11.GL_INT, meshStride, 32);

            if (data.indices != null && data.indices.remaining() > 0) {
                eboId = GL15.glGenBuffers();
                if (eboId == 0) {
                    throw new IllegalStateException("Failed to generate EBO!");
                }
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
            }

            instanceVboId = GL15.glGenBuffers();
            if (instanceVboId == 0) {
                throw new IllegalStateException("Failed to generate instance VBO!");
            }

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) maxInstances * instanceDataSize * 4, GL15.GL_STREAM_DRAW);

            int stride = instanceDataSize * 4;

            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, stride, 0);
            InstancedGlCompat.glVertexAttribDivisorCompat(4, 1);

            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, stride, 3 * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(5, 1);

            GL20.glEnableVertexAttribArray(6);
            GL20.glVertexAttribPointer(6, 3, GL11.GL_FLOAT, false, stride, 7 * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(6, 1);

            GL20.glEnableVertexAttribArray(7);
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, stride, 10 * 4);
            InstancedGlCompat.glVertexAttribDivisorCompat(7, 1);

            GL20.glEnableVertexAttribArray(8);
            GL20.glVertexAttribPointer(8, 4, GL11.GL_FLOAT, false, stride, LIGHT_FLOAT_OFFSET * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(8, 1);
            GL20.glEnableVertexAttribArray(9);
            GL20.glVertexAttribPointer(9, 4, GL11.GL_FLOAT, false, stride, (LIGHT_FLOAT_OFFSET + 4) * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(9, 1);
            GL20.glEnableVertexAttribArray(10);
            GL20.glVertexAttribPointer(10, 4, GL11.GL_FLOAT, false, stride, (LIGHT_FLOAT_OFFSET + 8) * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(10, 1);
            GL20.glEnableVertexAttribArray(11);
            GL20.glVertexAttribPointer(11, 4, GL11.GL_FLOAT, false, stride, (LIGHT_FLOAT_OFFSET + 12) * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(11, 1);

            // InstUvRect vec4 (loc 12): sprite-rect ремап normalized-VBO → атлас.
            GL20.glEnableVertexAttribArray(12);
            GL20.glVertexAttribPointer(12, 4, GL11.GL_FLOAT, false, stride, UVRECT_FLOAT_OFFSET * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(12, 1);

            // InstColor vec4 (loc 13): per-instance RGBA-тинт (white = passthrough).
            GL20.glEnableVertexAttribArray(13);
            GL20.glVertexAttribPointer(13, 4, GL11.GL_FLOAT, false, stride, TINT_FLOAT_OFFSET * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(13, 1);

            // GradParams vec4 (loc 14): пространственный фоллофф тинта (axis<0 = off).
            GL20.glEnableVertexAttribArray(14);
            GL20.glVertexAttribPointer(14, 4, GL11.GL_FLOAT, false, stride, GRAD_FLOAT_OFFSET * 4L);
            InstancedGlCompat.glVertexAttribDivisorCompat(14, 1);

            GL30.glBindVertexArray(0);

            instanceBuffer = MemoryUtil.memAllocFloat(maxInstances * instanceDataSize);
            fadingSnapshot = MemoryUtil.memAllocFloat(maxInstances * instanceDataSize);
            this.instanceBufferAddress = MemoryUtil.memAddress0(instanceBuffer);
            final long bufferAddress = MemoryUtil.memAddress(instanceBuffer);
            final long fadingAddress = MemoryUtil.memAddress(fadingSnapshot);
            instanceBufferCleanable = CLEANER.register(this, () -> {
                try {
                    if (bufferAddress != 0L) {
                        MemoryUtil.nmemFree(bufferAddress);
                    }
                    if (fadingAddress != 0L) {
                        MemoryUtil.nmemFree(fadingAddress);
                    }
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("Failed to free instanceBuffer via Cleaner", t);
                }
            });

            if (data.byteBuffer != null && data.indices != null
                    && data.indices.remaining() > 0) {
                try {
                    java.nio.ByteBuffer srcVb = data.byteBuffer.duplicate();
                    atlasVertexBytesRetained = MemoryUtil.memAlloc(srcVb.remaining());
                    atlasVertexBytesRetained.put(srcVb);
                    atlasVertexBytesRetained.flip();

                    java.nio.IntBuffer srcIb = data.indices.duplicate();
                    atlasIndexCountRetained = srcIb.remaining();
                    atlasIndicesRetained = MemoryUtil.memAllocInt(atlasIndexCountRetained);
                    atlasIndicesRetained.put(srcIb);
                    atlasIndicesRetained.flip();
                } catch (Throwable t) {
                    MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: failed to retain MDI atlas copy ({}); MDI path will skip this renderer", t.toString());
                    if (atlasVertexBytesRetained != null) {
                        MemoryUtil.memFree(atlasVertexBytesRetained);
                        atlasVertexBytesRetained = null;
                    }
                    if (atlasIndicesRetained != null) {
                        MemoryUtil.memFree(atlasIndicesRetained);
                        atlasIndicesRetained = null;
                    }
                    atlasIndexCountRetained = 0;
                }
            }

            long vram = (data.byteBuffer != null ? data.byteBuffer.remaining() : 0L)
                    + ((long) indexCount * 4L)
                    + (long) maxInstances * instanceDataSize * 4L;

            data.close();
            initialized = true;
            vramBytes = vram;
            NucleusDebug.addRendererVram(vram);

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize InstancedStaticPartRenderer", e);
            // super.cleanup() (AbstractGpuMesh) выходит рано по !initialized — а в
            // конструкторе initialized ещё false. Уже сгенерированные GL-объекты
            // удаляем явно, иначе исключение между glGen* и концом try утекает
            // VAO/vertex VBO/EBO (instanceVboId чистится в cleanup() ниже).
            if (vaoId != -1) {
                try { GL30.glDeleteVertexArrays(vaoId); } catch (Throwable ignored) {}
                vaoId = -1;
            }
            if (vboId != -1) {
                try { GL15.glDeleteBuffers(vboId); } catch (Throwable ignored) {}
                vboId = -1;
            }
            if (eboId != -1) {
                try { GL15.glDeleteBuffers(eboId); } catch (Throwable ignored) {}
                eboId = -1;
            }
            cleanup();
            initialized = false;
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
        }
    }

    // ── Instance data write ────────────────────────────────────────────

    /**
     * Конвертирует composed view-space трансформ (R_cam·T(rel)·local, контракт
     * одинаков на 1.20.1 и 1.21.1 — см. {@link FrameViewState}) в мировые posTmp/rotTmp.
     * Мир вместо камеры в записи ⇒ движение камеры не «грязнит» инстансы (GpuSpanUploader
     * даёт нулевой аплоад статичной сцены), камера применяется в vsh через ModelViewMat.
     */
    void convertToWorldRecord(Matrix4f composed) {
        tmpWorldMat.set(FrameViewState.inverseViewRotation()).mul(composed);
        tmpWorldMat.getTranslation(posTmp);
        posTmp.add(FrameViewState.relCamX(), FrameViewState.relCamY(), FrameViewState.relCamZ());
        tmpWorldMat.getNormalizedRotation(rotTmp);
        rotTmp.normalize();
    }

    void memPutInstanceRecordAtBaseFloat(int baseFloatIndex) {
        long a = instanceBufferAddress + (long) baseFloatIndex * 4L;
        MemoryUtil.memPutFloat(a, posTmp.x);
        MemoryUtil.memPutFloat(a + 4, posTmp.y);
        MemoryUtil.memPutFloat(a + 8, posTmp.z);
        MemoryUtil.memPutFloat(a + 12, rotTmp.x);
        MemoryUtil.memPutFloat(a + 16, rotTmp.y);
        MemoryUtil.memPutFloat(a + 20, rotTmp.z);
        MemoryUtil.memPutFloat(a + 24, rotTmp.w);
        MemoryUtil.memPutFloat(a + 28, objBbox[0]);
        MemoryUtil.memPutFloat(a + 32, objBbox[1]);
        MemoryUtil.memPutFloat(a + 36, objBbox[2]);
        float sx = objBbox[3] - objBbox[0];
        float sy = objBbox[4] - objBbox[1];
        float sz = objBbox[5] - objBbox[2];
        MemoryUtil.memPutFloat(a + 40, sx);
        MemoryUtil.memPutFloat(a + 44, sy);
        MemoryUtil.memPutFloat(a + 48, sz);
        // fade квантуется до 1/255: иначе плавный ramp фейда по дистанции меняет
        // младшие биты каждый кадр и span-дифф GpuSpanUploader никогда не сходит
        // в ноль при движении камеры. 8 бит альфы на глаз неотличимы.
        float fade = quantizeFade(SingleMeshVboRenderer.getFadeAlpha());
        MemoryUtil.memPutFloat(a + 52, fade);
        long lightA = a + (long) LIGHT_FLOAT_OFFSET * 4L;
        for (int i = 0; i < lightFloatCount; i++) {
            MemoryUtil.memPutFloat(lightA + (long) i * 4L, tmpCornerUV[i]);
        }
        long uvA = a + (long) UVRECT_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(uvA, tmpUvRect[0]);
        MemoryUtil.memPutFloat(uvA + 4, tmpUvRect[1]);
        MemoryUtil.memPutFloat(uvA + 8, tmpUvRect[2]);
        MemoryUtil.memPutFloat(uvA + 12, tmpUvRect[3]);
        long tintA = a + (long) TINT_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(tintA, tmpTint[0]);
        MemoryUtil.memPutFloat(tintA + 4, tmpTint[1]);
        MemoryUtil.memPutFloat(tintA + 8, tmpTint[2]);
        MemoryUtil.memPutFloat(tintA + 12, tmpTint[3]);
        long gradA = a + (long) GRAD_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(gradA, tmpGrad[0]);
        MemoryUtil.memPutFloat(gradA + 4, tmpGrad[1]);
        MemoryUtil.memPutFloat(gradA + 8, tmpGrad[2]);
        MemoryUtil.memPutFloat(gradA + 12, tmpGrad[3]);
    }

    /**
     * Сравнивает компоненты новой записи (posTmp/rotTmp/objBbox/fade/tmpCornerUV)
     * с содержимым instanceBuffer на слоте {@code baseFloat}. Записи прошлого флаша
     * сохраняются в буфере (clear() сбрасывает только position), поэтому совпадение
     * означает «машина не изменилась с прошлого флаша» — memPut можно пропустить.
     *
     * <p>Сравнение с допуском (см. {@link #recEq}), а не точное: мировая запись
     * получается через R⁻¹·(R_cam·T·local), и при ВРАЩЕНИИ камеры произведение
     * R⁻¹·R_cam≈I даёт ulp-джиттер последних битов — точное равенство считало
     * все машины грязными при каждом повороте. Допуски на порядки меньше
     * видимого порога (~пиксель на экране ≈ 2мм на метровой дистанции).
     */
    private boolean recordMatchesBuffer(int baseFloatIndex) {
        long a = instanceBufferAddress + (long) baseFloatIndex * 4L;
        if (!recEq(MemoryUtil.memGetFloat(a), posTmp.x, POS_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 4), posTmp.y, POS_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 8), posTmp.z, POS_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 12), rotTmp.x, ROT_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 16), rotTmp.y, ROT_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 20), rotTmp.z, ROT_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 24), rotTmp.w, ROT_EPS)) return false;
        if (MemoryUtil.memGetFloat(a + 28) != objBbox[0]) return false;
        if (MemoryUtil.memGetFloat(a + 32) != objBbox[1]) return false;
        if (MemoryUtil.memGetFloat(a + 36) != objBbox[2]) return false;
        float sx = objBbox[3] - objBbox[0];
        float sy = objBbox[4] - objBbox[1];
        float sz = objBbox[5] - objBbox[2];
        if (MemoryUtil.memGetFloat(a + 40) != sx) return false;
        if (MemoryUtil.memGetFloat(a + 44) != sy) return false;
        if (MemoryUtil.memGetFloat(a + 48) != sz) return false;
        float fade = quantizeFade(SingleMeshVboRenderer.getFadeAlpha());
        if (MemoryUtil.memGetFloat(a + 52) != fade) return false;
        long lightA = a + (long) LIGHT_FLOAT_OFFSET * 4L;
        for (int i = 0; i < lightFloatCount; i++) {
            if (!recEq(MemoryUtil.memGetFloat(lightA + (long) i * 4L), tmpCornerUV[i], LIGHT_EPS)) return false;
        }
        long uvA = a + (long) UVRECT_FLOAT_OFFSET * 4L;
        // Точное равенство: rect приходят из стабильных спрайт-границ, меняется
        // только при смене скина — тогда запись обязана переписаться.
        if (MemoryUtil.memGetFloat(uvA) != tmpUvRect[0]) return false;
        if (MemoryUtil.memGetFloat(uvA + 4) != tmpUvRect[1]) return false;
        if (MemoryUtil.memGetFloat(uvA + 8) != tmpUvRect[2]) return false;
        if (MemoryUtil.memGetFloat(uvA + 12) != tmpUvRect[3]) return false;
        long tintA = a + (long) TINT_FLOAT_OFFSET * 4L;
        // Точное равенство, как у uvRect: резолвер тинта обязан сходиться к стабильным
        // значениям (иначе skip-write деградирует в ежекадровую запись — не ошибка, но
        // дешевле в самом резолвере).
        if (MemoryUtil.memGetFloat(tintA) != tmpTint[0]) return false;
        if (MemoryUtil.memGetFloat(tintA + 4) != tmpTint[1]) return false;
        if (MemoryUtil.memGetFloat(tintA + 8) != tmpTint[2]) return false;
        if (MemoryUtil.memGetFloat(tintA + 12) != tmpTint[3]) return false;
        long gradA = a + (long) GRAD_FLOAT_OFFSET * 4L;
        // Фоллофф — статический конфиг части: значения стабильны, сравнение точное.
        if (MemoryUtil.memGetFloat(gradA) != tmpGrad[0]) return false;
        if (MemoryUtil.memGetFloat(gradA + 4) != tmpGrad[1]) return false;
        if (MemoryUtil.memGetFloat(gradA + 8) != tmpGrad[2]) return false;
        if (MemoryUtil.memGetFloat(gradA + 12) != tmpGrad[3]) return false;
        return true;
    }

    /** |a-b| ≤ eps (NaN не равен ничему — консервативно пишет запись). */
    private static boolean recEq(float a, float b, float eps) {
        return Math.abs(a - b) <= eps;
    }

    /** Допуск сравнения мировых координат записи (~1мм — субпиксель даже вблизи). */
    private static final float POS_EPS = 1.0e-3f;
    /** Допуск кватерниона записи (~1e-4 рад ≈ 0.006° — невидимо). */
    private static final float ROT_EPS = 1.0e-5f;
    /** Допуск углов света (триллинейные веса тоже проходят через камерно-относительный pose). */
    private static final float LIGHT_EPS = 1.0e-4f;

    /**
     * Готов ли рендерер к {@code MdiBatchCoordinator.submitClean}: в этой фазе записи
     * не было ни одной записи в буфер, и буфер синхронизирован со снапшотом координатора.
     */
    boolean canSubmitMdiClean() {
        return ClientRenderFlags.mdiCleanFrameReuse() && !mdiRecordWriteHappened && mdiBufferSynced;
    }

    /** Буфер снова соответствует снапшоту координатора (accepted сабмит/чистый реассерт). */
    void noteMdiDispatched() {
        mdiBufferSynced = true;
    }

    /** Синхронизация потеряна: direct-путь, перестановка партицией, renderSingle, отказ сабмита. */
    void noteMdiDispatchLost() {
        mdiBufferSynced = false;
    }

    /**
     * Span-аплоад окна данных в instance VBO прямого пути (offset 0):
     * сравнивает с теневой копией и грузит только изменившиеся диапазоны.
     * Orphan убран: при пропуске span-ов GPU-буфер обязан СОХРАНЯТЬ старое
     * содержимое, glBufferData(orphan) его уничтожил бы.
     */
    void uploadToInstanceVboSpanned(FloatBuffer src, int srcFloatOffset, int destFloatOffset, int floats) {
        if (floats <= 0 || instanceVboId <= 0) {
            return;
        }
        if (this.lastAnchorGen != activeAnchorGen) {
            this.lastAnchorGen = activeAnchorGen;
            this.vboShadowValid = false;
            this.mdiBufferSynced = false;
        }
        int need = 64;
        while (need < floats) {
            need <<= 1;
        }
        if (vboShadow == null || vboShadowFloats < floats) {
            if (vboShadow != null) {
                MemoryUtil.memFree(vboShadow);
            }
            vboShadow = MemoryUtil.memAllocFloat(need);
            vboShadowFloats = need;
            vboShadowValid = false;
        }
        if (vboShadowValid) {
            GpuSpanUploader.diffUpload(vboShadow, src, srcFloatOffset, destFloatOffset, floats, instanceVboId);
        } else {
            GpuSpanUploader.fullUpload(vboShadow, destFloatOffset, src, srcFloatOffset, floats, instanceVboId);
            vboShadowValid = true;
        }
    }

    void uploadInstanceStreamToBoundVbo() {
        // instanceBuffer уже flip: валидные записи [0, remaining).
        uploadToInstanceVboSpanned(instanceBuffer, 0, 0, instanceBuffer.remaining());
    }

    protected List<BakedQuad> getQuadsForIrisPath() {
        return quadsForIris;
    }

    // ── renderSingle ───────────────────────────────────────────────────

    public void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity) {
        renderSingle(poseStack, packedLight, blockPos, blockEntity, null);
    }

    @Override
    public void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
                             @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        if (!initialized || vaoId <= 0 || eboId <= 0 || indexCount <= 0 || instanceVboId <= 0 || instanceBuffer == null) return;

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            if (irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                // Fallback уходит в bufferSource (отрисовка на endBatch) — companion VAO
                // shadow-батча обязан быть отвязан (per-part release в shadow отключён).
                com.hbm_m.client.render.shader.IrisRenderBatch.detachCompanionVaoForVanillaWork();
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    RenderHooks.putBulkDataShaded(consumer, pose, quad, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

        vanillaHelper.renderSingleVanilla(poseStack, packedLight, blockPos, blockEntity, bufferSource);
    }

    // ── addInstance ─────────────────────────────────────────────────────

    /**
     * Fast-path dirty-skip: может ли слот {@code instanceCount} быть подтверждён
     * roster-assert'ом для машины {@code posKey} без пересборки записи?
     * Roster-ключи живут в {@link #instanceOcclusionKeys} (= pos.asLong(), пишется
     * и в addInstance, и в assert) — содержимое буфера соответствует слоту k,
     * пока ни одна запись в [0, k] не была переставлена/перезаписана другой машиной.
     * Только чтение — вызов безопасен в проверочной фазе (до коммита).
     */
    public boolean canAssertInstance(long posKey) {
        return initialized && instanceBuffer != null
                && instanceCount < maxInstances
                && instanceOcclusionKeys[instanceCount] == posKey;
    }

    /**
     * Квант fade записей инстанс-буфера: 1/255 (см. {@link #memPutInstanceRecordAtBaseFloat}).
     * Единственная точка квантования — запись, skip-write-сравнение и roster-assert
     * обязаны сверять один и тот же квант.
     */
    public static float quantizeFade(float fade) {
        return Math.round(fade * 255.0f) * (1.0f / 255.0f);
    }

    /**
     * Fast-path dirty-skip с проверкой fade: слот {@code instanceCount} подтверждается
     * только при совпадении roster-ключа И квантованного fade записи в буфере.
     * <p>
     * Дистанционный fade — чистая функция позиции камеры и меняется каждый кадр
     * движения БЕЗ dirty-события; key-ассерт без этой проверки замораживает альфу
     * записи до случайного полного пересбора (pop вместо растворения при отдалении,
     * «застревание» полупрозрачной при приближении). Проверка — одно чтение флоата
     * из буфера; машины с fade=1 (основная масса) проходят здесь же.
     */
    public boolean canAssertInstance(long posKey, float expectedQuantizedFade) {
        if (!canAssertInstance(posKey)) {
            return false;
        }
        long fadeAddr = instanceBufferAddress
                + ((long) instanceCount * instanceDataSize + instanceFadeFloatOffset) * 4L;
        return MemoryUtil.memGetFloat(fadeAddr) == expectedQuantizedFade;
    }

    /**
     * Коммит roster-assert для машины {@code blockPos}: содержимое буфера уже
     * содержит запись прошлого кадра (clear() после флаша сбрасывает только
     * position), инстанс просто продлевает своё присутствие — без матриц,
     * света и сравнения флоатов. Вызывать ТОЛЬКО после {@link #canAssertInstance}.
     */
    public void assertCleanInstance(BlockPos blockPos) {
        if (instanceCount >= maxInstances) {
            return;
        }
        long key = OcclusionCullingHelper.occlusionKeyForBlock(blockPos);
        if (instanceOcclusionKeys[instanceCount] != key) {
            return; // защитно: вызывающий обязан проверить canAssertInstance
        }
        if (instanceCount == 0) {
            overflowLogged = false;
            mdiRecordWriteHappened = false;
            var level = Minecraft.getInstance().level;
            batchSkyDarken = (level != null) ? level.getSkyDarken(1.0f) : -1f;
        }
        instanceCullIndices[instanceCount] = -1;
        instanceCount++;
        ((Buffer) instanceBuffer).position(instanceDataSize * instanceCount);
    }

    /**
     * Содержимое instance-буфера больше не соответствует roster-ключам
     * (renderSingle перезаписал слот 0). Все canAssertInstance начнут выдавать
     * миссы → машины уйдут в полный пересбор.
     */
    void invalidateRoster() {
        if (instanceOcclusionKeys.length > 0) {
            instanceOcclusionKeys[0] = Long.MIN_VALUE;
        }
    }

    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, null);
    }

    @Override
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource, null);
    }

    /**
     * Like {@link #addInstance(PoseStack, int, BlockPos, BlockEntity, MultiBufferSource)} but
     * reuses {@code sharedCornerUV8} (16 floats from {@link LightSampleCache#getOrSample8}) for all
     * parts of one machine in the same frame — avoids repeated spatial sampling at farm scale.
     */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                            @Nullable float[] sharedCornerUV8) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource, sharedCornerUV8, null);
    }

    /**
     * Полная форма: {@code uvRect} = {u0, v0, du, dv} ремапа sprite-local VBO в атлас
     * (текстурно-вариантные части дверей; null/identity — обычные машины с атласными UV);
     * {@code tint} = {r,g,b,a} per-instance цветовой множитель (null/white — passthrough;
     * RGB может быть &gt; 1 — overbright-накал, см. блок_lit InstColor).
     */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                            @Nullable float[] sharedCornerUV8, @Nullable float[] uvRect) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource, sharedCornerUV8, uvRect, null);
    }

    /** Полная форма с per-instance тинтом; см. 7-arg перегрузку для описания остальных параметров. */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                            @Nullable float[] sharedCornerUV8, @Nullable float[] uvRect,
                            @Nullable float[] tint) {
        addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource, sharedCornerUV8, uvRect, tint, null);
    }

    /**
     * Полная форма с тинтом и фоллоффом. {@code grad} = {axis (0/1/2, &lt;0 = off),
     * fullCoord, zeroCoord, pad} — конфиг части ({@code MachineSpecBuilder.tintFalloff}),
     * фоллофф применяется в шейдере по модельным координатам вершины.
     */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource,
                            @Nullable float[] sharedCornerUV8, @Nullable float[] uvRect,
                            @Nullable float[] tint, @Nullable float[] grad) {
        if (!initialized) return;

        if (uvRect != null && uvRect.length >= 4) {
            tmpUvRect[0] = uvRect[0];
            tmpUvRect[1] = uvRect[1];
            tmpUvRect[2] = uvRect[2];
            tmpUvRect[3] = uvRect[3];
        } else {
            tmpUvRect[0] = 0f;
            tmpUvRect[1] = 0f;
            tmpUvRect[2] = 1f;
            tmpUvRect[3] = 1f;
        }
        if (tint != null && tint.length >= 4) {
            tmpTint[0] = tint[0];
            tmpTint[1] = tint[1];
            tmpTint[2] = tint[2];
            tmpTint[3] = tint[3];
        } else {
            tmpTint[0] = 1f;
            tmpTint[1] = 1f;
            tmpTint[2] = 1f;
            tmpTint[3] = 0f;
        }
        if (grad != null && grad.length >= 4 && grad[0] >= 0f) {
            tmpGrad[0] = grad[0];
            tmpGrad[1] = grad[1];
            tmpGrad[2] = grad[2];
            tmpGrad[3] = grad[3];
        } else {
            tmpGrad[0] = -1f;
            tmpGrad[1] = 0f;
            tmpGrad[2] = 0f;
            tmpGrad[3] = 0f;
        }

        // Shadow pass (обе платформы): немедленная отрисовка через АКТИВНЫЙ
        // per-BE батч (быстрый путь, см. IrisRenderBatch.begin). Раньше блок
        // был forge-only: на neoforge 1.21.1 инстансы накапливались в shadow
        // с теневыми матрицами и затем флашились в основном проходе. Fallback
        // без батча — putBulkData через bufferSource (SHADOW_BLOCK на endBatch).
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            // Глобальный shadow-батч: запись вместо немедленного дроука — флаш один
            // на всю BE-фазу (Iris-миксин в ShadowRenderer.renderShadows). При
            // выключенном/неудавшемся батче — прежний немедленный путь.
            if (irisHelper.tryRecordShadowInstance(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                // Fallback в bufferSource при открытом shadow-батче — см. detach-комментарий в renderSingle.
                // Тинт читаем из tmpTint (заполнен выше в этом addInstance) — тени цвет не
                // используют, но alpha/overbright не должны теряться на fallback-пути.
                com.hbm_m.client.render.shader.IrisRenderBatch.detachCompanionVaoForVanillaWork();
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                float tr = tmpTint[0], tg = tmpTint[1], tb = tmpTint[2];
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    float shade = RenderHooks.quadShade(quad.getDirection());
                    RenderHooks.putBulkData(consumer, pose, quad,
                            shade * tr, shade * tg, shade * tb, fade, packedLight,
                            OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

        if (instanceCount >= maxInstances) {
            OVERFLOW_ADD_COUNT.incrementAndGet();
            if (!overflowLogged) {
                overflowLogged = true;
                MainRegistry.LOGGER.warn(
                        "InstancedStaticPartRenderer overflow: maxInstances={} reached for tag={}, skipping extra instances until next flush",
                        maxInstances, mdiTraceTag);
            }
            return;
        }
        if (instanceCount == 0) {
            overflowLogged = false;
            mdiRecordWriteHappened = false;
            var level = Minecraft.getInstance().level;
            batchSkyDarken = (level != null) ? level.getSkyDarken(1.0f) : -1f;
        }

        Matrix4f mat = poseStack.last().pose();
        // ВЕРДИКТ ПО ВАНИЛЬНЫМ ИСТОЧНИКАМ 1.21.1 (GameRenderer.renderLevel + LevelRenderer.renderLevel):
        // R_cam (frustumMatrix) передаётся ОТДЕЛЬНО от проекции и пушится в RenderSystem.getModelViewStack()
        // перед циклом BE — то есть RenderSystem.getModelViewMatrix() == R_cam на ОБЕИХ версиях,
        // а poseStack несёт только T(blockPos - cam) * local. Предыдущая ветка "pose уже содержит камеру"
        // была ложной и выбрасывала R_cam → модели летали по экрану. Стриппинг отменён.
        // (Уточнение 2026-09-12 по свежим сорсам: на 1.20.1 наоборот — R_cam запечён в poseStack,
        // а modelViewStack до AFTER_BLOCK_ENTITIES identity; composed = mvm·pose совпадает на обеих.)
        tmpCompositeMat.set(RenderSystem.getModelViewMatrix()).mul(mat);
        convertToWorldRecord(tmpCompositeMat);

        fillInstanceCornerLight(blockEntity, packedLight, blockPos, poseStack.last().pose(), sharedCornerUV8);

        instanceCullIndices[instanceCount] = -1;
        instanceOcclusionKeys[instanceCount] = OcclusionCullingHelper.occlusionKeyForBlock(blockPos);
        int baseFloat = instanceCount * instanceDataSize;
        // Skip-write: clear() после флаша сбрасывает только position — записи прошлого
        // флаша остаются в буфере. Если новая запись побайтово совпадает (статичная
        // машина: тот же pos/rot, квантованный fade, тот же свет), memPut пропускается,
        // и координатор может переиспользовать свой снапшот целиком (submitClean).
        if (!recordMatchesBuffer(baseFloat)) {
            memPutInstanceRecordAtBaseFloat(baseFloat);
            mdiRecordWriteHappened = true;
        }
        instanceCount++;
        ((Buffer) instanceBuffer).position(instanceDataSize * instanceCount);
    }

    /** Companion-меш для глобального shadow-батча ({@link IrisShadowBatchCollector}); лениво строится. */
    @Nullable
    IrisCompanionMesh getOrBuildCompanionForShadowBatch() {
        return irisHelper.getOrBuildIrisCompanion();
    }

    /**
     * Инстансный дроук теневых записей: наш ExtendedShader (vanilla-parity FSH —
     * теневому FB не нужен pack-формат), parent VAO, span-дифф записей в instance
     * VBO, ОДИН {@code glDrawElementsInstanced} на part-renderer.
     * <p>
     * Записи — позы в shadow-space (точная декомпозиция T·R PoseStack'а BER'а),
     * поэтому ModelViewMat ЖЁСТКО identity: iris_ModelViewMat из RenderSystem
     * брать нельзя (на Oculus 1.8 стек в shadow может нести shadow-MV — это и
     * было «ползание» теней в раннем варианте). Клип = P_shadow · pose, ровно
     * тот же контракт, что у pack-программного drawCompanion-флаша.
     * {@code records} — position в конце записи; метод делает flip/clear сам.
     */
    boolean drawShadowInstances(java.nio.FloatBuffer records, int count,
                                ShaderInstance shader, Matrix4f proj) {
        if (!initialized || vaoId <= 0 || instanceVboId <= 0 || instanceBuffer == null || count <= 0) {
            return false;
        }
        try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            records.flip();
            uploadToInstanceVboSpanned(records, 0, 0, count * instanceDataSize);
            vanillaHelper.enableVertexAttribsForDraw();

            vanillaHelper.useIrisProgram(shader, proj, IDENTITY_MV);
            SingleMeshVboRenderer.primeIrisInstancedSamplerMap(shader, Minecraft.getInstance());
            // Паковская тене-дисторсия — обязана совпадать с сэмплером пака
            // (IrisShadowDistortion извлекает режим/константы из исходника пака).
            // ВСЕ юниформы объявлены "float" — set(int) у float-юниформа падает
            // NPE (Uniform.intValues == null, debug.log 0913 23:41, BSL).
            com.mojang.blaze3d.shaders.Uniform uMode = shader.getUniform("hbmShadowDistortionMode");
            if (uMode != null) { uMode.set((float) com.hbm_m.client.render.shader.IrisShadowDistortion.mode()); uMode.upload(); }
            com.mojang.blaze3d.shaders.Uniform uDist = shader.getUniform("hbmShadowDistortion");
            if (uDist != null) { uDist.set(com.hbm_m.client.render.shader.IrisShadowDistortion.distortion()); uDist.upload(); }
            com.mojang.blaze3d.shaders.Uniform uScale = shader.getUniform("hbmShadowDepthScale");
            if (uScale != null) { uScale.set(com.hbm_m.client.render.shader.IrisShadowDistortion.depthScale()); uScale.upload(); }

            InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, indexCount,
                    GL11.GL_UNSIGNED_INT, 0, count);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Iris shadow instanced draw failed", e);
            records.clear();
            return false;
        }
        records.clear();
        return true;
    }

    /** Identity ModelViewMat для теневого инстанс-дроука (записи уже в shadow-space). */
    private static final Matrix4f IDENTITY_MV = new Matrix4f();

    /**
     * Fills {@link #tmpCornerUV} for the instanced VBO and, when Iris flush needs it,
     * {@link #instanceLightUV} for the current slot.
     *
     * <p>When {@code sharedCornerUV8} is supplied (one 8-corner sample per machine per frame),
     * vanilla path skips {@link LightSampleCache#getOrSample} and {@code tmpLocalPose} work.
     */
    private void fillInstanceCornerLight(@Nullable BlockEntity blockEntity, int packedLight,
                                         BlockPos blockPos, Matrix4f worldPose,
                                         @Nullable float[] sharedCornerUV8) {
        boolean hasSharedCorners = sharedCornerUV8 != null && sharedCornerUV8.length >= 16;
        boolean needsIrisInstanceLight = ShaderCompatibilityDetector.canUseIrisExtendedShader()
                || ShaderCompatibilityDetector.isExternalShaderActive();

        if (needsIrisInstanceLight) {
            int sampleBase = instanceCount * 2;
            LightSampleCache.getOrSample(blockEntity, packedLight, instanceLightUV, sampleBase);
        }

        if (hasSharedCorners) {
            System.arraycopy(sharedCornerUV8, 0, tmpCornerUV, 0, 16);
            return;
        }

        if (LightSampleCache.BASE_POSE_SET.get()) {
            tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(worldPose);
        } else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            //? if < 1.21.1 {
            tmpInvViewRot.identity().set(RenderSystem.getInverseViewRotationMatrix());
            //?} else {
            /*// rotation(camera.rotation()) = R_cam⁻¹ (frustumMatrix ванилы =
            // rotation(rotation().conjugate()) = R_cam, quaternion уже несёт
            // ОБРАТНУЮ view-ротацию — как в FrameViewState.capture; ранний
            // лишний .invert() поворачивал сэмпл-точки света на R_cam² и
            // свет «бегал» по машине при повороте камеры.
            tmpInvViewRot.identity().rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            *///?}
            tmpLocalPose.set(tmpInvViewRot).mul(worldPose);
            tmpLocalPose.m30(tmpLocalPose.m30() - (float) (blockPos.getX() - cam.x));
            tmpLocalPose.m31(tmpLocalPose.m31() - (float) (blockPos.getY() - cam.y));
            tmpLocalPose.m32(tmpLocalPose.m32() - (float) (blockPos.getZ() - cam.z));
        }

        long partHash = System.identityHashCode(this);
        LightSampleCache.getOrSample8(blockEntity, partHash, objBbox, blockPos, tmpLocalPose,
                packedLight, tmpCornerUV);
    }

    // ── Flush ──────────────────────────────────────────────────────────

    @Override
    public void flush() {
        flush(RenderSystem.getProjectionMatrix());
    }


    /**
     * Обязательный re-bind atlas + lightmap после {@link ShaderInstance#apply()} и перед glDraw*.
     * <p>
     * <b>РЕГРЕССИЯ-СТОП:</b> без этого instanced машины белые (Sampler2 читает unit 0 = atlas).
     * Делегат — {@link SingleMeshVboRenderer#bindBlockLitSamplerTextures}; не дублировать логику здесь.
     */
    static void bindBlockLitTexturesBeforeDraw(ShaderInstance shader) {
        SingleMeshVboRenderer.bindBlockLitSamplerTextures(shader);
    }

    /**
     * Вызывается из {@link com.hbm_m.client.render.culling.InstancedRenderFrame#presentAfterBlockEntities}
     * в том же кадре, что addInstance — не откладывать flush на конец уровня.
     */
    @Override
    public void flush(Matrix4f projectionMatrix) {
        // Отложенное затухание прошлой фазы либо уже нарисовано flushFading(),
        // либо протухло (повторный AFTER_BLOCK_ENTITIES-проход за кадр) — сброс.
        deferredFadingCount = 0;
        if (instanceCount == 0) return;

        if (!initialized || vaoId <= 0 || eboId <= 0 || instanceVboId <= 0 || instanceBuffer == null) {
            instanceCount = 0;
            if (instanceBuffer != null) {
                instanceBuffer.clear();
            }
            return;
        }

        boolean useIrisFlush = ShaderCompatibilityDetector.canUseIrisExtendedShader();
        if (useIrisFlush) {
            irisHelper.flushBatchIris(projectionMatrix);
        } else if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // Iris/Oculus owns the GL program; vanilla instanced shaders would draw with
            // "No active program". Callers must fall back to per-BE VBO / putBulkData.
        } else {
            vanillaHelper.flushBatchVanilla(projectionMatrix);
        }

        instanceCount = 0;
        instanceBuffer.clear();
        overflowLogged = false;
    }

    /**
     * Фаза 2 (вызывается из InstancedRenderFrame ПОСЛЕ MDI-диспетча): добирает
     * затухающие инстансы, отложенные прямым путём в {@link #deferFading}.
     * Безусловно гасит отложенное состояние — пропуск вызова не может
     * «протухнуть» в следующий кадр. Под Iris отложенного не бывает
     * (flushBatchVanilla там не вызывается) — естественный no-op.
     */
    public void flushFading(Matrix4f projectionMatrix) {
        int count = deferredFadingCount;
        deferredFadingCount = 0;
        if (count <= 0) return;
        if (!initialized || vaoId <= 0 || eboId <= 0 || instanceVboId <= 0 || fadingSnapshot == null) return;
        if (ShaderCompatibilityDetector.isExternalShaderActive()) return;
        vanillaHelper.flushFadingVanilla(projectionMatrix, count);
    }

    /**
     * Копирует затухающие записи [firstRecord, instanceCount) главного буфера в
     * снапшот фазы 2. Вызывается из {@code flushBatchVanilla} после партиции;
     * instanceBuffer в этот момент position=0 (после flip), содержимое уже
     * переставлено [opaque | fading].
     */
    void deferFading(int firstRecord, int count) {
        deferredFadingCount = count;
        if (count <= 0 || fadingSnapshot == null) return;
        long srcAddr = MemoryUtil.memAddress(instanceBuffer)
                + (long) firstRecord * instanceDataSize * 4L;
        MemoryUtil.memCopy(srcAddr, MemoryUtil.memAddress(fadingSnapshot),
                (long) count * instanceDataSize * 4L);
    }

    /**
     * Ключ сортировки fading-окон прямого пути: distSq ДАЛЬНЕГО fading-инстанса
     * (запись 0 снапшота — back-to-front внутри рендерера). -1 = фейда нет.
     * Используется MachineSpec.flushFading для глобального back-to-front порядка окон.
     */
    public float fadingSortKeyDistSq() {
        if (deferredFadingCount <= 0 || fadingSnapshot == null) return -1f;
        float dx = fadingSnapshot.get(0) - FrameViewState.relCamX();
        float dy = fadingSnapshot.get(1) - FrameViewState.relCamY();
        float dz = fadingSnapshot.get(2) - FrameViewState.relCamZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /** fade ниже порога считается затухающим (консистентно с minFade-порогом блендинга). */
    static final float OPAQUE_FADE_THRESHOLD = 0.99f;

    /**
     * Переставляет записи инстанс-буфера в порядке [opaque в исходном порядке |
     * затухающие back-to-front по |InstPos|²] и возвращает число opaque-записей.
     * Буфер: position 0, валидные записи [0, instanceCount·stride).
     * <p>
     * Общий механизм фазового порядка G для MDI-снапшотов
     * ({@code MdiBatchCoordinator.partitionOpaqueFirst}) и прямого пути
     * ({@code flushBatchVanilla}): «непрозрачные раньше затухающих» обязан
     * соблюдаться во ВСЕХ путях рендера одинаково.
     *
     * @param parallelKeys необязательный параллельный массив roster-ключей
     *                     (машина на слот): переставляется синхронно с записями,
     *                     чтобы соответствие «ключ ⇔ запись в слоте» сохранилось
     *                     после партиции (fast-path dirty-skip). null — не трогать.
     */
    static int partitionInstancesOpaqueFirst(FloatBuffer buf, int instanceCount, int floatsPerInstance, int fadeOffset,
                                             long[] parallelKeys) {
        int n = instanceCount;
        if (buf == null || n <= 0) return 0;

        int fadeTotal = 0;
        for (int i = 0; i < n; i++) {
            if (buf.get(i * floatsPerInstance + fadeOffset) < OPAQUE_FADE_THRESHOLD) {
                fadeTotal++;
            }
        }
        if (fadeTotal == 0) return n;

        FloatBuffer tmp = MemoryUtil.memAllocFloat(n * floatsPerInstance);
        long[] tmpKeys = parallelKeys != null ? new long[n] : null;
        try {
            int[] fadeIdx = new int[fadeTotal];
            float[] fadeDistSq = new float[fadeTotal];
            int opaqueWrote = 0;
            int k = 0;
            for (int i = 0; i < n; i++) {
                int recBase = i * floatsPerInstance;
                if (buf.get(recBase + fadeOffset) < OPAQUE_FADE_THRESHOLD) {
                    fadeIdx[k] = i;
                    float x = buf.get(recBase);
                    float y = buf.get(recBase + 1);
                    float z = buf.get(recBase + 2);
                    fadeDistSq[k] = x * x + y * y + z * z;
                    k++;
                } else {
                    copyInstanceRecord(buf, recBase, tmp, opaqueWrote * floatsPerInstance, floatsPerInstance);
                    if (tmpKeys != null) {
                        tmpKeys[opaqueWrote] = parallelKeys[i];
                    }
                    opaqueWrote++;
                }
            }
            // insertion sort по убыванию distSq (back-to-front); затухающих обычно единицы
            for (int a = 1; a < fadeTotal; a++) {
                int idx = fadeIdx[a];
                float d = fadeDistSq[a];
                int b = a - 1;
                while (b >= 0 && fadeDistSq[b] < d) {
                    fadeIdx[b + 1] = fadeIdx[b];
                    fadeDistSq[b + 1] = fadeDistSq[b];
                    b--;
                }
                fadeIdx[b + 1] = idx;
                fadeDistSq[b + 1] = d;
            }
            int dst = opaqueWrote;
            for (int a = 0; a < fadeTotal; a++, dst++) {
                copyInstanceRecord(buf, fadeIdx[a] * floatsPerInstance, tmp, dst * floatsPerInstance, floatsPerInstance);
                if (tmpKeys != null) {
                    tmpKeys[dst] = parallelKeys[fadeIdx[a]];
                }
            }
            MemoryUtil.memCopy(MemoryUtil.memAddress(tmp), MemoryUtil.memAddress(buf),
                    (long) n * floatsPerInstance * 4L);
            if (tmpKeys != null) {
                System.arraycopy(tmpKeys, 0, parallelKeys, 0, n);
            }
            return n - fadeTotal;
        } finally {
            MemoryUtil.memFree(tmp);
        }
    }

    private static void copyInstanceRecord(FloatBuffer src, int srcRecBase, FloatBuffer dst, int dstRecBase, int floats) {
        for (int f = 0; f < floats; f++) {
            dst.put(dstRecBase + f, src.get(srcRecBase + f));
        }
    }

    // ── IrisCompanionMeshRenderer interface ────────────────────────────

    @Override
    public void flushBatchIris(Matrix4f projectionMatrix) {
        irisHelper.flushBatchIris(projectionMatrix);
    }

    @Override
    public boolean drawSingleWithIrisExtended(PoseStack poseStack, int packedLight,
                                              BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        return irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity);
    }

    // ── State queries ──────────────────────────────────────────────────

    @Override
    public boolean isInitialized() {
        return initialized && vaoId > 0 && vboId > 0 && eboId > 0;
    }

    @Override
    public int getInstanceCount() {
        return instanceCount;
    }

    public void setMdiTraceTag(@Nullable String tag) {
        this.mdiTraceTag = tag;
    }

    @Nullable
    public String getMdiTraceTag() {
        return mdiTraceTag;
    }

    // ── Cleanup ────────────────────────────────────────────────────────

    @Override
    public void cleanup() {
        super.cleanup();

        // Retained-запись координатора для этого рендерера больше не валидна:
        // слот атласа снесёт evictRendererIfRegistered, флаги запрещают submitClean.
        this.mdiBufferSynced = false;
        this.mdiRecordWriteHappened = false;

        final long vramToRelease = this.vramBytes;
        this.vramBytes = 0L;
        if (vramToRelease != 0L) {
            NucleusDebug.removeRendererVram(vramToRelease);
        }

        final int instanceVboToDelete = this.instanceVboId;
        final Cleaner.Cleanable bufferCleanable = this.instanceBufferCleanable;
        final FloatBuffer shadowToFree = this.vboShadow;
        this.vboShadow = null;
        this.vboShadowFloats = 0;
        this.vboShadowValid = false;
        final java.nio.ByteBuffer atlasVbToFree = this.atlasVertexBytesRetained;
        final java.nio.IntBuffer atlasIbToFree = this.atlasIndicesRetained;
        this.atlasVertexBytesRetained = null;
        this.atlasIndicesRetained = null;
        this.atlasIndexCountRetained = 0;

        this.instanceVboId = -1;
        this.instanceBuffer = null;
        this.fadingSnapshot = null;
        this.instanceBufferCleanable = null;
        vanillaHelper.invalidateShaderCache();

        irisHelper.cleanup();

        final InstancedStaticPartRenderer mdiEvictTarget = this;
        RenderSystem.recordRenderCall(() -> {
            try {
                com.hbm_m.client.render.MdiGeometryAtlas.evictRendererIfRegistered(mdiEvictTarget);
                if (instanceVboToDelete != -1) {
                    GL15.glDeleteBuffers(instanceVboToDelete);
                }
                if (bufferCleanable != null) {
                    bufferCleanable.clean();
                }
                if (shadowToFree != null) {
                    MemoryUtil.memFree(shadowToFree);
                }
                if (atlasVbToFree != null) {
                    MemoryUtil.memFree(atlasVbToFree);
                }
                if (atlasIbToFree != null) {
                    MemoryUtil.memFree(atlasIbToFree);
                }
            } catch (Exception e) {
                MainRegistry.LOGGER.error("InstancedStaticPartRenderer.cleanup failed", e);
            }
        });
    }
}
