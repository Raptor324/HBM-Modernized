package com.hbm_m.client.render.culling;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
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
 * GPU AABB culling: frustum + depth-buffer occlusion (AAA-style screen-space test).
 *
 * <p><b>Архитектура (multi-pass):</b> кадр N−1 после BER — {@link GpuDepthSource#capturePostBerDepthForNextFrame},
 * {@link #dispatch} (depth lag-1), fence. Кадр N до BER — {@link #tryReadback} (bitmask, ~512 B);
 * во время BER — {@link #submit} + {@link OcclusionCullingHelper#shouldRender} по lag-1;
 * после BER — снова {@link #dispatch}. Машины окклюдируют друг друга с задержкой 1 кадр.
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
    private static final int BITMASK_WORDS = (MAX_ENTRIES + 31) / 32;
    // AABBEntry: vec3 minPos + uint flags + vec3 maxPos + uint instanceIndex = 32 bytes (std430)
    private static final int ENTRY_BYTES = 32;
    // std140: mat4 + 3×vec4
    private static final int PARAMS_BYTES = 112;
    private static final int LOCAL_SIZE_X = 64;
    private static final int DEPTH_TEXTURE_UNIT = 2;
    /** NDC depth bias — сравнение с terrain depth (блоки/сущности, без BER). */
    private static final float DEPTH_OCCLUSION_BIAS = 0.022f;
    /** Сэмплов по оси в screen-rect AABB (4×4). */
    private static final float DEPTH_SAMPLES_PER_AXIS = 4f;
    /** Внутри этого радиуса от камеры — не depth-occlude (стабильность у объекта). */
    /** Мин. dist² до AABB (ближайшая точка на боксе) — без depth-occlusion, только frustum. */
    /** dist²: внутри — depth occlusion не применяется (только frustum). 144 = 12 блоков. */
    private static final float NEAR_SKIP_OCCLUSION_SQ = 144.0f;

    private static volatile boolean initialized = false;
    private static volatile boolean initAttempted = false;
    private static volatile boolean supported = false;

    private static int program = 0;
    private static int paramsUbo = 0;
    private static int depthSamplerLoc = -1;

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
    /** Lag-1 readback (ключ → видимость) для {@link #shouldRender} до BER. */
    private static int[] lagVisibleBits = new int[0];
    private static int lagVisibleCount = 0;
    private static Long2IntOpenHashMap lagKeyToIndex = null;
    private static boolean lagReadbackValid = false;
    private static long lagReadbackGeneration = 0L;

    /** Same-frame readback (staging index → видимость) только для MDI. */
    private static int[] mdiVisibleBits = new int[0];
    private static int mdiVisibleCount = 0;
    private static boolean mdiReadbackValid = false;
    private static int mdiReadbackStagingEpoch = -1;

    /** Увеличивается в {@link #beginFrame}; сбрасывает staging для следующего render-кадра. */
    private static int stagingEpoch = 0;

    private static long lastDispatchGeneration = 0L;

    /** Same-frame MDI: output SSBO index after {@link #dispatchSameFrame} (no ping-pong toggle). */
    private static int sameFrameOutputIdx = -1;
    private static int sameFrameCullCount = 0;

    private GpuCullingPipeline() {}

    /** @deprecated prefer {@link #isLagReadbackValid()} / {@link #isMdiReadbackValid()} */
    public static long getReadbackGeneration() {
        return lagReadbackGeneration;
    }

    public static boolean isLagReadbackValid() {
        return lagReadbackValid;
    }

    /** True только после успешного {@link #tryReadbackSameFrame} для текущего {@link #stagingEpoch}. */
    public static boolean isMdiReadbackValid() {
        return mdiReadbackValid && mdiReadbackStagingEpoch == stagingEpoch;
    }

    public static int getStagingEpoch() {
        return stagingEpoch;
    }

    public static long getLastDispatchGeneration() {
        return lastDispatchGeneration;
    }

    /** Сброс lag-1 результатов (смена Iris/vanilla, смена пакета шейдеров). */
    public static void clearReadback() {
        lagKeyToIndex = null;
        lagReadbackValid = false;
        lagReadbackGeneration = 0L;
        mdiReadbackValid = false;
        mdiReadbackStagingEpoch = -1;
    }

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
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) BITMASK_WORDS * 4, GL15.GL_STREAM_READ);

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
            depthSamplerLoc = GL20.glGetUniformLocation(program, "uDepthTex");
            if (depthSamplerLoc >= 0) {
                GL20.glUseProgram(program);
                GL20.glUniform1i(depthSamplerLoc, DEPTH_TEXTURE_UNIT);
                GL20.glUseProgram(0);
            }
            MainRegistry.LOGGER.info("[HBM-GpuCulling] GPU depth occlusion culling enabled. vendor={} renderer={} version={}",
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
        stagingEpoch++;
        stagingCount = 0;
        stagingKeyToIndex.clear();
        if (stagingEntries != null) {
            stagingEntries.clear();
        }
        mdiReadbackValid = false;
        clearSameFrameResult();
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

    /** Индекс в staging / visibility bitmask; {@code -1} если ключ не зарегистрирован. */
    public static int getStagingIndex(long key) {
        return stagingKeyToIndex.get(key);
    }

    public static int getStagingCount() {
        return stagingCount;
    }

    public static void clearSameFrameResult() {
        sameFrameOutputIdx = -1;
        sameFrameCullCount = 0;
    }

    /** SSBO bitmask после {@link #dispatchSameFrame}; {@code 0} если нет результата. */
    public static int getSameFrameVisibilitySsbo() {
        return sameFrameOutputIdx >= 0 ? outputSsbo[sameFrameOutputIdx] : 0;
    }

    public static int getSameFrameCullCount() {
        return sameFrameCullCount;
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
    /**
     * GPU cull для текущего кадра без readback: результат остаётся в {@link #outputSsbo}
     * для {@link GpuMdiCompaction}. Не переключает ping-pong (lag-1 readback сохраняется).
     */
    public static void dispatchSameFrame(Matrix4f viewProj, Vec3 cameraPos, int depthTextureId,
                                         int viewportWidth, int viewportHeight) {
        sameFrameOutputIdx = -1;
        sameFrameCullCount = 0;
        mdiReadbackValid = false;
        if (!initialized || stagingCount == 0) {
            return;
        }
        if (depthTextureId <= 0 || viewportWidth <= 0 || viewportHeight <= 0) {
            return;
        }
        try {
            int w = writeIdx;
            runCullDispatch(w, viewProj, cameraPos, depthTextureId, viewportWidth, viewportHeight, false);
            sameFrameOutputIdx = w;
            sameFrameCullCount = stagingCount;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-GpuCulling] dispatchSameFrame failed", t);
        }
    }

    public static void dispatch(Matrix4f viewProj, Vec3 cameraPos, int depthTextureId, int viewportWidth, int viewportHeight) {
        if (!initialized || stagingCount == 0) {
            writeIdx ^= 1;
            return;
        }
        if (depthTextureId <= 0 || viewportWidth <= 0 || viewportHeight <= 0) {
            writeIdx ^= 1;
            return;
        }
        try {
            int w = writeIdx;
            runCullDispatch(w, viewProj, cameraPos, depthTextureId, viewportWidth, viewportHeight, true);
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-GpuCulling] Dispatch failed", t);
        }
    }

    private static void runCullDispatch(int w, Matrix4f viewProj, Vec3 cameraPos, int depthTextureId,
                                        int viewportWidth, int viewportHeight, boolean lagPingPong) {
        try {

            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, inputSsbo[w]);
            stagingEntries.position(0).limit(stagingCount * ENTRY_BYTES);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, stagingEntries);

            paramsStaging.clear();
            viewProj.get(paramsStaging);
            paramsStaging.position(64);
            paramsStaging.putFloat((float) cameraPos.x);
            paramsStaging.putFloat((float) cameraPos.y);
            paramsStaging.putFloat((float) cameraPos.z);
            paramsStaging.putFloat((float) stagingCount);
            paramsStaging.putFloat((float) viewportWidth);
            paramsStaging.putFloat((float) viewportHeight);
            paramsStaging.putFloat(DEPTH_OCCLUSION_BIAS);
            paramsStaging.putFloat(DEPTH_SAMPLES_PER_AXIS);
            paramsStaging.putFloat(NEAR_SKIP_OCCLUSION_SQ);
            paramsStaging.putFloat(0.0f);
            // z: 1 = frustum-only в compute (Iris/Oculus depth не совпадает с vanilla viewProj)
            paramsStaging.putFloat(ShaderCompatibilityDetector.isExternalShaderActive() ? 1.0f : 0.0f);
            paramsStaging.putFloat(0.0f);
            paramsStaging.flip();
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, paramsUbo);
            GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0L, paramsStaging);
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, paramsUbo);

            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, inputSsbo[w]);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, outputSsbo[w]);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outputSsbo[w]);
            int wordsToClear = (stagingCount + 31) / 32;
            GL43.glClearBufferSubData(
                    GL43.GL_SHADER_STORAGE_BUFFER, GL30.GL_R32UI, 0L,
                    (long) wordsToClear * 4, GL30.GL_RED_INTEGER, GL30.GL_UNSIGNED_INT,
                    new int[] {0});

            int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + DEPTH_TEXTURE_UNIT);
            int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTextureId);

            GL20.glUseProgram(program);
            int groups = (stagingCount + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;
            GL43.glDispatchCompute(groups, 1, 1);
            int barrier = GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_TEXTURE_FETCH_BARRIER_BIT;
            if (!lagPingPong) {
                barrier |= GL43.GL_COMMAND_BARRIER_BIT;
            }
            GL43.glMemoryBarrier(barrier);
            GL20.glUseProgram(0);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
            GL13.glActiveTexture(prevActive);

            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

            keyToIndex[w].clear();
            keyToIndex[w].putAll(stagingKeyToIndex);
            dispatchedCount[w] = stagingCount;
            if (fence[w] != 0L) {
                GL32.glDeleteSync(fence[w]);
            }
            fence[w] = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
            if (lagPingPong) {
                lastDispatchGeneration++;
                writeIdx ^= 1;
            }
        } finally {
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }
    }

    /**
     * Readback результата dispatch прошлого кадра. Буфер для чтения — {@code writeIdx ^ 1}
     * (активный {@link #writeIdx} указывает на set для следующей записи).
     *
     * @param waitNs 0 = non-blocking; иначе {@link GL32#glClientWaitSync} timeout (нс)
     */
    /**
     * Readback результата {@link #dispatchSameFrame} (тот же кадр, до MDI).
     */
    public static void tryReadbackSameFrame(long waitNs) {
        if (!initialized || sameFrameOutputIdx < 0 || sameFrameCullCount <= 0) {
            return;
        }
        if (readbackFromBuffer(sameFrameOutputIdx, sameFrameCullCount, waitNs, true)) {
            mdiReadbackValid = true;
            mdiReadbackStagingEpoch = stagingEpoch;
        }
    }

    /** Блокирующий readback для MDI, если fence ещё не готов. */
    public static void ensureMdiReadback() {
        if (isMdiReadbackValid()) {
            return;
        }
        tryReadbackSameFrame(500_000L);
        if (!isMdiReadbackValid()) {
            GL11.glFinish();
            tryReadbackSameFrame(0L);
        }
    }

    public static void tryReadback(long waitNs) {
        if (!initialized) return;
        int r = writeIdx ^ 1;
        if (readbackFromBuffer(r, dispatchedCount[r], waitNs, false)) {
            lagKeyToIndex = keyToIndex[r];
            lagReadbackValid = true;
            lagReadbackGeneration++;
        }
    }

    private static boolean readbackFromBuffer(int bufferIdx, int count, long waitNs, boolean forMdi) {
        if (bufferIdx < 0 || count <= 0) {
            return false;
        }
        long f = fence[bufferIdx];
        if (f == 0L) {
            return false;
        }
        try {
            int flags = waitNs > 0L ? GL32.GL_SYNC_FLUSH_COMMANDS_BIT : 0;
            int waitRes = GL32.glClientWaitSync(f, flags, waitNs);
            if (waitRes == GL32.GL_ALREADY_SIGNALED || waitRes == GL32.GL_CONDITION_SATISFIED) {
                int words = (count + 31) / 32;
                int[] target = forMdi ? mdiVisibleBits : lagVisibleBits;
                if (target.length < words) {
                    target = new int[words];
                    if (forMdi) {
                        mdiVisibleBits = target;
                    } else {
                        lagVisibleBits = target;
                    }
                }
                if (forMdi) {
                    mdiVisibleCount = count;
                } else {
                    lagVisibleCount = count;
                }
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outputSsbo[bufferIdx]);
                ByteBuffer mapped = MemoryUtil.memByteBuffer(
                        org.lwjgl.opengl.GL30.nglMapBufferRange(
                                GL43.GL_SHADER_STORAGE_BUFFER, 0L, (long) words * 4,
                                GL30.GL_MAP_READ_BIT),
                        words * 4);
                if (mapped != null) {
                    mapped.order(ByteOrder.nativeOrder());
                    for (int i = 0; i < words; i++) {
                        target[i] = mapped.getInt(i * 4);
                    }
                    GL30.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
                }
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                GL32.glDeleteSync(f);
                fence[bufferIdx] = 0L;
                return true;
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-GpuCulling] Readback failed", t);
        }
        return false;
    }

    /** Возвращает видимость по ключу из lag-1 readback. Нет записи в прошлом dispatch — консервативно видим. */
    /** Per-staging-index visibility from same-frame MDI readback only. */
    public static boolean isCullIndexVisible(int cullIndex) {
        if (cullIndex < 0 || !isMdiReadbackValid()) {
            return true;
        }
        int word = cullIndex >> 5;
        int bit = cullIndex & 31;
        if (word >= mdiVisibleBits.length || cullIndex >= mdiVisibleCount) {
            return true;
        }
        return (mdiVisibleBits[word] & (1 << bit)) != 0;
    }

    public static boolean isVisible(long key) {
        if (!initialized || !lagReadbackValid || lagKeyToIndex == null) {
            return true;
        }
        int idx = lagKeyToIndex.get(key);
        if (idx < 0 || idx >= lagVisibleCount) {
            return true;
        }
        int word = idx >> 5;
        int bit = idx & 31;
        if (word >= lagVisibleBits.length) {
            return true;
        }
        return (lagVisibleBits[word] & (1 << bit)) != 0;
    }

    /** True, если для ключа есть результат последнего успешного lag-1 readback. */
    public static boolean hasGpuVisibilityResult(long key) {
        if (!lagReadbackValid || lagKeyToIndex == null) {
            return false;
        }
        int idx = lagKeyToIndex.get(key);
        return idx >= 0 && idx < lagVisibleCount;
    }

    public static boolean hasResultFor(long key) {
        return hasGpuVisibilityResult(key);
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
