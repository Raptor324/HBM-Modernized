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
 * Compute scatter uploader: consolidates N small staging-ring -> dest-VBO copies
 * into a single compute dispatch (Phase 4 roadmap, the scatter.glsl pattern of CrankShaft).
 * <p>
 * The enable threshold lives in {@link GpuSpanUploader}: bulk burst updates (TTL
 * re-collect, mega-bases) produce dozens of spans per flush; each span via
 * {@code glCopyBufferSubData} is a separate driver call. One dispatch replaces
 * them all: ops SSBO (16 bytes per copy) + the staging ring as SSBO source
 * + the dest VBO as SSBO sink.
 * <p>
 * Gates: GL 4.3 compute (or ARB_compute_shader + SSBO), not an Intel iGPU
 * (unified memory - the DMA path is safer and no slower), kill-switch
 * {@code -Dhbm.gpuScatter=false}. The barrier after the dispatch makes the dest VBO
 * available for subsequent use as a vertex attribute.
 */
@OnlyIn(Dist.CLIENT)
public final class ComputeScatterUploader {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Maximum copies in a single dispatch (ops SSBO size). */
    private static final int MAX_OPS = 64;
    /** Struct CopyOp: 4 × uint. */
    private static final int OP_BYTES = 16;

    /** Emergency JVM override -Dhbm.gpuScatter=false; the config nucleusGpuScatter is the primary source. */
    private static final boolean SCATTER_FLAG =
            !"false".equalsIgnoreCase(System.getProperty("hbm.gpuScatter", "true"));

    private static boolean enabled() {
        return com.hbm_m.config.ModClothConfig.get().nucleusGpuScatter && SCATTER_FLAG;
    }

    private static ComputeScatterUploader instance;
    private static boolean resolved = false;
    private static Boolean intelGpu = null;

    private int programId = 0;
    private int opsSsbo = 0;
    private ByteBuffer opsHost;

    private ComputeScatterUploader() {}

    /**
     * System-property kill-switch; span/byte thresholds live in
     * {@link GpuSpanUploader} (the flush context is there).
     */
    public static boolean isGloballyEnabled() {
        return enabled();
    }

    /** null = scatter unavailable (no GL 4.3, Intel iGPU, compilation failed) - use the DMA path. */
    public static ComputeScatterUploader getOrCreate() {
        if (resolved) {
            return instance;
        }
        resolved = true;
        if (!enabled() || !hardwareCapable()) {
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
            // Intel iGPU: unified memory, scatter gains nothing and risks
            // ring-bus stalls - keep the DMA path (CrankShaft pattern).
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
     * Runs a single scatter dispatch: {@code opCount} copies of word-aligned
     * ranges from {@code srcBufferId} (staging ring) into {@code destVbo}.
     *
     * @param ops triplets {@code [srcWord, dstWord, words] x opCount} (4-byte
     *            alignment required - all offsets originate from float offsets)
     * @return true if the dispatch was issued (the caller syncs the shadow);
     *         false if not issued, the caller falls back to the per-span path.
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

        // The dest VBO is later used as a vertex attribute and read by copies/SSBOs -
        // without a barrier the compute writes are invisible to subsequent commands.
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
