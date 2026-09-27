package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * Compute scatter uploader: консолидирует N мелких копий staging-ring → dest-VBO
 * в одну compute-диспетчу (Phase 4 roadmap, паттерн scatter.glsl CrankShaft).
 * <p>
 * Порог включения — в {@link GpuSpanUploader}: массовые burst-апдейты (TTL
 * re-collect, мега-базы) дают десятки span-ов на flush; каждый span через
 * {@code glCopyBufferSubData} — это отдельный driver-вызов. Одна диспетча
 * заменяет их все: ops-SSBO (16 байт на копию) + staging ring как SSBO-источник
 * + dest VBO как SSBO-приёмник.
 * <p>
 * Гейты: GL 4.3 compute (или ARB_compute_shader + SSBO), не Intel iGPU
 * (unified memory — DMA-путь безопаснее и не медленнее), kill-switch
 * {@code -Dhbm.gpuScatter=false}. Барьер после диспетчи открывает dest VBO
 * для последующего использования как vertex-атрибута.
 */
@OnlyIn(Dist.CLIENT)
public final class ComputeScatterUploader {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Максимум копий в одной диспетче (размер ops-SSBO). */
    private static final int MAX_OPS = 64;
    /** Struct CopyOp: 4 × uint. */
    private static final int OP_BYTES = 16;

    private static final boolean ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("hbm.gpuScatter", "true"));

    private static ComputeScatterUploader instance;
    private static boolean resolved = false;
    private static Boolean intelGpu = null;

    private int programId = 0;
    private int opsSsbo = 0;
    private ByteBuffer opsHost;

    private ComputeScatterUploader() {}

    /**
     * Системное свойство kill-switch; пороги спанов/байт лежат в
     * {@link GpuSpanUploader} (там контекст flush'а).
     */
    public static boolean isGloballyEnabled() {
        return ENABLED;
    }

    /** null = scatter недоступен (не GL 4.3, Intel iGPU, компиляция упала) — идти DMA-путём. */
    public static ComputeScatterUploader getOrCreate() {
        if (resolved) {
            return instance;
        }
        resolved = true;
        if (!ENABLED || !hardwareCapable()) {
            return null;
        }
        try {
            ComputeScatterUploader u = new ComputeScatterUploader();
            if (u.initialise()) {
                instance = u;
                LOGGER.info("[HBM-M] ComputeScatterUploader ready (maxOps={})", MAX_OPS);
            }
        } catch (Throwable t) {
            LOGGER.warn("[HBM-M] ComputeScatterUploader unavailable: {}", t.toString());
            instance = null;
        }
        return instance;
    }

    private static boolean hardwareCapable() {
        try {
            GLCapabilities caps = GL.getCapabilities();
            if (caps.glDispatchCompute == 0L
                    || !(caps.OpenGL43 || caps.GL_ARB_compute_shader)
                    || !(caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object)) {
                return false;
            }
            // Intel iGPU: unified memory, scatter не даёт выигрыша и рискует
            // ring-bus статьями — оставляем DMA-путь (паттерн CrankShaft).
            if (isIntelGpu()) {
                return false;
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isIntelGpu() {
        Boolean cached = intelGpu;
        if (cached == null) {
            String renderer;
            try {
                renderer = GL11.glGetString(GL11.GL_RENDERER);
            } catch (Throwable t) {
                renderer = null;
            }
            cached = renderer != null && renderer.contains("Intel");
            intelGpu = cached;
        }
        return cached;
    }

    private boolean initialise() {
        GLCapabilities caps = GL.getCapabilities();
        if (caps.glBufferSubData == 0L || caps.glDispatchCompute == 0L) {
            return false;
        }
        programId = compileComputeShader("/assets/hbm_m/shaders/cull/upload_scatter.comp");
        opsSsbo = GL15.glGenBuffers();
        GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, opsSsbo);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) MAX_OPS * OP_BYTES, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        opsHost = ByteBuffer.allocateDirect(MAX_OPS * OP_BYTES).order(ByteOrder.nativeOrder());
        return programId > 0 && opsSsbo > 0;
    }

    private static String loadShaderSource(String resourcePath) {
        try (InputStream in = ComputeScatterUploader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Shader resource not found: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read shader " + resourcePath, e);
        }
    }

    private static int compileComputeShader(String resourcePath) {
        String source = loadShaderSource(resourcePath);
        int shader = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
        com.hbm_m.platform.RenderHooks.safeShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Failed to compile compute shader " + resourcePath + ": " + log);
        }
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shader);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(shader);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 4096);
            GL20.glDeleteProgram(program);
            throw new IllegalStateException("Failed to link compute program for " + resourcePath + ": " + log);
        }
        return program;
    }

    /**
     * Выполняет одну scatter-диспетчу: {@code opCount} копий word-выровненных
     * диапазонов из {@code srcBufferId} (staging ring) в {@code destVbo}.
     *
     * @param ops triplets {@code [srcWord, dstWord, words] × opCount} (4-байтное
     *            выравнивание обязательно — все offset'ы происходят из float-смещений)
     * @return true — диспетча выпущена (вызывающий синхронизирует shadow);
     *         false — диспетча не выпущена, вызывающий идёт per-span путём.
     */
    public boolean scatter(int srcBufferId, int destVbo, long[] ops, int opCount) {
        if (programId <= 0 || opsSsbo <= 0 || srcBufferId <= 0 || destVbo <= 0
                || opCount <= 0 || opCount > MAX_OPS || ops == null || ops.length < opCount * 3) {
            return false;
        }

        opsHost.clear();
        for (int k = 0; k < opCount; k++) {
            long srcWord = ops[k * 3];
            long dstWord = ops[k * 3 + 1];
            long words = ops[k * 3 + 2];
            if (srcWord < 0 || dstWord < 0 || words <= 0
                    || srcWord > 0xFFFFFFFFL || dstWord > 0xFFFFFFFFL || words > 0xFFFFFFFFL) {
                return false;
            }
            opsHost.putInt((int) srcWord);
            opsHost.putInt((int) dstWord);
            opsHost.putInt((int) words);
            opsHost.putInt(0);
        }
        opsHost.flip();

        GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, opsSsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, opsHost);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, opsSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, srcBufferId);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, destVbo);

        GL20.glUseProgram(programId);
        GL43.glDispatchCompute(opCount, 1, 1);

        // Dest VBO далее используется как vertex-атрибут и читается копиями/SSBO —
        // без барьера compute-записи не видны последующим командам.
        GL42.glMemoryBarrier(GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT
                | GL43.GL_SHADER_STORAGE_BARRIER_BIT
                | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);

        GL20.glUseProgram(0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, 0);
        return true;
    }

    public static void destroy() {
        if (instance != null) {
            if (instance.programId > 0) {
                GL20.glDeleteProgram(instance.programId);
            }
            if (instance.opsSsbo > 0) {
                GL15.glDeleteBuffers(instance.opsSsbo);
            }
            instance = null;
        }
        resolved = false;
    }
}
