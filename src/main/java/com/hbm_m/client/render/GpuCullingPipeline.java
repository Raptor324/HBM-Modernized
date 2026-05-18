package com.hbm_m.client.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
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
 * GPU-side AABB frustum culling через compute shader (GL 4.3 / ARB_compute_shader).
 *
 * <p><b>Архитектура:</b> кадр N собирает AABB+key в CPU staging,
 * заливает в SSBO, диспатчит compute, ставит fence. Кадр N+1 берёт
 * результаты предыдущего кадра через {@link #isVisible(long)} (lag=1,
 * приемлемо для статичных машин).
 *
 * <p><b>Ping-pong:</b> два set'а буферов (input/output/fence).
 * Активный set пишется в кадре N; пассивный набор содержит результат
 * предыдущего диспатча, который мы читаем в начале кадра N через
 * {@code glClientWaitSync(0)} (non-blocking).
 *
 * <p><b>Capability gating:</b> отсутствие compute или SSBO вызывает
 * {@link #initialize()} false; внешний код тогда переключается на
 * {@link CpuFrustumCuller}.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class GpuCullingPipeline {

    private static final int MAX_ENTRIES = 4096;
    // AABBEntry: vec3 minPos + uint flags + vec3 maxPos + uint instanceIndex = 32 bytes (std430)
    private static final int ENTRY_BYTES = 32;
    // CullParams uniform block: 6*vec4 + vec4 + vec4 = 8 vec4 = 128 bytes (std140)
    private static final int PARAMS_BYTES = 128;
    private static final int LOCAL_SIZE_X = 64;

    private static volatile boolean initialized = false;
    private static volatile boolean initAttempted = false;
    private static volatile boolean supported = false;

    private static int program = 0;
    private static int paramsUbo = 0;

    // Ping-pong buffers
    private static final int[] inputSsbo = new int[2];
    private static final int[] outputSsbo = new int[2];
    private static final long[] fence = new long[2];
    private static final int[] dispatchedCount = new int[2];
    private static final Long2IntOpenHashMap[] keyToIndex = new Long2IntOpenHashMap[2];

    // Active (write) and read sides toggle each frame
    private static int writeIdx = 0;

    // Per-frame staging
    private static ByteBuffer stagingEntries;
    private static ByteBuffer paramsStaging;
    private static int stagingCount = 0;
    private static final Long2IntOpenHashMap stagingKeyToIndex = new Long2IntOpenHashMap();
    private static int[] readVisible = new int[0]; // results for the buffer we read this frame
    private static Long2IntOpenHashMap readKeyToIndex = null;

    private GpuCullingPipeline() {}

    public static boolean isSupported() {
        if (!initAttempted) {
            // Defer real init to render thread; just check caps here if possible.
            return checkCaps();
        }
        return supported;
    }

    private static boolean checkCaps() {
        try {
            GLCapabilities caps = GL.getCapabilities();
            if (caps == null) return false;
            boolean hasCompute = caps.glDispatchCompute != 0L && (caps.OpenGL43 || caps.GL_ARB_compute_shader);
            boolean hasSsbo = caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object;
            return hasCompute && hasSsbo;
        } catch (Throwable t) {
            return false;
        }
    }

    public static synchronized boolean initialize() {
        if (initAttempted) return supported;
        initAttempted = true;
        if (!RenderSystem.isOnRenderThread()) {
            return false;
        }
        if (!checkCaps()) {
            String vendor = safeGlGetString(GL11.GL_VENDOR);
            String renderer = safeGlGetString(GL11.GL_RENDERER);
            String version = safeGlGetString(GL11.GL_VERSION);
            MainRegistry.LOGGER.info("[HBM-GpuCulling] GPU compute culling NOT available. vendor={} renderer={} version={} -> using CPU AABB frustum fallback",
                    vendor, renderer, version);
            return false;
        }

        try {
            String src = loadShaderSource();
            if (src == null) {
                MainRegistry.LOGGER.warn("[HBM-GpuCulling] Failed to load cull_aabb.compute resource; disabling GPU culling.");
                return false;
            }
            int shader = GL43.glCreateShader(GL43.GL_COMPUTE_SHADER);
            GL20.glShaderSource(shader, src);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(shader);
                GL20.glDeleteShader(shader);
                MainRegistry.LOGGER.warn("[HBM-GpuCulling] Compute shader compile failed: {}", log);
                return false;
            }
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, shader);
            GL20.glLinkProgram(program);
            GL20.glDeleteShader(shader);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(program);
                GL20.glDeleteProgram(program);
                program = 0;
                MainRegistry.LOGGER.warn("[HBM-GpuCulling] Compute shader link failed: {}", log);
                return false;
            }

            paramsUbo = GL15.glGenBuffers();
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, paramsUbo);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, paramsUbo);
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, PARAMS_BYTES, GL15.GL_DYNAMIC_DRAW);

            for (int i = 0; i < 2; i++) {
                inputSsbo[i] = GL15.glGenBuffers();
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, inputSsbo[i]);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) MAX_ENTRIES * ENTRY_BYTES, GL15.GL_STREAM_DRAW);

                outputSsbo[i] = GL15.glGenBuffers();
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outputSsbo[i]);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) MAX_ENTRIES * 4, GL15.GL_STREAM_READ);

                fence[i] = 0L;
                dispatchedCount[i] = 0;
                keyToIndex[i] = new Long2IntOpenHashMap();
                keyToIndex[i].defaultReturnValue(-1);
            }
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

            stagingEntries = MemoryUtil.memAlloc(MAX_ENTRIES * ENTRY_BYTES).order(ByteOrder.nativeOrder());
            paramsStaging = MemoryUtil.memAlloc(PARAMS_BYTES).order(ByteOrder.nativeOrder());
            stagingKeyToIndex.defaultReturnValue(-1);

            String vendor = safeGlGetString(GL11.GL_VENDOR);
            String renderer = safeGlGetString(GL11.GL_RENDERER);
            String version = safeGlGetString(GL11.GL_VERSION);
            MainRegistry.LOGGER.info("[HBM-GpuCulling] GPU compute culling enabled. vendor={} renderer={} version={}",
                    vendor, renderer, version);
            supported = true;
            initialized = true;
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-GpuCulling] Initialization failed; falling back to CPU.", t);
            destroyInternal();
            return false;
        }
    }

    private static String safeGlGetString(int name) {
        try { return GL11.glGetString(name); } catch (Throwable t) { return "?"; }
    }

    private static String loadShaderSource() {
        try {
            ResourceLocation rl = new ResourceLocation("hbm_m", "shaders/compute/cull_aabb.compute");
            var resOpt = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (resOpt.isPresent()) {
                try (InputStream is = resOpt.get().open()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            MainRegistry.LOGGER.warn("[HBM-GpuCulling] Resource load IOException", e);
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-GpuCulling] Resource load failed", t);
        }
        return null;
    }

    /** Сбрасывает CPU staging для следующего кадра после {@link #dispatch}. */
    public static void beginFrame() {
        stagingCount = 0;
        stagingKeyToIndex.clear();
    }

    /**
     * Регистрирует AABB кандидата на культинг. {@code key} обычно — {@code BlockPos.asLong()} с
     * флагом shadow-pass (см. OcclusionCullingHelper).
     */
    public static void submit(long key, AABB box) {
        if (!initialized) return;
        if (stagingCount >= MAX_ENTRIES) return;
        if (stagingKeyToIndex.containsKey(key)) return;
        int idx = stagingCount++;
        int off = idx * ENTRY_BYTES;
        stagingEntries.putFloat(off,      (float) box.minX);
        stagingEntries.putFloat(off + 4,  (float) box.minY);
        stagingEntries.putFloat(off + 8,  (float) box.minZ);
        stagingEntries.putInt(  off + 12, 0);
        stagingEntries.putFloat(off + 16, (float) box.maxX);
        stagingEntries.putFloat(off + 20, (float) box.maxY);
        stagingEntries.putFloat(off + 24, (float) box.maxZ);
        stagingEntries.putInt(  off + 28, idx);
        stagingKeyToIndex.put(key, idx);
    }

    /** Только отладка: сколько AABB в CPU staging до {@link #dispatch}. */
    public static int debugStagingEntryCount() {
        return stagingCount;
    }

    /**
     * Загружает накопленные AABB в активный SSBO, обновляет UBO с фрустумом и
     * камерой, диспатчит compute и ставит fence. Вызывается раз в кадр в
     * render-thread (например на этапе AFTER_BLOCK_ENTITIES).
     */
    public static void dispatch(Matrix4f viewProj, Vec3 cameraPos) {
        if (!initialized || stagingCount == 0) {
            // Still toggle so reads stay sane.
            writeIdx ^= 1;
            return;
        }
        try {
            int w = writeIdx;

            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, inputSsbo[w]);
            stagingEntries.position(0).limit(stagingCount * ENTRY_BYTES);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, stagingEntries);
            stagingEntries.clear();

            // Fill UBO (std140 layout: vec4-aligned)
            paramsStaging.clear();
            float[] planes = extractPlanes(viewProj);
            for (int i = 0; i < 6; i++) {
                paramsStaging.putFloat(planes[i * 4]);
                paramsStaging.putFloat(planes[i * 4 + 1]);
                paramsStaging.putFloat(planes[i * 4 + 2]);
                paramsStaging.putFloat(planes[i * 4 + 3]);
            }
            paramsStaging.putFloat((float) cameraPos.x);
            paramsStaging.putFloat((float) cameraPos.y);
            paramsStaging.putFloat((float) cameraPos.z);
            paramsStaging.putFloat((float) stagingCount);
            paramsStaging.putFloat(16.0f); // nearAlwaysVisibleSq
            paramsStaging.putFloat(0.0f);  // maxDistSq (0 = no far cutoff here)
            paramsStaging.putFloat(0.0f);
            paramsStaging.putFloat(0.0f);
            paramsStaging.flip();
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, paramsUbo);
            GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0L, paramsStaging);
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, paramsUbo);

            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, inputSsbo[w]);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, outputSsbo[w]);

            GL20.glUseProgram(program);
            int groups = (stagingCount + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;
            GL43.glDispatchCompute(groups, 1, 1);
            GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);
            GL20.glUseProgram(0);
            // Снять привязки UBO/SSBO — иначе compute может оставить binding point 0/1 занятыми
            // и следующий MDI/VBO код читает «чужие» буферы.
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

            // Snapshot key map for this dispatch
            keyToIndex[w].clear();
            keyToIndex[w].putAll(stagingKeyToIndex);
            dispatchedCount[w] = stagingCount;

            // Place fence so we can poll completion next frame.
            if (fence[w] != 0L) {
                GL32.glDeleteSync(fence[w]);
            }
            fence[w] = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

            // Toggle ping-pong; next frame writes into the OTHER set, this one
            // is read-from.
            writeIdx ^= 1;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-GpuCulling] Dispatch failed", t);
        }
    }

    /**
     * Pulls results from previously-dispatched buffer (the one we are NOT
     * currently writing to) if its fence completed. Non-blocking; if not
     * ready, leaves {@link #readVisible} from the previous successful read.
     * Call once per frame after BER, before {@link OcclusionCullingHelper#onFrameStart()} (paired with {@link #dispatch}).
     */
    public static void tryReadback() {
        if (!initialized) return;
        int r = writeIdx; // after dispatch toggled writeIdx; this index is the older one we just wrote to last frame
        long f = fence[r];
        int count = dispatchedCount[r];
        if (f == 0L || count == 0) return;
        try {
            int waitRes = GL32.glClientWaitSync(f, 0, 0L);
            if (waitRes == GL32.GL_ALREADY_SIGNALED || waitRes == GL32.GL_CONDITION_SATISFIED) {
                if (readVisible.length < count) {
                    readVisible = new int[count];
                }
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outputSsbo[r]);
                ByteBuffer mapped = MemoryUtil.memByteBuffer(
                        org.lwjgl.opengl.GL30.nglMapBufferRange(
                                GL43.GL_SHADER_STORAGE_BUFFER, 0L, (long) count * 4,
                                GL30.GL_MAP_READ_BIT),
                        count * 4);
                if (mapped != null) {
                    mapped.order(ByteOrder.nativeOrder());
                    for (int i = 0; i < count; i++) {
                        readVisible[i] = mapped.getInt(i * 4);
                    }
                    GL30.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
                }
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                readKeyToIndex = keyToIndex[r];
                GL32.glDeleteSync(f);
                fence[r] = 0L;
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-GpuCulling] Readback failed", t);
        }
    }

    /** Возвращает видимость по ключу из РЕЗУЛЬТАТОВ ПРЕДЫДУЩЕГО КАДРА (lag=1). */
    public static boolean isVisible(long key) {
        if (!initialized || readKeyToIndex == null) return true;
        int idx = readKeyToIndex.get(key);
        if (idx < 0 || idx >= readVisible.length) return true; // unseen last frame -> default visible
        return readVisible[idx] != 0;
    }

    public static boolean hasResultFor(long key) {
        if (readKeyToIndex == null) return false;
        return readKeyToIndex.get(key) >= 0;
    }

    private static float[] extractPlanes(Matrix4f m) {
        float m00 = m.m00(), m01 = m.m01(), m02 = m.m02(), m03 = m.m03();
        float m10 = m.m10(), m11 = m.m11(), m12 = m.m12(), m13 = m.m13();
        float m20 = m.m20(), m21 = m.m21(), m22 = m.m22(), m23 = m.m23();
        float m30 = m.m30(), m31 = m.m31(), m32 = m.m32(), m33 = m.m33();

        float[] p = new float[24];
        setP(p, 0, m03 + m00, m13 + m10, m23 + m20, m33 + m30);
        setP(p, 1, m03 - m00, m13 - m10, m23 - m20, m33 - m30);
        setP(p, 2, m03 + m01, m13 + m11, m23 + m21, m33 + m31);
        setP(p, 3, m03 - m01, m13 - m11, m23 - m21, m33 - m31);
        setP(p, 4, m03 + m02, m13 + m12, m23 + m22, m33 + m32);
        setP(p, 5, m03 - m02, m13 - m12, m23 - m22, m33 - m32);
        return p;
    }

    private static void setP(float[] p, int i, float a, float b, float c, float d) {
        float invLen = 1.0f / (float) Math.sqrt(a * a + b * b + c * c);
        p[i * 4]     = a * invLen;
        p[i * 4 + 1] = b * invLen;
        p[i * 4 + 2] = c * invLen;
        p[i * 4 + 3] = d * invLen;
    }

    public static synchronized void destroy() {
        destroyInternal();
    }

    private static void destroyInternal() {
        try {
            if (program != 0) { GL20.glDeleteProgram(program); program = 0; }
            if (paramsUbo != 0) { GL15.glDeleteBuffers(paramsUbo); paramsUbo = 0; }
            for (int i = 0; i < 2; i++) {
                if (inputSsbo[i] != 0) { GL15.glDeleteBuffers(inputSsbo[i]); inputSsbo[i] = 0; }
                if (outputSsbo[i] != 0) { GL15.glDeleteBuffers(outputSsbo[i]); outputSsbo[i] = 0; }
                if (fence[i] != 0L) { GL32.glDeleteSync(fence[i]); fence[i] = 0L; }
                dispatchedCount[i] = 0;
            }
            if (stagingEntries != null) { MemoryUtil.memFree(stagingEntries); stagingEntries = null; }
            if (paramsStaging != null) { MemoryUtil.memFree(paramsStaging); paramsStaging = null; }
        } catch (Throwable ignored) {}
        initialized = false;
        supported = false;
    }
}
